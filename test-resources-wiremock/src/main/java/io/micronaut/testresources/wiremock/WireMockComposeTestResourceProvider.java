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
package io.micronaut.testresources.wiremock;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves WireMock properties from Docker Compose services.
 */
public final class WireMockComposeTestResourceProvider extends WireMockTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor WIREMOCK =
        new ComposeResolverSupport.ServiceDescriptor("wiremock", List.of(), 8080);

    @Override
    public String getDisplayName() {
        return "Docker Compose WireMock";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            WIREMOCK,
            context -> true,
            context -> switch (propertyName) {
                case WIREMOCK_HOST -> context.host();
                case WIREMOCK_PORT -> String.valueOf(context.publishedPort());
                case WIREMOCK_URL -> context.http();
                default -> null;
            });
    }
}
