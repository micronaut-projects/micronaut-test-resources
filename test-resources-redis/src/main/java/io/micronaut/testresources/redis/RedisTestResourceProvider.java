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

import com.redis.testcontainers.RedisContainer;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.micronaut.testresources.redis.RedisConfigurationSupport.isClusterMode;

/**
 * A test resource provider which will spawn a Redis test container.
 */
public class RedisTestResourceProvider extends AbstractTestContainersProvider<RedisContainer> {

    public static final String REDIS_URI = "redis.uri";

    public static final String DEFAULT_IMAGE = RedisContainer.DEFAULT_IMAGE_NAME.asCanonicalNameString();
    public static final String SIMPLE_NAME = "redis";

    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(REDIS_URI);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(REDIS_URI);

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        boolean clusterMode = isClusterMode(testResourcesConfig);
        return clusterMode ? List.of() : SUPPORTED_PROPERTIES_LIST;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected RedisContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return new RedisContainer(imageName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, RedisContainer container) {
        if (REDIS_URI.equals(propertyName)) {
            return Optional.of(container.getRedisURI());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
