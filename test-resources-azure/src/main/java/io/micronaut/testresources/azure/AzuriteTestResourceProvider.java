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
package io.micronaut.testresources.azure;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn an Azurite test container.
 */
public class AzuriteTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {
    public static final String PROPERTY_PREFIX = "azure.credential.storage-shared-key.";
    public static final String ACCOUNT_NAME_PROPERTY = "azure.credential.storage-shared-key.account-name";
    public static final String ACCOUNT_KEY_PROPERTY = "azure.credential.storage-shared-key.account-key";
    public static final String CONNECTION_STRING_PROPERTY = "azure.credential.storage-shared-key.connection-string";

    public static final List<String> RESOLVABLE_PROPERTIES_LIST = List.of(
        ACCOUNT_NAME_PROPERTY,
        ACCOUNT_KEY_PROPERTY,
        CONNECTION_STRING_PROPERTY
    );

    public static final String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
    public static final String DEFAULT_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    public static final String DEFAULT_IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.35.0";
    public static final String SIMPLE_NAME = "azurite";
    public static final String DISPLAY_NAME = "Azurite";
    public static final String LISTEN_HOST = "0.0.0.0";

    public static final int BLOB_PORT = 10000;
    public static final int QUEUE_PORT = 10001;
    public static final int TABLE_PORT = 10002;

    private static final Map<String, String> FIXED_PROPERTIES = Map.of(
        ACCOUNT_NAME_PROPERTY, DEFAULT_ACCOUNT_NAME,
        ACCOUNT_KEY_PROPERTY, DEFAULT_ACCOUNT_KEY
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        return List.copyOf(RESOLVABLE_PROPERTIES_LIST);
    }

    @Override
    @SuppressWarnings("java:S2095") // Container lifecycle is managed by AbstractTestContainersProvider/TestContainers.
    protected GenericContainer<?> createContainer(DockerImageName imageName,
                                                  Map<String, Object> requestedProperties,
                                                  Map<String, Object> testResourcesConfig) {
        return new GenericContainer<>(imageName)
            .withCommand(
                SIMPLE_NAME,
                "--blobHost", LISTEN_HOST,
                "--queueHost", LISTEN_HOST,
                "--tableHost", LISTEN_HOST,
                "--skipApiVersionCheck"
            )
            .withExposedPorts(BLOB_PORT, QUEUE_PORT, TABLE_PORT);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        String fixedValue = FIXED_PROPERTIES.get(propertyName);
        if (fixedValue != null) {
            return Optional.of(fixedValue);
        }
        if (CONNECTION_STRING_PROPERTY.equals(propertyName)) {
            return Optional.of(connectionString(container));
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties,
                                   Map<String, Object> testResourcesConfig) {
        return isSharedKeyProperty(propertyName);
    }

    @Override
    public String getDisplayName() {
        return metadata(true);
    }

    @Override
    protected String getSimpleName() {
        return metadata(false);
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    private String connectionString(GenericContainer<?> container) {
        return "DefaultEndpointsProtocol=http;"
            + "AccountName=" + DEFAULT_ACCOUNT_NAME + ";"
            + "AccountKey=" + DEFAULT_ACCOUNT_KEY + ";"
            + "BlobEndpoint=" + endpoint(container, BLOB_PORT) + ";"
            + "QueueEndpoint=" + endpoint(container, QUEUE_PORT) + ";"
            + "TableEndpoint=" + endpoint(container, TABLE_PORT) + ";";
    }

    private String endpoint(GenericContainer<?> container, int port) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(port) + "/" + DEFAULT_ACCOUNT_NAME;
    }

    private boolean isSharedKeyProperty(String propertyName) {
        return propertyName != null && propertyName.startsWith(PROPERTY_PREFIX);
    }

    private String metadata(boolean displayName) {
        return displayName ? DISPLAY_NAME : SIMPLE_NAME;
    }
}
