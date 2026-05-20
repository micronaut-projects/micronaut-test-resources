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

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeLabels;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Resolves Redis properties from Docker Compose services.
 */
public final class RedisComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    private static final Logger LOG = LoggerFactory.getLogger(RedisComposeTestResourcesProvider.class);

    public RedisComposeTestResourcesProvider() {
        super("redis", Set.of("redis"), 6379, List.of("redis.uri"));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        if (context.service().labels().containsKey(ComposeLabels.PASSWORD) || context.service().environment().containsKey("REDIS_PASSWORD")) {
            LOG.warn("Ignoring Docker Compose Redis service {} because authenticated Redis URI mapping is not supported", context.service().name());
            return null;
        }
        return "redis://" + context.hostPort();
    }
}
