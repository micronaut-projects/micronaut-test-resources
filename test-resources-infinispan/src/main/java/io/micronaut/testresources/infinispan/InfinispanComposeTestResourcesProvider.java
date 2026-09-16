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
package io.micronaut.testresources.infinispan;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves Infinispan properties from Docker Compose services.
 */
public final class InfinispanComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public InfinispanComposeTestResourcesProvider() {
        super("infinispan", Set.of(), 11222, List.of(
            "infinispan.client.hotrod.server.host",
            "infinispan.client.hotrod.server.port",
            "infinispan.client.hotrod.security.authentication.username",
            "infinispan.client.hotrod.security.authentication.password"
        ));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "infinispan.client.hotrod.server.host" -> context.host();
            case "infinispan.client.hotrod.server.port" -> String.valueOf(context.port());
            case "infinispan.client.hotrod.security.authentication.username" -> context.labelOrEnvironment(ComposeLabels.USERNAME, "USER", "admin");
            case "infinispan.client.hotrod.security.authentication.password" -> context.labelOrEnvironment(ComposeLabels.PASSWORD, "PASS", "password");
            default -> null;
        };
    }
}
