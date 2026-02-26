package com.acme.remoteconfig;

import java.time.Duration;

public class RemoteConfigClientProperties {

    private boolean enabled = true;
    private String baseUrl;
    private String path = "/api/config/{app}/{profile}";
    private String app;
    private String profile;
    private String propertySourceName = "remoteConfig";
    private boolean highestPrecedence = true;
    private boolean failFast = false;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(3);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public String getPropertySourceName() { return propertySourceName; }
    public void setPropertySourceName(String propertySourceName) { this.propertySourceName = propertySourceName; }

    public boolean isHighestPrecedence() { return highestPrecedence; }
    public void setHighestPrecedence(boolean highestPrecedence) { this.highestPrecedence = highestPrecedence; }

    public boolean isFailFast() { return failFast; }
    public void setFailFast(boolean failFast) { this.failFast = failFast; }

    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
}
