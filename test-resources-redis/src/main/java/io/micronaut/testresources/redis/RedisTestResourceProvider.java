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

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A test resource provider which will spawn a Redis test container.
 */
public class RedisTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {

    public static final String REDIS_URI = "redis.uri";
    public static final String DEFAULT_IMAGE = "redis";
    public static final String SIMPLE_NAME = "redis";
    public static final int REDIS_PORT = 6379;

    private static final Set<String> SUPPORTED_PROPERTIES;

    static {
        Set<String> supported = new HashSet<>();
        supported.add(REDIS_URI);
        SUPPORTED_PROPERTIES = Collections.unmodifiableSet(supported);
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Collections.singletonList(REDIS_URI);
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
    protected GenericContainer<?> createContainer(DockerImageName imageName, Map<String, Object> properties) {
        GenericContainer<?> container = new GenericContainer<>(imageName);
        container.withExposedPorts(REDIS_PORT);
        return container;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        if (REDIS_URI.equals(propertyName)) {
            return Optional.of("redis://" + container.getHost() + ":" + container.getMappedPort(REDIS_PORT));
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
