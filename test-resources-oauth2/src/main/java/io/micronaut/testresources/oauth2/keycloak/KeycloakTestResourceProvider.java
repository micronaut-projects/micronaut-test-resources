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
package io.micronaut.testresources.oauth2.keycloak;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A test resource provider which will spawn a Keycloak test container.
 */
public class KeycloakTestResourceProvider extends AbstractTestContainersProvider<KeycloakTestResourceProvider.KeycloakContainer> {
    public static final String DISPLAY_NAME = "Keycloak";
    public static final String SIMPLE_NAME = "keycloak";
    public static final String DEFAULT_IMAGE = "quay.io/keycloak/keycloak:26.6.1";

    public static final String CLIENT_ID = "micronaut.security.oauth2.clients.keycloak.client-id";
    public static final String CLIENT_SECRET = "micronaut.security.oauth2.clients.keycloak.client-secret";
    public static final String ISSUER = "micronaut.security.oauth2.clients.keycloak.openid.issuer";
    public static final String JWKS_URL = "micronaut.security.token.jwt.signatures.jwks.keycloak.url";

    private static final List<String> RESOLVABLE_PROPERTIES = List.of(CLIENT_ID, CLIENT_SECRET, ISSUER, JWKS_URL);
    private static final Set<String> RESOLVABLE_PROPERTIES_SET = Set.copyOf(RESOLVABLE_PROPERTIES);

    private static final int KEYCLOAK_PORT = 8080;
    private static final String DEFAULT_REALM = "micronaut";
    private static final String DEFAULT_CLIENT_PREFIX = "micronaut-test-resources-";
    private static final String DEFAULT_ADMIN_USERNAME_PREFIX = "mn-test-resources-admin-";
    private static final String REALMS_PATH = "/realms/";

    private static final String REALM_CONFIGURATION_KEY = "containers.keycloak.realm";
    private static final String CLIENT_ID_CONFIGURATION_KEY = "containers.keycloak.client-id";
    private static final String CLIENT_SECRET_CONFIGURATION_KEY = "containers.keycloak.client-secret";

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
    @SuppressWarnings("java:S2095") // AbstractTestContainersProvider owns the container lifecycle after creation.
    protected KeycloakContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        KeycloakConfiguration keycloakConfiguration = buildConfiguration(testResourcesConfig);
        return new KeycloakContainer(imageName, keycloakConfiguration)
            .withExposedPorts(KEYCLOAK_PORT)
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", keycloakConfiguration.adminUsername())
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", keycloakConfiguration.adminPassword())
            .withCopyToContainer(
                Transferable.of(keycloakConfiguration.realmFileContents().getBytes(StandardCharsets.UTF_8)),
                "/opt/keycloak/data/import/" + keycloakConfiguration.realm() + "-realm.json"
            )
            .withCommand("start-dev", "--import-realm")
            .waitingFor(Wait.forHttp(REALMS_PATH + keycloakConfiguration.realm() + "/.well-known/openid-configuration")
                .forPort(KEYCLOAK_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(3)));
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, KeycloakContainer container) {
        KeycloakConfiguration keycloakConfiguration = container.configuration();
        return switch (propertyName) {
            case CLIENT_ID -> Optional.of(keycloakConfiguration.clientId());
            case CLIENT_SECRET -> Optional.of(keycloakConfiguration.clientSecret());
            case ISSUER -> Optional.of(baseUrl(container) + REALMS_PATH + keycloakConfiguration.realm());
            case JWKS_URL -> Optional.of(baseUrl(container) + REALMS_PATH + keycloakConfiguration.realm() + "/protocol/openid-connect/certs");
            default -> Optional.empty();
        };
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES_SET.contains(propertyName);
    }

    private static KeycloakConfiguration buildConfiguration(Map<String, Object> testResourcesConfig) {
        String realm = stringOrDefault(testResourcesConfig.get(REALM_CONFIGURATION_KEY), DEFAULT_REALM);
        String clientId = stringOrDefault(testResourcesConfig.get(CLIENT_ID_CONFIGURATION_KEY), DEFAULT_CLIENT_PREFIX + UUID.randomUUID());
        String clientSecret = stringOrDefault(testResourcesConfig.get(CLIENT_SECRET_CONFIGURATION_KEY), UUID.randomUUID().toString());
        String adminUsername = DEFAULT_ADMIN_USERNAME_PREFIX + UUID.randomUUID();
        String adminPassword = UUID.randomUUID().toString();
        return new KeycloakConfiguration(
            realm,
            clientId,
            clientSecret,
            adminUsername,
            adminPassword,
            renderRealmFile(realm, clientId, clientSecret)
        );
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static String renderRealmFile(String realm, String clientId, String clientSecret) {
        return """
            {
              "realm": "%s",
              "enabled": true,
              "clients": [
                {
                  "clientId": "%s",
                  "enabled": true,
                  "protocol": "openid-connect",
                  "publicClient": false,
                  "secret": "%s",
                  "serviceAccountsEnabled": true,
                  "standardFlowEnabled": false,
                  "implicitFlowEnabled": false,
                  "directAccessGrantsEnabled": false
                }
              ]
            }
            """.formatted(
            escapeJson(realm),
            escapeJson(clientId),
            escapeJson(clientSecret)
        );
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static String baseUrl(GenericContainer<?> container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(KEYCLOAK_PORT);
    }

    static final class KeycloakContainer extends GenericContainer<KeycloakContainer> {
        private final KeycloakConfiguration configuration;

        KeycloakContainer(DockerImageName imageName, KeycloakConfiguration configuration) {
            super(imageName);
            this.configuration = configuration;
        }

        KeycloakConfiguration configuration() {
            return configuration;
        }
    }

    private record KeycloakConfiguration(
        String realm,
        String clientId,
        String clientSecret,
        String adminUsername,
        String adminPassword,
        String realmFileContents
    ) {
    }
}
