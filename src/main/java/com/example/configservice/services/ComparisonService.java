package com.example.configservice.services;

import com.example.configservice.dto.ConfigDifference;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for comparing configurations across different profiles and applications.
 * Utilizes in-memory caches from ConfigRepoService for efficient comparison.
 */
@Service
public class ComparisonService {

    private final ConfigRepoService configRepoService;

    public ComparisonService(ConfigRepoService configRepoService) {
        this.configRepoService = configRepoService;
    }

    /**
     * Compare sources (raw file contents) between two profiles for a specific app.
     * Shows ONLY specific app profile file comparison: app_uat vs app_prod (no merging)
     * <p>
     * Example: /config/compare/source/order-service/uat/prod
     * Compares: order-service_uat.properties vs order-service_prod.properties
     */
    public Map<String, Object> compareSourcesForApp(String app, String profileA, String profileB) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", app);
        response.put("profileA", profileA);
        response.put("profileB", profileB);
        response.put("comparisonType", "sources");

        // Get fileContents cache
        Map<String, Map<String, Object>> fileContentsCache = configRepoService.getFileContentsCache();

        // Build filenames: app_profile.properties or app_profile.yml
        String filenameA = findFileForAppProfile(fileContentsCache, app, profileA);
        String filenameB = findFileForAppProfile(fileContentsCache, app, profileB);

        // Extract properties from specific files only
        Map<String, String> configA = extractFileProperties(fileContentsCache, filenameA);
        Map<String, String> configB = extractFileProperties(fileContentsCache, filenameB);

        // Compute differences
        List<ConfigDifference> differences = computeSourceDifferences(app, configA, configB, profileA, profileB, filenameA, filenameB);
        response.put("differences", differences);
        response.put("fileA", filenameA != null ? filenameA : app + "_" + profileA);
        response.put("fileB", filenameB != null ? filenameB : app + "_" + profileB);
        response.put("statistics", calculateStatistics(differences, profileA, profileB));

        return response;
    }

    /**
     * Compare sources (raw file contents) between two profiles for all applications.
     * Shows ONLY specific app profile file comparison for each app: app_uat vs app_prod (no merging)
     * <p>
     * Example: /config/compare/source/all/uat/prod
     */
    public Map<String, Object> compareSourcesForAllApps(String profileA, String profileB) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apps", "all");
        response.put("profileA", profileA);
        response.put("profileB", profileB);
        response.put("comparisonType", "sources");

        List<Map<String, Object>> appComparisons = new ArrayList<>();
        Map<String, Integer> aggregateStats = new LinkedHashMap<>();
        aggregateStats.put("totalApps", 0);
        aggregateStats.put("total", 0);
        aggregateStats.put("match", 0);
        aggregateStats.put("noMatch", 0);
        aggregateStats.put(profileA + "-missing", 0);
        aggregateStats.put(profileB + "-missing", 0);

        // Get fileContents cache to find all apps
        Map<String, Map<String, Object>> fileContentsCache = configRepoService.getFileContentsCache();
        Set<String> applications = extractApplicationsFromFileCache(fileContentsCache);

        for (String app : applications) {
            try {
                // Find specific app profile files
                String filenameA = findFileForAppProfile(fileContentsCache, app, profileA);
                String filenameB = findFileForAppProfile(fileContentsCache, app, profileB);

                // Only include if at least one file exists
                if (filenameA != null || filenameB != null) {
                    Map<String, String> configA = extractFileProperties(fileContentsCache, filenameA);
                    Map<String, String> configB = extractFileProperties(fileContentsCache, filenameB);

                    List<ConfigDifference> differences = computeSourceDifferences(app, configA, configB, profileA, profileB, filenameA, filenameB);
                    Map<String, Integer> appStats = calculateStatistics(differences, profileA, profileB);

                    Map<String, Object> appComparison = new LinkedHashMap<>();
                    appComparison.put("app", app);
                    appComparison.put("fileA", filenameA != null ? filenameA : app + "_" + profileA);
                    appComparison.put("fileB", filenameB != null ? filenameB : app + "_" + profileB);
                    appComparison.put("differences", differences);
                    appComparison.put("statistics", appStats);

                    appComparisons.add(appComparison);

                    // Aggregate stats - exact same keys as individual app stats
                    aggregateStats.put("totalApps", aggregateStats.get("totalApps") + 1);
                    aggregateStats.put("total", aggregateStats.get("total") + appStats.get("total"));
                    aggregateStats.put("match", aggregateStats.get("match") + appStats.get("match"));
                    aggregateStats.put("noMatch", aggregateStats.get("noMatch") + appStats.get("noMatch"));
                    aggregateStats.put(profileA + "-missing",
                            aggregateStats.get(profileA + "-missing") + appStats.get(profileA + "-missing"));
                    aggregateStats.put(profileB + "-missing",
                            aggregateStats.get(profileB + "-missing") + appStats.get(profileB + "-missing"));
                }
            } catch (Exception e) {
                // Skip apps that don't have configs for these profiles
            }
        }

        response.put("appComparisons", appComparisons);
        response.put("statistics", aggregateStats);
        return response;
    }

    /**
     * Compare merged values between two profiles for a specific app.
     * Shows merged configuration with source tracking for each property.
     * <p>
     * Example: /config/compare/values/order-service/uat/prod
     */
    public Map<String, Object> compareValuesForApp(String app, String profileA, String profileB) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", app);
        response.put("profileA", profileA);
        response.put("profileB", profileB);
        response.put("comparisonType", "values");

        // Get merged configs with source tracking
        Map<String, Object> mergedA = configRepoService.getMergedConfigWithSources(app, profileA);
        Map<String, Object> mergedB = configRepoService.getMergedConfigWithSources(app, profileB);

        // Compute differences with source tracking
        List<ConfigDifference> differences = computeValueDifferences(mergedA, mergedB, profileA, profileB);

        response.put("differences", differences);
        response.put("statistics", calculateStatistics(differences, profileA, profileB));

        return response;
    }

    /**
     * Compare merged values between two profiles for all applications.
     * Shows merged configuration with source tracking for each property.
     * <p>
     * Example: /config/compare/values/all/uat/prod
     */
    public Map<String, Object> compareValuesForAllApps(String profileA, String profileB) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apps", "all");
        response.put("profileA", profileA);
        response.put("profileB", profileB);
        response.put("comparisonType", "values");

        List<Map<String, Object>> appComparisons = new ArrayList<>();
        Map<String, Integer> aggregateStats = new LinkedHashMap<>();
        aggregateStats.put("totalApps", 0);
        aggregateStats.put("total", 0);
        aggregateStats.put("match", 0);
        aggregateStats.put("noMatch", 0);
        aggregateStats.put(profileA + "-missing", 0);
        aggregateStats.put(profileB + "-missing", 0);

        // Get all applications from cache
        Set<String> applications = getApplicationList();

        for (String app : applications) {
            try {
                Map<String, Object> mergedA = configRepoService.getMergedConfigWithSources(app, profileA);
                Map<String, Object> mergedB = configRepoService.getMergedConfigWithSources(app, profileB);

                if (!mergedA.isEmpty() || !mergedB.isEmpty()) {
                    List<ConfigDifference> differences = computeValueDifferences(mergedA, mergedB, profileA, profileB);
                    Map<String, Integer> appStats = calculateStatistics(differences, profileA, profileB);

                    Map<String, Object> appComparison = new LinkedHashMap<>();
                    appComparison.put("app", app);
                    appComparison.put("differences", differences);
                    appComparison.put("statistics", appStats);

                    appComparisons.add(appComparison);

                    // Aggregate stats - exact same keys as individual app stats
                    aggregateStats.put("totalApps", aggregateStats.get("totalApps") + 1);
                    aggregateStats.put("total", aggregateStats.get("total") + appStats.get("total"));
                    aggregateStats.put("match", aggregateStats.get("match") + appStats.get("match"));
                    aggregateStats.put("noMatch", aggregateStats.get("noMatch") + appStats.get("noMatch"));
                    aggregateStats.put(profileA + "-missing",
                            aggregateStats.get(profileA + "-missing") + appStats.get(profileA + "-missing"));
                    aggregateStats.put(profileB + "-missing",
                            aggregateStats.get(profileB + "-missing") + appStats.get(profileB + "-missing"));
                }
            } catch (Exception e) {
                // Skip apps that don't have configs for these profiles
            }
        }

        response.put("appComparisons", appComparisons);
        response.put("statistics", aggregateStats);
        return response;
    }

    // ============ HELPER METHODS ============

    /**
     * Generic method to calculate statistics from a list of differences.
     * Returns a map with counts: total, match, noMatch, missing
     *
     * @param differences List of ConfigDifference objects
     * @return Map containing statistics with keys: total, match, noMatch, {profileA}-missing, {profileB}-missing
     */
    private Map<String, Integer> calculateStatistics(List<ConfigDifference> differences, String profileA, String profileB) {
        Map<String, Integer> stats = new LinkedHashMap<>();

        int total = differences.size();
        int match = 0;
        int noMatch = 0;
        int profileAMissing = 0;
        int profileBMissing = 0;

        for (ConfigDifference diff : differences) {
            String status = diff.getStatus();
            if ("SAME".equals(status)) {
                match++;
            } else if ("DIFF".equals(status)) {
                noMatch++;
            } else if ((profileA + "-missing").equals(status)) {
                profileAMissing++;
            } else if ((profileB + "-missing").equals(status)) {
                profileBMissing++;
            }
        }

        stats.put("total", total);
        stats.put("match", match);
        stats.put("noMatch", noMatch);
        stats.put(profileA + "-missing", profileAMissing);
        stats.put(profileB + "-missing", profileBMissing);

        return stats;
    }

    /**
     * Capitalize first letter of a string.
     * Example: "uat" -> "Uat", "prod" -> "Prod"
     */
    private String capitalizeName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Find file for a specific app and profile in fileContents cache.
     * Tries both .properties and .yml extensions.
     */
    private String findFileForAppProfile(Map<String, Map<String, Object>> fileContentsCache, String app, String profile) {
        String filenameProperties = app + "_" + profile + ".properties";
        String filenameYml = app + "_" + profile + ".yml";
        String filenameYaml = app + "_" + profile + ".yaml";

        if (fileContentsCache.containsKey(filenameProperties)) {
            return filenameProperties;
        }
        if (fileContentsCache.containsKey(filenameYml)) {
            return filenameYml;
        }
        if (fileContentsCache.containsKey(filenameYaml)) {
            return filenameYaml;
        }
        return null;
    }

    /**
     * Extract properties from a specific file in the fileContents cache.
     */
    private Map<String, String> extractFileProperties(Map<String, Map<String, Object>> fileContentsCache, String filename) {
        Map<String, String> properties = new LinkedHashMap<>();

        if (filename != null && fileContentsCache.containsKey(filename)) {
            Map<String, Object> fileProps = fileContentsCache.get(filename);
            for (Map.Entry<String, Object> entry : fileProps.entrySet()) {
                properties.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        return properties;
    }

    /**
     * Extract all applications from fileContents cache keys.
     * Parses filenames like "order-service_uat.properties" to extract "order-service"
     */
    private Set<String> extractApplicationsFromFileCache(Map<String, Map<String, Object>> fileContentsCache) {
        Set<String> applications = new HashSet<>();

        for (String filename : fileContentsCache.keySet()) {
            // Remove extension
            String nameWithoutExt = filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf("."))
                    : filename;

            // Remove profile suffix (last part after _)
            String[] parts = nameWithoutExt.split("_");
            if (parts.length >= 2) {
                // Everything except the last part is the app name
                String app = String.join("_", Arrays.copyOfRange(parts, 0, parts.length - 1));
                if (!app.equals("application")) {  // Skip global application files
                    applications.add(app);
                }
            }
        }

        return applications;
    }

    /**
     * Compute differences between two raw config maps (source comparison).
     */
    private List<ConfigDifference> computeSourceDifferences(String app, Map<String, String> configA,
                                                            Map<String, String> configB,
                                                            String profileA, String profileB,
                                                            String filenameA, String filenameB) {
        List<ConfigDifference> differences = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();

        allKeys.addAll(configA.keySet());
        allKeys.addAll(configB.keySet());

        // Use actual filenames for source tracking
        String sourceA = filenameA != null ? filenameA : app + "_" + profileA;
        String sourceB = filenameB != null ? filenameB : app + "_" + profileB;

        for (String key : allKeys) {
            String valueA = configA.get(key);
            String valueB = configB.get(key);

            String status;
            if (valueA == null && valueB != null) {
                status = profileA + "-missing";
            } else if (valueA != null && valueB == null) {
                status = profileB + "-missing";
            } else if (Objects.equals(valueA, valueB)) {
                status = "SAME";
            } else {
                status = "DIFF";
            }

            differences.add(new ConfigDifference(
                    key,
                    valueA != null ? valueA : "NOT_FOUND",
                    valueB != null ? valueB : "NOT_FOUND",
                    sourceA,
                    sourceB,
                    status
            ));
        }

        // Sort by status (DIFF/missing first) then by key
        differences.sort((d1, d2) -> {
            int statusCompare = d2.getStatus().compareTo(d1.getStatus());
            if (statusCompare != 0) return statusCompare;
            return d1.getKey().compareTo(d2.getKey());
        });

        return differences;
    }

    /**
     * Compute differences between two merged configs with source tracking.
     * Each property has {value, source} structure.
     * Uses input profileA/profileB for status messages instead of extracting from source.
     */
    @SuppressWarnings("unchecked")
    private List<ConfigDifference> computeValueDifferences(Map<String, Object> mergedA,
                                                           Map<String, Object> mergedB,
                                                           String profileA, String profileB) {
        List<ConfigDifference> differences = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();

        allKeys.addAll(mergedA.keySet());
        allKeys.addAll(mergedB.keySet());

        for (String key : allKeys) {
            Object objA = mergedA.get(key);
            Object objB = mergedB.get(key);

            // Extract value and source from wrapped objects
            String valueA = "NOT_FOUND";
            String sourceA = "NOT_FOUND";
            if (objA instanceof Map) {
                Map<String, Object> mapA = (Map<String, Object>) objA;
                valueA = String.valueOf(mapA.get("value"));
                sourceA = String.valueOf(mapA.get("source"));
            }

            String valueB = "NOT_FOUND";
            String sourceB = "NOT_FOUND";
            if (objB instanceof Map) {
                Map<String, Object> mapB = (Map<String, Object>) objB;
                valueB = String.valueOf(mapB.get("value"));
                sourceB = String.valueOf(mapB.get("source"));
            }

            String status;
            if ("NOT_FOUND".equals(valueA) && !"NOT_FOUND".equals(valueB)) {
                status = profileA + "-missing";
            } else if (!"NOT_FOUND".equals(valueA) && "NOT_FOUND".equals(valueB)) {
                status = profileB + "-missing";
            } else if (Objects.equals(valueA, valueB)) {
                status = "SAME";
            } else {
                status = "DIFF";
            }

            differences.add(new ConfigDifference(
                    key,
                    valueA,
                    valueB,
                    sourceA,
                    sourceB,
                    status
            ));
        }

        // Sort by status (DIFF first) then by key
        differences.sort((d1, d2) -> {
            int statusCompare = d2.getStatus().compareTo(d1.getStatus());
            if (statusCompare != 0) return statusCompare;
            return d1.getKey().compareTo(d2.getKey());
        });

        return differences;
    }


    /**
     * Get list of all applications from merged cache keys.
     */
    private Set<String> getApplicationList() {
        Set<String> applications = new HashSet<>();

        // Extract app names from mergedConfigWithSourcesCache keys
        // Keys are in format: "app:profile"
        Map<String, Map<String, Object>> mergedCache = configRepoService.getMergedConfigWithSourcesCache();

        for (String key : mergedCache.keySet()) {
            String[] parts = key.split(":");
            if (parts.length >= 1) {
                applications.add(parts[0]);
            }
        }

        return applications;
    }
}
