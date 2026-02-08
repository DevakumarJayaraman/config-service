package com.example.configservice.services;

import com.example.configservice.config.ConfigRepoProperties;
import com.example.configservice.dto.ConfigSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.example.configservice.resolvers.SecretResolverRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ConfigRepoService {

  private final ConfigRepoProperties props;
  private final Environment env;
  private final SecretResolverRegistry secretResolverRegistry;
  private final PropertiesPropertySourceLoader propsLoader = new PropertiesPropertySourceLoader();
  private final ObjectMapper jsonMapper = new ObjectMapper();

  // Map 1: File-level cache - filename -> properties map
  private final Map<String, Map<String, Object>> fileContents = new HashMap<>();

  // Map 2: Merged configuration with source tracking - app:profile -> {key: {value, source}}
  // This is the ONLY merged cache. We extract values on-the-fly for APIs that don't need sources.
  private final Map<String, Map<String, Object>> mergedConfigWithSourcesCache = new HashMap<>();

  public ConfigRepoService(ConfigRepoProperties props, Environment env) {
    this.props = props;
    this.env = env;
    this.secretResolverRegistry = new SecretResolverRegistry();
  }

  @PostConstruct
  public synchronized void initializeCaches() {
    try {
      Path configRoot = Paths.get(props.rootDir());
      if (!Files.exists(configRoot)) {
        System.out.println("Config root directory not found: " + configRoot);
        return;
      }

      try (DirectoryStream<Path> appDirs = Files.newDirectoryStream(configRoot, Files::isDirectory)) {
        for (Path appDir : appDirs) {
          try (DirectoryStream<Path> files = Files.newDirectoryStream(appDir)) {
            for (Path file : files) {
              if (Files.isRegularFile(file)) {
                loadAndCacheFile(file, file.getFileName().toString());
              }
            }
          }
        }
      }

      populateMergedConfigCache();
      System.out.println("✓ Caches initialized: " + fileContents.size() + " files, " + mergedConfigWithSourcesCache.size() + " merged configs");
    } catch (Exception e) {
      System.err.println("Error initializing caches: " + e.getMessage());
    }
  }

  private void loadAndCacheFile(Path file, String filename) {
    try {
      String lower = filename.toLowerCase(Locale.ROOT);
      Map<String, Object> properties = new LinkedHashMap<>();

      if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
        Map<String, Object> nested = loadYamlAsNested(new FileSystemResource(file.toAbsolutePath().toString()));
        properties = flattenMap(nested, "");
      } else if (lower.endsWith(".properties")) {
        List<PropertySource<?>> ps = propsLoader.load(filename, new FileSystemResource(file.toAbsolutePath().toString()));
        properties = flattenPropertySources(ps);
      }

      if (!properties.isEmpty()) {
        fileContents.put(filename, new LinkedHashMap<>(properties));
      }
    } catch (Exception e) {
      System.err.println("Error loading " + filename + ": " + e.getMessage());
    }
  }

  private void populateMergedConfigCache() {
    Set<String> processed = new HashSet<>();
    for (String filename : fileContents.keySet()) {
      String nameWithoutExt = filename.contains(".")
          ? filename.substring(0, filename.lastIndexOf("."))
          : filename;
      String[] parts = nameWithoutExt.split("_");
      if (parts.length >= 2) {
        String profile = parts[parts.length - 1];
        String app = String.join("_", Arrays.copyOfRange(parts, 0, parts.length - 1));
        String key = app + ":" + profile;
        if (!processed.contains(key)) {
          processed.add(key);
          computeAndCacheMergedConfig(app, profile);
        }
      }
    }
  }

  private void computeAndCacheMergedConfig(String app, String profile) {
    try {
      Map<String, Object> merged = new LinkedHashMap<>();
      Map<String, String> sourceTracking = new LinkedHashMap<>();  // Track sources

      List<String> filenames = getApplicableFilenames(app, profile);

      // Merge in order, tracking which file each key comes from
      for (String filename : filenames) {
        Map<String, Object> fileProps = fileContents.get(filename);
        if (fileProps != null) {
          for (String key : fileProps.keySet()) {
            merged.put(key, fileProps.get(key));
            sourceTracking.put(key, filename);  // Track source for this key
          }
        }
      }

      // Always resolve placeholders
      merged = resolvePlaceholdersInMap(merged);

      // Cache with source tracking information (this is our ONLY merged cache)
      String cacheKey = app + ":" + profile;
      Map<String, Object> mergedWithSources = wrapWithSourceTracking(merged, sourceTracking);
      mergedConfigWithSourcesCache.put(cacheKey, mergedWithSources);
    } catch (Exception e) {
      System.err.println("Error computing merged config for " + app + ":" + profile);
    }
  }

  /**
   * Wraps each value with source tracking information.
   * Transforms: "key": "value" -> "key": { "value": "value", "source": "filename" }
   */
  private Map<String, Object> wrapWithSourceTracking(Map<String, Object> merged, Map<String, String> sourceTracking) {
    Map<String, Object> result = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : merged.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      String source = sourceTracking.getOrDefault(key, "unknown");

      Map<String, Object> wrappedValue = new LinkedHashMap<>();
      wrappedValue.put("value", value);
      wrappedValue.put("source", source);

      result.put(key, wrappedValue);
    }

    return result;
  }

  private List<String> getApplicableFilenames(String app, String profile) {
    List<String> result = new ArrayList<>();
    addIfExists(result, "application_default");
    if (!profile.equals("default")) {
      addIfExists(result, "application_" + profile);
    }
    addIfExists(result, app + "_default");
    if (!profile.equals("default")) {
      addIfExists(result, app + "_" + profile);
    }
    return result;
  }

  private void addIfExists(List<String> list, String pattern) {
    if (fileContents.containsKey(pattern + ".properties")) {
      list.add(pattern + ".properties");
    } else if (fileContents.containsKey(pattern + ".yml")) {
      list.add(pattern + ".yml");
    } else if (fileContents.containsKey(pattern + ".yaml")) {
      list.add(pattern + ".yaml");
    }
  }

  public Map<String, Object> mergedFlat(String app, String profile) {
    String key = app + ":" + profile;
    Map<String, Object> configWithSources = mergedConfigWithSourcesCache.getOrDefault(key, new LinkedHashMap<>());

    if (configWithSources.isEmpty()) {
      throw new NoSuchElementException("No config for app=" + app + ", profile=" + profile);
    }

    // Extract plain values (remove source tracking) for GET API
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : configWithSources.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> wrapped = (Map<String, Object>) value;
        result.put(entry.getKey(), wrapped.get("value"));
      } else {
        result.put(entry.getKey(), value);
      }
    }

    return result;
  }

  public Map<String, Object> mergedDeep(String app, String profile) {
    Map<String, Object> flat = mergedFlat(app, profile);
    return toNestedMap(flat);
  }

  public Map<String, String> getSourcesForConfig(String app, String profile) {
    Map<String, String> sources = new LinkedHashMap<>();
    List<String> filenames = getApplicableFilenames(app, profile);
    for (String filename : filenames) {
      Map<String, Object> props = fileContents.get(filename);
      if (props != null) {
        for (String key : props.keySet()) {
          sources.put(key, filename);
        }
      }
    }
    return sources;
  }

  public List<ConfigSource> loadSources(String app, String profile) {
    List<ConfigSource> sources = new ArrayList<>();
    List<String> filenames = getApplicableFilenames(app, profile);
    for (String filename : filenames) {
      Map<String, Object> properties = fileContents.get(filename);
      if (properties != null) {
        sources.add(new ConfigSource(filename, "<cached>",
            filename.endsWith(".properties") ? "properties" : "yaml",
            new LinkedHashMap<>(properties)));
      }
    }
    if (sources.isEmpty()) {
      throw new NoSuchElementException("No config for app=" + app + ", profile=" + profile);
    }
    return sources;
  }


  public String toPropertiesText(Map<String, Object> flat) {
    StringBuilder sb = new StringBuilder();
    flat.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
    return sb.toString();
  }

  public String toJson(Object data) throws Exception {
    return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
  }

  public String toYaml(Object data) throws Exception {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    return yamlMapper.writeValueAsString(data);
  }

  public Map<String, Object> getCacheStatistics() {
    Map<String, Object> stats = new LinkedHashMap<>();
    stats.put("fileContentsCacheSize", fileContents.size());
    stats.put("mergedConfigWithSourcesCacheSize", mergedConfigWithSourcesCache.size());
    stats.put("totalCacheEntries", fileContents.size() + mergedConfigWithSourcesCache.size());
    stats.put("cachedFilenames", new ArrayList<>(fileContents.keySet()));
    stats.put("cachedConfigurations", new ArrayList<>(mergedConfigWithSourcesCache.keySet()));
    return stats;
  }

  public synchronized void clearCache() {
    fileContents.clear();
    mergedConfigWithSourcesCache.clear();
  }

  /**
   * Returns a copy of the fileContents cache (Map 1).
   * Key: filename (e.g., "order-service_uat.properties")
   * Value: Map of key-value pairs from that file (NOT merged)
   */
  public Map<String, Map<String, Object>> getFileContentsCache() {
    return new LinkedHashMap<>(fileContents);
  }

  /**
   * Returns a copy of the mergedConfigWithSourcesCache.
   * Key: "app:profile" (e.g., "order-service:uat")
   * Value: Merged configuration with source tracking for each property
   * Each property is wrapped as: { "value": actualValue, "source": "filename" }
   */
  public Map<String, Map<String, Object>> getMergedConfigWithSourcesCache() {
    return new LinkedHashMap<>(mergedConfigWithSourcesCache);
  }

  /**
   * Get a specific merged config with source tracking for a given app/profile.
   */
  public Map<String, Object> getMergedConfigWithSources(String app, String profile) {
    String key = app + ":" + profile;
    return mergedConfigWithSourcesCache.getOrDefault(key, new LinkedHashMap<>());
  }

  // ...existing code...

  private Map<String, Object> flattenPropertySources(List<PropertySource<?>> sources) {
    Map<String, Object> out = new LinkedHashMap<>();
    for (PropertySource<?> ps : sources) {
      if (ps.getSource() instanceof Map<?, ?> m) {
        for (var e : m.entrySet()) {
          String key = String.valueOf(e.getKey());
          Object val = unwrapValue(e.getValue());
          out.put(key, val);
        }
      }
    }
    return out;
  }

  private Object unwrapValue(Object val) {
    if (val != null && val.getClass().getName().equals("org.springframework.boot.origin.OriginTrackedValue")) {
      try {
        java.lang.reflect.Method getValue = val.getClass().getMethod("getValue");
        Object unwrapped = getValue.invoke(val);
        return unwrapped != null ? unwrapped : "";
      } catch (Exception e) {
        return val.toString();
      }
    }
    if (val != null) {
      if (val instanceof String || val instanceof Integer || val instanceof Long ||
          val instanceof Double || val instanceof Float || val instanceof Boolean) {
        return val;
      }
      return val.toString();
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadYamlAsNested(FileSystemResource resource) throws IOException {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    Map<String, Object> data = yamlMapper.readValue(resource.getInputStream(), Map.class);
    return data != null ? data : new LinkedHashMap<>();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> flattenMap(Object obj, String prefix) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (obj instanceof Map) {
      for (var entry : ((Map<String, Object>) obj).entrySet()) {
        String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Map) {
          result.putAll(flattenMap(value, key));
        } else {
          result.put(key, value);
        }
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toNestedMap(Map<String, Object> flat) {
    Map<String, Object> root = new LinkedHashMap<>();
    for (var e : flat.entrySet()) {
      putNested(root, e.getKey(), e.getValue());
    }
    return root;
  }

  @SuppressWarnings("unchecked")
  private void putNested(Map<String, Object> root, String key, Object value) {
    String[] parts = key.split("\\.");
    Map<String, Object> cur = root;
    for (int i = 0; i < parts.length - 1; i++) {
      Object next = cur.get(parts[i]);
      if (!(next instanceof Map)) {
        next = new LinkedHashMap<String, Object>();
        cur.put(parts[i], next);
      }
      cur = (Map<String, Object>) next;
    }
    cur.put(parts[parts.length - 1], value);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> resolvePlaceholdersInMap(Map<String, Object> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (var entry : map.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String s) {
        s = resolveVaultPlaceholders(s);
        s = resolveConfigPlaceholders(s, map);
        s = env.resolvePlaceholders(s);
        result.put(entry.getKey(), s);
      } else if (value instanceof Map) {
        result.put(entry.getKey(), resolvePlaceholdersInMap((Map<String, Object>) value));
      } else {
        result.put(entry.getKey(), value);
      }
    }
    return result;
  }

  private String resolveVaultPlaceholders(String value) {
    if (value == null || !value.contains("${")) {
      return value;
    }
    for (String prefix : secretResolverRegistry.getRegisteredPrefixes()) {
      value = resolvePlaceholder(value, prefix,
          secretPath -> secretResolverRegistry.getResolver(prefix).resolveSecret(secretPath));
    }
    return value;
  }

  private String resolveConfigPlaceholders(String value, Map<String, Object> properties) {
    if (value == null || !value.contains("${")) {
      return value;
    }
    String pattern = "\\$\\{([^:}]+)}";
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher matcher = p.matcher(value);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      Object val = properties.get(key);
      String replacement = val != null ? String.valueOf(val) : matcher.group(0);
      matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String resolvePlaceholder(String value, String prefix, java.util.function.Function<String, String> resolver) {
    String pattern = "\\$\\{" + prefix + ":([^}]+)}";
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
    java.util.regex.Matcher matcher = p.matcher(value);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String secretPath = matcher.group(1);
      String resolvedValue = resolver.apply(secretPath);
      matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(resolvedValue));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
