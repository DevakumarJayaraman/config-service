package com.acme.remoteconfig;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Setter
@Getter
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

}
