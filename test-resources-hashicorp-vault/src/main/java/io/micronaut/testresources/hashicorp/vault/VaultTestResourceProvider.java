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
package io.micronaut.testresources.hashicorp.vault;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a Hashicorp Vault test container.
 */
public class VaultTestResourceProvider extends AbstractTestContainersProvider<VaultContainer<?>> {

    public static final String VAULT_CLIENT_URI_PROPERTY = "vault.client.uri";
    public static final String VAULT_CLIENT_TOKEN_PROPERTY = "vault.client.token";
    public static final List<String> RESOLVABLE_PROPERTIES_LIST = Collections.unmodifiableList(Arrays.asList(
        VAULT_CLIENT_URI_PROPERTY,
        VAULT_CLIENT_TOKEN_PROPERTY
    ));
    public static final Set<String> RESOLVABLE_PROPERTIES_SET = Collections.unmodifiableSet(new HashSet<>(RESOLVABLE_PROPERTIES_LIST));
    public static final String VAULT_CLIENT_TOKEN_VALUE = "vault-token";
    public static final String DEFAULT_IMAGE = "vault";
    public static final String SIMPLE_NAME = "hashicorp-vault";
    public static final String HASHICORP_VAULT_TOKEN_KEY = "containers.hashicorp-vault.token";
    public static final String TEST_RESOURCES_CONTAINERS_PATH_KEY = "containers.hashicorp-vault.path";
    public static final String HASHICORP_VAULT_SECRETS_KEY = "containers.hashicorp-vault.secrets";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_LIST;
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
    protected VaultContainer<?> createContainer(DockerImageName imageName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        VaultContainer<?> container = new VaultContainer<>(imageName);
        container.withVaultToken(testResourcesConfiguration.getOrDefault(HASHICORP_VAULT_TOKEN_KEY, VAULT_CLIENT_TOKEN_VALUE).toString());

        if (testResourcesConfiguration.containsKey(TEST_RESOURCES_CONTAINERS_PATH_KEY) && testResourcesConfiguration.containsKey(HASHICORP_VAULT_SECRETS_KEY)) {
            @SuppressWarnings("unchecked")
            List<String> secrets = (List<String>) testResourcesConfiguration.get(HASHICORP_VAULT_SECRETS_KEY);
            container.withSecretInVault(
                testResourcesConfiguration.get(TEST_RESOURCES_CONTAINERS_PATH_KEY).toString(),
                secrets.get(0),
                secrets.subList(1, secrets.size()).toArray(new String[0])
            );
        }
        return container;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, VaultContainer<?> container) {
        if (VAULT_CLIENT_URI_PROPERTY.equals(propertyName)) {
            return Optional.of("http://" + container.getHost() + ":" + container.getMappedPort(8200));
        } else if (VAULT_CLIENT_TOKEN_PROPERTY.equals(propertyName)) {
            return Optional.of(VAULT_CLIENT_TOKEN_VALUE);
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        return RESOLVABLE_PROPERTIES_SET.contains(propertyName);
    }
}
