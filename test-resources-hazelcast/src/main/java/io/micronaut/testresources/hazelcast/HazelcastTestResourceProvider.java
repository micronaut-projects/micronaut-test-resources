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
package io.micronaut.testresources.hazelcast;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class HazelcastTestResourceProvider extends AbstractTestContainersProvider<GenericContainer<?>> {

    public static final String HAZELCAST_ADDRESSES = "hazelcast.client.network.addresses";
    public static final String DEFAULT_IMAGE = "hazelcast/hazelcast:latest-slim";
    public static final String DISPLAY_NAME = "Hazelcast";
    public static final String SIMPLE_NAME = "hazelcast";
    private static final int DEFAULT_PORT = 5701;

    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(HAZELCAST_ADDRESSES);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(HAZELCAST_ADDRESSES);

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES_LIST;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
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
    protected GenericContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new GenericContainer<>(imageName)
            .withExposedPorts(DEFAULT_PORT);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, GenericContainer<?> container) {
        if (HAZELCAST_ADDRESSES.equals(propertyName)) {
            String address = "%s:%d".formatted(container.getHost(), container.getMappedPort(DEFAULT_PORT));
            return Optional.of(address);
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
