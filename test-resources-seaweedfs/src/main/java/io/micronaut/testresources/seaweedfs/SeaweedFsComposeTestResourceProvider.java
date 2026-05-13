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
package io.micronaut.testresources.seaweedfs;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves SeaweedFS properties from Docker Compose services.
 */
public final class SeaweedFsComposeTestResourceProvider extends SeaweedFsTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor SEAWEEDFS =
        new ComposeResolverSupport.ServiceDescriptor("seaweedfs", List.of(), S3_PORT);

    @Override
    public String getDisplayName() {
        return "Docker Compose SeaweedFS";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            SEAWEEDFS,
            context -> true,
            context -> switch (propertyName) {
                case SEAWEEDFS_URL -> context.http();
                case SEAWEEDFS_ACCESS_KEY -> DEFAULT_ACCESS_KEY;
                case SEAWEEDFS_SECRET_KEY -> DEFAULT_SECRET_KEY;
                default -> null;
            });
    }
}
