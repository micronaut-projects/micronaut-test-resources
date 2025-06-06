/*
 * Copyright 2017-2024 original authors
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn an Wiremock test container.
 */
public class WiremockTestResourceProvider extends AbstractTestContainersProvider<WireMockContainer> {

    public static final String WIREMOCK_PORT = "wiremock.port";
    public static final String WIREMOCK_HOST = "wiremock.host";
    public static final String WIREMOCK_URL = "wiremock.url";
    public static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(WIREMOCK_PORT, WIREMOCK_HOST, WIREMOCK_URL);
    public static final String CLI_ARGS_PROPERTY_PATH = "containers.wiremock.cli-args";

    @Override
    public String getDisplayName() {
        return "Wiremock";
    }

    @Override
    protected String getSimpleName() {
        return "wiremock";
    }

    @Override
    protected String getDefaultImageName() {
        return "wiremock/wiremock:3.13.0";
    }

    @Override
    protected WireMockContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        System.out.println(testResourcesConfig);
        return new WireMockContainer(imageName).withCliArg(testResourcesConfig.get(CLI_ARGS_PROPERTY_PATH) != null ? (String) testResourcesConfig.get(CLI_ARGS_PROPERTY_PATH) : "");
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, WireMockContainer container) {
        if (propertyName.equals(WIREMOCK_PORT)) {
            return Optional.of(container.getPort().toString());
        } else if (propertyName.equals(WIREMOCK_HOST)) {
            return Optional.of(container.getHost());
        } else if (propertyName.equals(WIREMOCK_URL)) {
            return Optional.of(container.getBaseUrl());
        }
        return Optional.empty();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES_LIST;
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES_LIST.contains(propertyName);
    }
}
