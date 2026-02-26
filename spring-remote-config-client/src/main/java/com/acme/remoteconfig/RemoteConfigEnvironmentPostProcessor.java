package com.acme.remoteconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.URI;
import java.util.*;

public class RemoteConfigEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PREFIX = "remote.config.";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        RemoteConfigClientProperties props = new RemoteConfigClientProperties();

        props.setEnabled(Boolean.parseBoolean(environment.getProperty(PREFIX + "enabled", "true")));
        props.setBaseUrl(environment.getProperty(PREFIX + "base-url"));
        props.setPath(environment.getProperty(PREFIX + "path", props.getPath()));
        props.setApp(environment.getProperty(PREFIX + "app"));
        props.setProfile(environment.getProperty(PREFIX + "profile"));
        props.setPropertySourceName(environment.getProperty(PREFIX + "property-source-name", props.getPropertySourceName()));
        props.setHighestPrecedence(Boolean.parseBoolean(environment.getProperty(PREFIX + "highest-precedence", "true")));
        props.setFailFast(Boolean.parseBoolean(environment.getProperty(PREFIX + "fail-fast", "false")));

        if (!props.isEnabled() || !StringUtils.hasText(props.getBaseUrl())) {
            return;
        }

        String appName = StringUtils.hasText(props.getApp()) ? props.getApp()
                : environment.getProperty("spring.application.name", "application");

        String profile = StringUtils.hasText(props.getProfile()) ? props.getProfile()
                : (environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "default");

        URI uri = UriComponentsBuilder.fromHttpUrl(props.getBaseUrl())
                .path(props.getPath().replace("{app}", appName).replace("{profile}", profile))
                .build()
                .toUri();

        try {
            SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
            RestTemplate restTemplate = new RestTemplate(rf);

            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RestClientException("Failed to fetch remote config");
            }

            Map<String, Object> map = response.getBody();
            if (map == null || map.isEmpty()) return;

            MapPropertySource propertySource = new MapPropertySource(props.getPropertySourceName(), map);

            MutablePropertySources sources = environment.getPropertySources();
            if (sources.contains(props.getPropertySourceName())) {
                sources.remove(props.getPropertySourceName());
            }

            if (props.isHighestPrecedence()) {
                sources.addFirst(propertySource);
            } else {
                sources.addLast(propertySource);
            }

        } catch (Exception e) {
            if (props.isFailFast()) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
