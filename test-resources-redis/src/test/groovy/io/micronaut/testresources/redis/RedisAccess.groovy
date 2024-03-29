/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands
import io.micronaut.context.annotation.Requires
import jakarta.inject.Inject

import java.util.function.Function

@jakarta.inject.Singleton
@Requires(notEnv = "cluster")
class RedisAccess {
    @Inject
    RedisClient redisClient

    public <T> T withClient(Function<? super RedisCommands<String, String>, T> transformer) {
        def conn = redisClient.connect().sync()
        return transformer.apply(conn)
    }
}
