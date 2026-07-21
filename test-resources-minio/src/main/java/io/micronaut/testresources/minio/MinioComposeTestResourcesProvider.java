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
package io.micronaut.testresources.minio;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves MinIO properties from Docker Compose services.
 */
public final class MinioComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public MinioComposeTestResourcesProvider() {
        super("minio", Set.of(), 9000, List.of("minio.url", "minio.access-key", "minio.secret-key"));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "minio.url" -> context.http();
            case "minio.access-key" -> context.labelOrEnvironment(ComposeLabels.ACCESS_KEY, "MINIO_ROOT_USER", "minioadmin");
            case "minio.secret-key" -> context.labelOrEnvironment(ComposeLabels.SECRET_KEY, "MINIO_ROOT_PASSWORD", "minioadmin");
            default -> null;
        };
    }
}
