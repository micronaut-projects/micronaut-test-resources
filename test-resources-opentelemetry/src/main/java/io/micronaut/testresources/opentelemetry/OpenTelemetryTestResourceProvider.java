/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.opentelemetry;

import io.micronaut.testresources.core.DefaultTestResourceImages;
import io.micronaut.testresources.core.TestResourcesResolutionException;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a Testcontainers-backed OpenTelemetry LGTM resource.
 */
public class OpenTelemetryTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {

    public static final String OTLP_ENDPOINT = "otel.exporter.otlp.endpoint";
    public static final String OTLP_ENDPOINT_ENV = "OTEL_EXPORTER_OTLP_ENDPOINT";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_OPENTELEMETRY_IMAGE;
    public static final String DISPLAY_NAME = "OpenTelemetry";
    public static final String SIMPLE_NAME = "opentelemetry";
    public static final String OTLP_ENDPOINT_PLACEHOLDER = "${auto.test.resources." + OTLP_ENDPOINT + "}";

    static final String DEFAULT_BACKEND = "lgtm";
    static final String DEFAULT_PROTOCOL = "grpc";
    static final String BACKEND = "containers." + SIMPLE_NAME + ".backend";
    static final String PROTOCOL = "containers." + SIMPLE_NAME + ".protocol";
    static final int OTLP_GRPC_PORT = 4317;
    static final int GRAFANA_PORT = 3000;

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryTestResourceProvider.class);
    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(OTLP_ENDPOINT);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(OTLP_ENDPOINT);
    private static final Set<String> LOGGED_GRAFANA_URLS = ConcurrentHashMap.newKeySet();

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES_LIST;
    }

    @Override
    @SuppressWarnings("java:S2095") // Container lifecycle is managed by AbstractTestContainersProvider/TestContainers.
    protected GenericContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        validateBackend(testResourcesConfig);
        validateProtocol(testResourcesConfig);
        return new GenericContainer<>(imageName)
            .withExposedPorts(OTLP_GRPC_PORT, GRAFANA_PORT)
            .waitingFor(Wait.forHttp("/api/health").forPort(GRAFANA_PORT));
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (OTLP_ENDPOINT.equals(expression)) {
            return List.of(OTLP_ENDPOINT_ENV);
        }
        return List.of();
    }

    @Override
    public String getDisplayName() {
        return providerName(true);
    }

    @Override
    protected String getSimpleName() {
        return providerName(false);
    }

    @Override
    protected String getDefaultImageName() {
        return defaultImage();
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        if (OTLP_ENDPOINT.equals(propertyName)) {
            String grafanaUrl = grafanaUrl(container);
            if (LOGGED_GRAFANA_URLS.add(grafanaUrl)) {
                LOGGER.info("OpenTelemetry LGTM Grafana UI available at {}", grafanaUrl);
            }
            return Optional.of("http://" + container.getHost() + ":" + container.getMappedPort(OTLP_GRPC_PORT));
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName) && !hasExplicitEndpoint(requestedProperties);
    }

    private static boolean hasExplicitEndpoint(Map<String, Object> requestedProperties) {
        return hasConfiguredEndpoint(requestedProperties.get(OTLP_ENDPOINT)) || hasText(requestedProperties.get(OTLP_ENDPOINT_ENV));
    }

    private static boolean hasConfiguredEndpoint(Object value) {
        return hasText(value) && !OTLP_ENDPOINT_PLACEHOLDER.equals(String.valueOf(value).trim());
    }

    private static boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private static void validateBackend(Map<String, Object> testResourcesConfig) {
        String backend = normalize(testResourcesConfig.get(BACKEND), DEFAULT_BACKEND);
        if (!DEFAULT_BACKEND.equals(backend)) {
            throw new TestResourcesResolutionException("Unsupported OpenTelemetry test resource backend '" + backend + "'. Supported backend: lgtm");
        }
    }

    private static void validateProtocol(Map<String, Object> testResourcesConfig) {
        String protocol = normalize(testResourcesConfig.get(PROTOCOL), DEFAULT_PROTOCOL);
        if (!DEFAULT_PROTOCOL.equals(protocol)) {
            throw new TestResourcesResolutionException("Unsupported OpenTelemetry test resource protocol '" + protocol + "'. Supported protocol: grpc");
        }
    }

    private static String normalize(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String stringValue = String.valueOf(value).trim();
        if (stringValue.isBlank()) {
            return defaultValue;
        }
        return stringValue.toLowerCase(Locale.US);
    }

    private static String grafanaUrl(GenericContainer<?> container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(GRAFANA_PORT);
    }

    private static String providerName(boolean displayName) {
        return displayName ? DISPLAY_NAME : SIMPLE_NAME;
    }

    private static String defaultImage() {
        return DEFAULT_IMAGE;
    }
}
