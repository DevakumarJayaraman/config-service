package com.example.configservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "config")
public record ConfigRepoProperties(
    String rootDir,
    long cacheTtlSeconds
) {}
