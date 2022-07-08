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
package io.micronaut.testresources.oauth2.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.DockerImageName;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A test resource provider which will spawn a Keycloak test container.
 */
public class KeycloakTestResourceProvider extends AbstractTestContainersProvider<KeycloakContainer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakTestResourceProvider.class);

    private static final String KEYCLOAK_PREFIX = "micronaut.security.oauth2.clients.keycloak.";
    private static final String CLIENT_SECRET = KEYCLOAK_PREFIX + "client-secret";
    private static final String CLIENT_ID = KEYCLOAK_PREFIX + "client-id";
    private static final String ISSUER = KEYCLOAK_PREFIX + "openid.issuer";
    private static final String JWT_TOKEN_URL = "micronaut.security.token.jwt.signatures.jwks.keycloak.url";

    private static final String DEFAULT_IMAGE = "quay.io/keycloak/keycloak";

    private static final List<String> SUPPORTED_PROPERTIES_LIST;
    private static final Set<String> SUPPORTED_PROPERTIES_SET;

    private static final String TEST_REALM = "realm";
    private static final String TEST_REALM_DEFAULT = "master";
    private static final String TEST_CLIENT = "client-id";
    private static final String TEST_CLIENT_DEFAULT = "client-id";
    private static final String TEST_SECRET = "client-secret";
    private static final String TEST_SECRET_DEFAULT = "test-secret";

    static {
        List<String> supported = new ArrayList<>();
        supported.add(CLIENT_SECRET);
        supported.add(CLIENT_ID);
        supported.add(ISSUER);
        supported.add(JWT_TOKEN_URL);
        SUPPORTED_PROPERTIES_LIST = Collections.unmodifiableList(supported);
        SUPPORTED_PROPERTIES_SET = Collections.unmodifiableSet(new HashSet<>(supported));
    }

    private final AtomicBoolean clientConfigured = new AtomicBoolean();
    private String realm;
    private String clientId;
    private String clientSecret;

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES_LIST;
    }

    @Override
    protected String getSimpleName() {
        return "keycloak";
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected KeycloakContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return new KeycloakContainer(imageName.asCanonicalNameString()) {
            @Override
            public void start() {
                super.start();
                assertConfigured(this, testResourcesConfiguration);
            }
        };
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, KeycloakContainer container) {
        switch (propertyName) {
            case CLIENT_SECRET:
                return Optional.of(clientSecret);
            case CLIENT_ID:
                return Optional.of(clientId);
            case ISSUER:
                return Optional.of(container.getAuthServerUrl() + "/realm/" + realm);
            case JWT_TOKEN_URL:
                return Optional.of(container.getAuthServerUrl() + "/realm/" + realm + "/protocol/openid-connect/certs");
            default:
        }
        return Optional.empty();
    }

    private void assertConfigured(KeycloakContainer container, Map<String, Object> testResourcesConfiguration) {
        if (clientConfigured.compareAndSet(false, true)) {
            Keycloak keycloakAdminClient = container.getKeycloakAdminClient();
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            realm = fromConfigurationOrDefault(testResourcesConfiguration, TEST_REALM, TEST_REALM_DEFAULT);
            clientId = fromConfigurationOrDefault(testResourcesConfiguration, TEST_CLIENT, TEST_CLIENT_DEFAULT);
            clientSecret = fromConfigurationOrDefault(testResourcesConfiguration, TEST_SECRET, TEST_SECRET_DEFAULT);
            clientRepresentation.setClientId(clientId);
            clientRepresentation.setClientId(clientSecret);
            try (Response response = keycloakAdminClient.realm(realm).clients().create(clientRepresentation)) {
                LOGGER.debug("Keycloak admin client answered: {}", response.getStatusInfo());
            }
        }
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return SUPPORTED_PROPERTIES_SET.contains(propertyName);
    }

    private static String fromConfigurationOrDefault(Map<String, Object> testResourcesConfiguration, String key, String defaultValue) {
        Object value = testResourcesConfiguration.get("containers.keycloak." + key);
        if (value != null) {
            return String.valueOf(value);
        }
        return defaultValue;
    }
}
