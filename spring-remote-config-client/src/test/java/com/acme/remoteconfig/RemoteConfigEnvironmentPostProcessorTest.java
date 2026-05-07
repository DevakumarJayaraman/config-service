package com.acme.remoteconfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class RemoteConfigEnvironmentPostProcessorTest {

    @Mock
    RestTemplate restTemplate;

    @Mock
    SpringApplication application;

    RemoteConfigEnvironmentPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new RemoteConfigEnvironmentPostProcessor() {
            @Override
            protected RestTemplate createRestTemplate(RemoteConfigClientProperties props) {
                return restTemplate;
            }
        };
    }

    // ── early-exit cases ─────────────────────────────────────────────────────

    @Test
    void skipsWhenDisabled() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.enabled", "false")
                .withProperty("remote.config.base-url", "http://localhost:8080");

        processor.postProcessEnvironment(env, application);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void skipsWhenBaseUrlIsAbsent() {
        MockEnvironment env = new MockEnvironment();

        processor.postProcessEnvironment(env, application);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void skipsWhenBaseUrlIsBlank() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "   ");

        processor.postProcessEnvironment(env, application);

        verifyNoInteractions(restTemplate);
    }

    // ── happy path ───────────────────────────────────────────────────────────

    @Test
    void addsPropertySourceAsFirstByDefault() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("spring.application.name", "myapp");

        Map<String, Object> body = Map.of("server.port", "9090", "feature.flag", "true");
        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getPropertySources().contains("remoteConfig")).isTrue();
        assertThat(env.getProperty("server.port")).isEqualTo("9090");
        assertThat(env.getPropertySources().iterator().next().getName())
                .as("remoteConfig must be the first (highest-precedence) property source")
                .isEqualTo("remoteConfig");
    }

    @Test
    void addsPropertySourceAsLastWhenHighestPrecedenceFalse() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.highest-precedence", "false");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getPropertySources().contains("remoteConfig")).isTrue();
        assertThat(env.getPropertySources().iterator().next().getName())
                .as("remoteConfig must NOT be first when highest-precedence=false")
                .isNotEqualTo("remoteConfig");
    }

    // ── app / profile resolution ─────────────────────────────────────────────

    @Test
    void usesExplicitAppAndProfileInUri() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.app", "order-service")
                .withProperty("remote.config.profile", "prod");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        verify(restTemplate).getForEntity(
                argThat(uri -> uri.toString().contains("order-service") && uri.toString().contains("prod")),
                eq(Map.class));
    }

    @Test
    void fallsBackToSpringApplicationName() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("spring.application.name", "billing-service");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        verify(restTemplate).getForEntity(
                argThat(uri -> uri.toString().contains("billing-service")),
                eq(Map.class));
    }

    @Test
    void fallsBackToLiteralApplicationWhenNoNameConfigured() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        verify(restTemplate).getForEntity(
                argThat(uri -> uri.toString().contains("/application/")),
                eq(Map.class));
    }

    @Test
    void usesFirstActiveProfile() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080");
        env.setActiveProfiles("staging");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        verify(restTemplate).getForEntity(
                argThat(uri -> uri.toString().contains("staging")),
                eq(Map.class));
    }

    @Test
    void fallsBackToDefaultProfileWhenNoActiveProfiles() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080");
        // no active profiles set

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        verify(restTemplate).getForEntity(
                argThat(uri -> uri.toString().endsWith("/default")),
                eq(Map.class));
    }

    // ── response body edge cases ─────────────────────────────────────────────

    @Test
    void doesNotAddPropertySourceWhenBodyIsNull() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getPropertySources().contains("remoteConfig")).isFalse();
    }

    @Test
    void doesNotAddPropertySourceWhenBodyIsEmpty() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Collections.emptyMap(), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getPropertySources().contains("remoteConfig")).isFalse();
    }

    // ── property source replacement ──────────────────────────────────────────

    @Test
    void replacesStalePropertySourceOnRefresh() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080");

        // Pre-seed an old remoteConfig source
        env.getPropertySources().addFirst(
                new MapPropertySource("remoteConfig", Map.of("old.key", "stale-value")));

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("new.key", "fresh-value"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getProperty("new.key")).isEqualTo("fresh-value");
        assertThat(env.getProperty("old.key")).isNull();
    }

    // ── custom configuration ─────────────────────────────────────────────────

    @Test
    void usesCustomPathTemplate() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://config:8080")
                .withProperty("remote.config.path", "/v2/config/{app}/{profile}")
                .withProperty("remote.config.app", "svc")
                .withProperty("remote.config.profile", "dev");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        verify(restTemplate).getForEntity(
                argThat(uri -> uri.toString().equals("http://config:8080/v2/config/svc/dev")),
                eq(Map.class));
    }

    @Test
    void usesCustomPropertySourceName() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.property-source-name", "centralConfig");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("k", "v"), HttpStatus.OK));

        processor.postProcessEnvironment(env, application);

        assertThat(env.getPropertySources().contains("centralConfig")).isTrue();
        assertThat(env.getPropertySources().contains("remoteConfig")).isFalse();
    }

    // ── error handling ───────────────────────────────────────────────────────

    @Test
    void silentlyIgnoresNetworkExceptionWhenFailFastFalse() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.fail-fast", "false");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatNoException().isThrownBy(() -> processor.postProcessEnvironment(env, application));
    }

    @Test
    void wrapsAndRethrowsNetworkExceptionWhenFailFastTrue() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.fail-fast", "true");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> processor.postProcessEnvironment(env, application))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void silentlyIgnoresNonSuccessResponseWhenFailFastFalse() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.fail-fast", "false");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        assertThatNoException().isThrownBy(() -> processor.postProcessEnvironment(env, application));
        assertThat(env.getPropertySources().contains("remoteConfig")).isFalse();
    }

    @Test
    void wrapsAndRethrowsNonSuccessResponseWhenFailFastTrue() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("remote.config.base-url", "http://localhost:8080")
                .withProperty("remote.config.fail-fast", "true");

        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> processor.postProcessEnvironment(env, application))
                .isInstanceOf(RuntimeException.class);
    }

    // ── createRestTemplate ───────────────────────────────────────────────────

    @Test
    void createRestTemplateAppliesTimeouts() {
        RemoteConfigEnvironmentPostProcessor real = new RemoteConfigEnvironmentPostProcessor();
        RemoteConfigClientProperties props = new RemoteConfigClientProperties();
        props.setConnectTimeout(Duration.ofSeconds(5));
        props.setReadTimeout(Duration.ofSeconds(10));

        RestTemplate template = real.createRestTemplate(props);

        assertThat(template).isNotNull();
    }

    // ── ordering ─────────────────────────────────────────────────────────────

    @Test
    void orderEnsuresRunAfterConfigDataProcessor() {
        // ConfigDataEnvironmentPostProcessor is at HIGHEST_PRECEDENCE + 10.
        // Our processor must be lower priority (higher number) so application.yml is loaded first.
        assertThat(processor.getOrder())
                .isGreaterThan(Ordered.HIGHEST_PRECEDENCE + 10)
                .isEqualTo(Ordered.LOWEST_PRECEDENCE - 5);
    }
}
