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

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Azurite properties from Docker Compose services.
 */
public final class AzuriteComposeTestResourceProvider extends AzuriteTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor AZURITE =
        new ComposeResolverSupport.ServiceDescriptor("azurite", List.of("azure-storage"), BLOB_PORT);

    @Override
    public String getDisplayName() {
        return "Docker Compose Azurite";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            AZURITE,
            context -> true,
            context -> switch (propertyName) {
                case ACCOUNT_NAME_PROPERTY -> DEFAULT_ACCOUNT_NAME;
                case ACCOUNT_KEY_PROPERTY -> DEFAULT_ACCOUNT_KEY;
                case CONNECTION_STRING_PROPERTY -> connectionString(context);
                default -> null;
            });
    }

    private static String connectionString(ComposeResolverSupport.ResolutionContext context) {
        return "DefaultEndpointsProtocol=http;"
            + "AccountName=" + DEFAULT_ACCOUNT_NAME + ";"
            + "AccountKey=" + DEFAULT_ACCOUNT_KEY + ";"
            + "BlobEndpoint=" + endpoint(context, BLOB_PORT) + ";"
            + "QueueEndpoint=" + endpoint(context, QUEUE_PORT) + ";"
            + "TableEndpoint=" + endpoint(context, TABLE_PORT) + ";";
    }

    private static String endpoint(ComposeResolverSupport.ResolutionContext context, int targetPort) {
        return context.httpEndpoint(targetPort)
            .map(url -> url + "/" + DEFAULT_ACCOUNT_NAME)
            .orElse("");
    }
}
