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

import io.micronaut.testresources.core.DefaultTestResourceImages;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A test resource provider which will spawn an Azure Cosmos emulator container.
 */
public class AzureCosmosTestResourceProvider extends AbstractTestContainersProvider<CosmosDBEmulatorContainer> {
    public static final String SIMPLE_NAME = "azure-cosmos";
    public static final String DISPLAY_NAME = "Azure Cosmos Emulator";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_AZURE_COSMOS_IMAGE;
    public static final String ENDPOINT = "azure.cosmos.endpoint";
    public static final String KEY = "azure.cosmos.key";

    static final int COSMOS_PORT = 8081;
    static final int VNEXT_HEALTH_PORT = 8080;

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);
    private static final String VNEXT_PREVIEW_TAG = "vnext-preview";
    private static final String TESTCONTAINERS_COMPATIBLE_IMAGE = "mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator";
    private static final List<String> RESOLVABLE_PROPERTIES = List.of(ENDPOINT, KEY);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.copyOf(RESOLVABLE_PROPERTIES);
    private static final Map<String, Function<CosmosDBEmulatorContainer, String>> PROPERTY_RESOLVERS = Map.of(
        ENDPOINT, CosmosDBEmulatorContainer::getEmulatorEndpoint,
        KEY, CosmosDBEmulatorContainer::getEmulatorKey
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, CosmosDBEmulatorContainer container) {
        return Optional.ofNullable(PROPERTY_RESOLVERS.get(propertyName)).map(resolver -> resolver.apply(container));
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }

    @SuppressWarnings("java:S2095") // Container lifecycle is transferred to the shared Testcontainers cache.
    @Override
    protected CosmosDBEmulatorContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        DockerImageName compatibleImage = imageName.asCompatibleSubstituteFor(TESTCONTAINERS_COMPATIBLE_IMAGE);
        CosmosDBEmulatorContainer container = new CosmosDBEmulatorContainer(compatibleImage).withStartupTimeout(STARTUP_TIMEOUT);
        if (VNEXT_PREVIEW_TAG.equals(imageName.getVersionPart())) {
            container
                .withEnv("PROTOCOL", "https")
                .withExposedPorts(COSMOS_PORT, VNEXT_HEALTH_PORT)
                .waitingFor(Wait.forHttp("/ready").forPort(VNEXT_HEALTH_PORT).forStatusCode(200).withStartupTimeout(STARTUP_TIMEOUT));
        }
        return container;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }
}
