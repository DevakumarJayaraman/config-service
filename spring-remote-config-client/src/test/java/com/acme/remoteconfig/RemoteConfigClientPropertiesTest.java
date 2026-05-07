package com.acme.remoteconfig;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteConfigClientPropertiesTest {

    @Test
    void defaultValues() {
        RemoteConfigClientProperties props = new RemoteConfigClientProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getBaseUrl()).isNull();
        assertThat(props.getPath()).isEqualTo("/api/config/{app}/{profile}");
        assertThat(props.getApp()).isNull();
        assertThat(props.getProfile()).isNull();
        assertThat(props.getPropertySourceName()).isEqualTo("remoteConfig");
        assertThat(props.isHighestPrecedence()).isTrue();
        assertThat(props.isFailFast()).isFalse();
        assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(props.getReadTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void settersOverrideDefaults() {
        RemoteConfigClientProperties props = new RemoteConfigClientProperties();

        props.setEnabled(false);
        props.setBaseUrl("http://config-service:8080");
        props.setPath("/custom/{app}/{profile}");
        props.setApp("payment-service");
        props.setProfile("production");
        props.setPropertySourceName("customSource");
        props.setHighestPrecedence(false);
        props.setFailFast(true);
        props.setConnectTimeout(Duration.ofSeconds(5));
        props.setReadTimeout(Duration.ofSeconds(10));

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getBaseUrl()).isEqualTo("http://config-service:8080");
        assertThat(props.getPath()).isEqualTo("/custom/{app}/{profile}");
        assertThat(props.getApp()).isEqualTo("payment-service");
        assertThat(props.getProfile()).isEqualTo("production");
        assertThat(props.getPropertySourceName()).isEqualTo("customSource");
        assertThat(props.isHighestPrecedence()).isFalse();
        assertThat(props.isFailFast()).isTrue();
        assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
}
