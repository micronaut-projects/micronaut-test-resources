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
package io.micronaut.testresources.azure;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves Azurite properties from Docker Compose services.
 */
public final class AzuriteComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    private static final String ACCOUNT_NAME = "azure.credential.storage-shared-key.account-name";
    private static final String ACCOUNT_KEY = "azure.credential.storage-shared-key.account-key";
    private static final String CONNECTION_STRING = "azure.credential.storage-shared-key.connection-string";
    private static final String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
    private static final String DEFAULT_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    public AzuriteComposeTestResourcesProvider() {
        super("azurite", Set.of("azure-storage"), 10000, List.of(ACCOUNT_NAME, ACCOUNT_KEY, CONNECTION_STRING));
    }

    @Override
    public List<Integer> getPorts() {
        return List.of(10000, 10001, 10002);
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case ACCOUNT_NAME -> DEFAULT_ACCOUNT_NAME;
            case ACCOUNT_KEY -> DEFAULT_ACCOUNT_KEY;
            case CONNECTION_STRING -> "DefaultEndpointsProtocol=http;"
                + "AccountName=" + DEFAULT_ACCOUNT_NAME + ";"
                + "AccountKey=" + DEFAULT_ACCOUNT_KEY + ";"
                + "BlobEndpoint=" + endpoint(context, 10000) + ";"
                + "QueueEndpoint=" + endpoint(context, 10001) + ";"
                + "TableEndpoint=" + endpoint(context, 10002) + ";";
            default -> null;
        };
    }

    private static String endpoint(ComposeTestResourcesProvider.ResolutionContext context, int port) {
        return context.http(port).map(value -> value + "/" + DEFAULT_ACCOUNT_NAME).orElse("");
    }
}
