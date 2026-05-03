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
package io.micronaut.testresources.wiremock;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which spawns a WireMock test container.
 */
public class WireMockTestResourceProvider extends AbstractTestContainersProvider<WireMockContainer> {

    static final String WIREMOCK_HOST = "wiremock.host";
    static final String WIREMOCK_PORT = "wiremock.port";
    static final String WIREMOCK_URL = "wiremock.url";
    static final String SIMPLE_NAME = "wiremock";
    static final String DISPLAY_NAME = "WireMock";
    static final String DEFAULT_IMAGE = "wiremock/wiremock:3.13.2-2";
    static final String CLI_ARGS_PROPERTY = "containers.wiremock.cli-args";

    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(
        WIREMOCK_HOST,
        WIREMOCK_PORT,
        WIREMOCK_URL
    );
    private static final Set<String> SUPPORTED_PROPERTIES = Set.copyOf(SUPPORTED_PROPERTIES_LIST);

    /**
     * Creates a WireMock test resource provider.
     */
    public WireMockTestResourceProvider() {
        // Required for service loading.
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES_LIST;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected WireMockContainer createContainer(DockerImageName imageName,
                                                Map<String, Object> requestedProperties,
                                                Map<String, Object> testResourcesConfig) {
        WireMockContainer container = new WireMockContainer(imageName);
        addCliArgs(container, testResourcesConfig.get(CLI_ARGS_PROPERTY));
        return container;
    }

    private static void addCliArgs(WireMockContainer container, Object configuredCliArgs) {
        if (configuredCliArgs instanceof Iterable<?> cliArgs) {
            cliArgs.forEach(arg -> container.withCliArg(String.valueOf(arg)));
        } else if (configuredCliArgs != null) {
            container.withCliArg(String.valueOf(configuredCliArgs));
        }
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, WireMockContainer container) {
        if (WIREMOCK_HOST.equals(propertyName)) {
            return Optional.of(container.getHost());
        }
        if (WIREMOCK_PORT.equals(propertyName)) {
            return Optional.of(String.valueOf(container.getPort()));
        }
        if (WIREMOCK_URL.equals(propertyName)) {
            return Optional.of(container.getBaseUrl());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }
}
