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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn an Azurite test container.
 */
public class AzuriteTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {
    public static final String ACCOUNT_NAME_PROPERTY = "azure.credential.storage-shared-key.account-name";
    public static final String ACCOUNT_KEY_PROPERTY = "azure.credential.storage-shared-key.account-key";
    public static final String CONNECTION_STRING_PROPERTY = "azure.credential.storage-shared-key.connection-string";

    public static final List<String> RESOLVABLE_PROPERTIES_LIST = Collections.unmodifiableList(Arrays.asList(
        ACCOUNT_NAME_PROPERTY,
        ACCOUNT_KEY_PROPERTY,
        CONNECTION_STRING_PROPERTY
    ));
    public static final Set<String> RESOLVABLE_PROPERTIES_SET =
        Collections.unmodifiableSet(new HashSet<>(RESOLVABLE_PROPERTIES_LIST));

    public static final String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
    public static final String DEFAULT_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    public static final String DEFAULT_IMAGE = "mcr.microsoft.com/azure-storage/azurite:3.35.0";
    public static final String SIMPLE_NAME = "azurite";
    public static final String DISPLAY_NAME = "Azurite";

    public static final int BLOB_PORT = 10000;
    public static final int QUEUE_PORT = 10001;
    public static final int TABLE_PORT = 10002;

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_LIST;
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
    protected GenericContainer<?> createContainer(DockerImageName imageName,
                                                  Map<String, Object> requestedProperties,
                                                  Map<String, Object> testResourcesConfig) {
        return new GenericContainer<>(imageName)
            .withCommand(
                "azurite",
                "--blobHost", "0.0.0.0",
                "--queueHost", "0.0.0.0",
                "--tableHost", "0.0.0.0",
                "--skipApiVersionCheck"
            )
            .withExposedPorts(BLOB_PORT, QUEUE_PORT, TABLE_PORT);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        if (ACCOUNT_NAME_PROPERTY.equals(propertyName)) {
            return Optional.of(DEFAULT_ACCOUNT_NAME);
        }
        if (ACCOUNT_KEY_PROPERTY.equals(propertyName)) {
            return Optional.of(DEFAULT_ACCOUNT_KEY);
        }
        if (CONNECTION_STRING_PROPERTY.equals(propertyName)) {
            return Optional.of(connectionString(container));
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties,
                                   Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_SET.contains(propertyName);
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
}
