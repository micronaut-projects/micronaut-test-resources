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
package io.micronaut.testresources.seaweedfs;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves SeaweedFS properties from Docker Compose services.
 */
public final class SeaweedFsComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public SeaweedFsComposeTestResourcesProvider() {
        super("seaweedfs", Set.of(), 8333, List.of("seaweedfs.url", "seaweedfs.access-key", "seaweedfs.secret-key"));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "seaweedfs.url" -> context.http();
            case "seaweedfs.access-key" -> context.labelOrEnvironment(ComposeLabels.ACCESS_KEY, "SEAWEEDFS_ACCESS_KEY", "some_access_key1");
            case "seaweedfs.secret-key" -> context.labelOrEnvironment(ComposeLabels.SECRET_KEY, "SEAWEEDFS_SECRET_KEY", "some_secret_key1");
            default -> null;
        };
    }
}
