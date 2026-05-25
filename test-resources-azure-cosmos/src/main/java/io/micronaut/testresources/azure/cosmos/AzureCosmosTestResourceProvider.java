/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.testresources.azure.cosmos;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn an Azure Cosmos emulator container.
 */
public class AzureCosmosTestResourceProvider extends AbstractTestContainersProvider<CosmosDBEmulatorContainer> {
    public static final String SIMPLE_NAME = "azure-cosmos";
    public static final String DISPLAY_NAME = "Azure Cosmos Emulator";
    public static final String DEFAULT_IMAGE = "mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest";
    public static final String ENDPOINT = "azure.cosmos.endpoint";
    public static final String KEY = "azure.cosmos.key";

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);
    private static final List<String> RESOLVABLE_PROPERTIES = List.of(ENDPOINT, KEY);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.copyOf(RESOLVABLE_PROPERTIES);

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected CosmosDBEmulatorContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new CosmosDBEmulatorContainer(imageName)
            .withStartupTimeout(STARTUP_TIMEOUT);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, CosmosDBEmulatorContainer container) {
        if (ENDPOINT.equals(propertyName)) {
            return Optional.of(container.getEmulatorEndpoint());
        }
        if (KEY.equals(propertyName)) {
            return Optional.of(container.getEmulatorKey());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
