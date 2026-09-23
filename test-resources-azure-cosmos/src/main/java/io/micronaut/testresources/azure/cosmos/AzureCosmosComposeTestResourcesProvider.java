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

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves Azure Cosmos emulator properties from Docker Compose services.
 */
public final class AzureCosmosComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    private static final String PROTOCOL_ENVIRONMENT_VARIABLE = "PROTOCOL";
    private static final String DEFAULT_PROTOCOL = "https";

    /**
     * The well-known Azure Cosmos emulator key. It is a fixed, publicly documented development
     * value, not a secret, and it is the only key the emulator accepts unless the Compose service
     * overrides it explicitly.
     */
    private static final String DEFAULT_EMULATOR_KEY =
        "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";

    public AzureCosmosComposeTestResourcesProvider() {
        super(
            AzureCosmosTestResourceProvider.SIMPLE_NAME,
            Set.of("cosmos", "cosmosdb", "azure-cosmos-emulator"),
            AzureCosmosTestResourceProvider.COSMOS_PORT,
            List.of(AzureCosmosTestResourceProvider.ENDPOINT, AzureCosmosTestResourceProvider.KEY)
        );
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        if (AzureCosmosTestResourceProvider.ENDPOINT.equals(context.propertyName())) {
            return protocol(context) + "://" + context.hostPort();
        }
        if (AzureCosmosTestResourceProvider.KEY.equals(context.propertyName())) {
            return context.labelOrEnvironment(ComposeLabels.PASSWORD, "AZURE_COSMOS_KEY", DEFAULT_EMULATOR_KEY);
        }
        return null;
    }

    private static String protocol(ComposeTestResourcesProvider.ResolutionContext context) {
        return context.environment(PROTOCOL_ENVIRONMENT_VARIABLE)
            .filter(value -> !value.isBlank())
            .orElse(DEFAULT_PROTOCOL);
    }
}
