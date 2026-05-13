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

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Keycloak properties from Docker Compose services.
 */
public final class KeycloakComposeTestResourceProvider extends KeycloakTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor KEYCLOAK =
        new ComposeResolverSupport.ServiceDescriptor("keycloak", List.of(), 8080);

    @Override
    public String getDisplayName() {
        return "Docker Compose Keycloak";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            KEYCLOAK,
            context -> true,
            context -> switch (propertyName) {
                case CLIENT_ID -> context.labelOrEnvironment("io.micronaut.test-resources.client-id", "KEYCLOAK_CLIENT_ID", "micronaut-test-resources");
                case CLIENT_SECRET -> context.labelOrEnvironment(ComposeResolverSupport.PASSWORD_LABEL, "KEYCLOAK_CLIENT_SECRET", "secret");
                case ISSUER -> issuer(context);
                case JWKS_URL -> issuer(context) + "/protocol/openid-connect/certs";
                default -> null;
            });
    }

    private static String issuer(ComposeResolverSupport.ResolutionContext context) {
        String realm = context.labelOrEnvironment("io.micronaut.test-resources.realm", "KEYCLOAK_REALM", "micronaut");
        return context.http() + "/realms/" + realm;
    }
}
