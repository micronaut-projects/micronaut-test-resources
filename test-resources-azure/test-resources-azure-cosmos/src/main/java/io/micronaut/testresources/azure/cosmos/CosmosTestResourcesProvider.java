/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.azure.cosmos;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CosmosDBEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CosmosTestResourcesProvider extends AbstractTestContainersProvider<CosmosDBEmulatorContainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CosmosTestResourcesProvider.class);
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);

    public static final String SIMPLE_NAME = "cosmosdb";
    public static final String DEFAULT_IMAGE = "mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest";

    public static final String ENDPOINT = "azure.cosmos.endpoint";
    public static final String KEY = "azure.cosmos.key";
    public static final List<String> RESOLVABLE_PROPERTIES_LIST = List.of(ENDPOINT, KEY);
    public static final Set<String> RESOLVABLE_PROPERTIES_SET = Set.of(ENDPOINT, KEY);

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_LIST;
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_SET.contains(propertyName);
    }

    @Override
    protected CosmosDBEmulatorContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new CosmosDBEmulatorWithKeystoreContainer(imageName).withStartupTimeout(STARTUP_TIMEOUT);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, CosmosDBEmulatorContainer container) {
        if (RESOLVABLE_PROPERTIES_SET.contains(propertyName)) {
            return Optional.ofNullable(switch (propertyName) {
                case ENDPOINT -> container.getEmulatorEndpoint();
                case KEY -> container.getEmulatorKey();
                default -> null;
            });
        }
        return Optional.empty();
    }

    private static class CosmosDBEmulatorWithKeystoreContainer extends CosmosDBEmulatorContainer {
        public CosmosDBEmulatorWithKeystoreContainer(DockerImageName imageName) {
            super(imageName);
        }

        @Override
        public void start() {
            super.start();
            configureKeyStore(this);
        }

        private void configureKeyStore(CosmosDBEmulatorContainer emulator) {
            try {
                var keyStoreFile = Files.createTempFile("azure-cosmos-emulator", ".keystore");
                var keyStore = emulator.buildNewKeyStore();
                try (var outputStream = Files.newOutputStream(keyStoreFile)) {
                    keyStore.store(outputStream, emulator.getEmulatorKey().toCharArray());
                }
            } catch (IOException | KeyStoreException | NoSuchAlgorithmException |
                     CertificateException ex) {
                LOGGER.error("Cannot create keystore for CosmosDB", ex);
            }
        }

    }
}
