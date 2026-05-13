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
package io.micronaut.testresources.redis;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves Redis properties from Docker Compose services.
 */
public final class RedisComposeTestResourceProvider extends RedisTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final Logger LOG = LoggerFactory.getLogger(RedisComposeTestResourceProvider.class);
    private static final ComposeResolverSupport.ServiceDescriptor REDIS =
        new ComposeResolverSupport.ServiceDescriptor("redis", List.of(), 6379);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(REDIS_URI, RedisClusterTestResourceProvider.REDIS_URIS);

    @Override
    public String getDisplayName() {
        return "Docker Compose Redis";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            REDIS,
            context -> true,
            context -> {
                if (context.hasEnvironment("REDIS_PASSWORD")) {
                    LOG.warn("Docker Compose Redis service declares REDIS_PASSWORD. Password-protected Redis Compose mapping is not inferred; add explicit application configuration or use the default provider.");
                    return null;
                }
                return "redis://" + context.hostPort();
            });
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
