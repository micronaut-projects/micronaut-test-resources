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
package io.micronaut.testresources.infinispan;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Infinispan properties from Docker Compose services.
 */
public final class InfinispanComposeTestResourceProvider extends InfinispanTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor INFINISPAN =
        new ComposeResolverSupport.ServiceDescriptor("infinispan", List.of(), 11222);

    @Override
    public String getDisplayName() {
        return "Docker Compose Infinispan";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            INFINISPAN,
            context -> true,
            context -> switch (propertyName) {
                case HOST -> context.host();
                case PORT -> String.valueOf(context.publishedPort());
                case USERNAME -> context.labelOrEnvironment(ComposeResolverSupport.USERNAME_LABEL, "USER", "admin");
                case PASSWORD -> context.labelOrEnvironment(ComposeResolverSupport.PASSWORD_LABEL, "PASS", "password");
                default -> null;
            });
    }
}
