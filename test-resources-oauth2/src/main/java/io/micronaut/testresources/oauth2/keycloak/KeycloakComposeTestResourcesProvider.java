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

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves Keycloak OAuth2 properties from Docker Compose services.
 */
public final class KeycloakComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public KeycloakComposeTestResourcesProvider() {
        super("keycloak", Set.of(), 8080, List.of(
            "micronaut.security.oauth2.clients.keycloak.client-id",
            "micronaut.security.oauth2.clients.keycloak.client-secret",
            "micronaut.security.oauth2.clients.keycloak.openid.issuer",
            "micronaut.security.token.jwt.signatures.jwks.keycloak.url"
        ));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "micronaut.security.oauth2.clients.keycloak.client-id" -> context.labelOrEnvironment(ComposeLabels.CLIENT_ID, "KEYCLOAK_CLIENT_ID", "micronaut-test-resources");
            case "micronaut.security.oauth2.clients.keycloak.client-secret" -> context.labelOrEnvironment(ComposeLabels.CLIENT_SECRET, "KEYCLOAK_CLIENT_SECRET", "secret");
            case "micronaut.security.oauth2.clients.keycloak.openid.issuer" -> issuer(context);
            case "micronaut.security.token.jwt.signatures.jwks.keycloak.url" -> issuer(context) + "/protocol/openid-connect/certs";
            default -> null;
        };
    }

    private static String issuer(ComposeTestResourcesProvider.ResolutionContext context) {
        return context.http() + "/realms/" + context.labelOrEnvironment(ComposeLabels.REALM, "KEYCLOAK_REALM", "micronaut");
    }
}
