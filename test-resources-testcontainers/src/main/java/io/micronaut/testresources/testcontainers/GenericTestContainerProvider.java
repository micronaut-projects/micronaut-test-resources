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
package io.micronaut.testresources.testcontainers;

import io.micronaut.testresources.core.TestResourcesResolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micronaut.testresources.testcontainers.TestContainerMetadataSupport.GENERIC_ORDER;
import static io.micronaut.testresources.testcontainers.TestContainerMetadataSupport.containerMetadataFor;

/**
 * A generic test containers provider. This provider is special in the sense
 * that it requires user provided configuration in order to spawn containers.
 * Therefore, the application configuration must contain entries under the
 * "test-resources.containers" property prefix.
 *
 * A generic container is represented with a name, a container image, and
 * exposes a number of ports.
 *
 * For example, imagine that a service requires an SMTP server to work
 * property. Then it is likely that some configuration property needs to
 * point to the server port. If that property is <code>smtp.port</code>, then it is
 * possible to expose a test container which will resolve that property using
 * the following configuration:
 *
 * <pre>
 * test-resources:
 *   containers:
 *     fakesmtp:
 *       image-name: ghusta/fakesmtp:2.0
 *       hostnames:
 *         - smtp.host
 *       exposed-ports:
 *         - smtp.port: 25
 * </pre>
 *
 * The mapped port from test containers will then be automatically be assigned
 * to the <code>smtp.port</code> property, and the host name of the running
 * test container will automatically be assigned to the <code>smtp.host</code>
 * property.
 */
@SuppressWarnings("unchecked")
public class GenericTestContainerProvider implements TestResourcesResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericTestContainerProvider.class);

    @Override
    public int getOrder() {
        return GENERIC_ORDER;
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        List<String> resolvable = containerMetadataFrom(testResourcesConfig)
            .flatMap(e -> Stream.concat(e.getExposedPorts().keySet().stream(), e.getHostNames().stream()))
            .distinct()
            .collect(Collectors.toList());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Properties which can be resolved by generic containers: {}", resolvable);
        }
        return resolvable;
    }

    @NotNull
    private Stream<TestContainerMetadata> containerMetadataFrom(Map<String, Object> testResourcesConfig) {
        List<String> genericContainers = containerNamesFrom(testResourcesConfig);
        return containerMetadataFor(genericContainers, testResourcesConfig);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        class MappedContainer {
            private final TestContainerMetadata md;
            private final GenericContainer<?> container;

            MappedContainer(TestContainerMetadata md, GenericContainer<?> container) {
                this.md = md;
                this.container = container;
            }
        }
        return containerMetadataFrom(testResourcesConfig)
            .filter(e -> e.getExposedPorts().containsKey(propertyName) || e.getHostNames().contains(propertyName))
            .filter(md -> md.getImageName().isPresent())
            .findFirst()
            .map(md -> {
                DockerImageName imageName = DockerImageName.parse(md.getImageName().get());
                return new MappedContainer(md, TestContainers.getOrCreate(propertyName, GenericTestContainerProvider.class,
                    md.getId(),
                    properties,
                    () -> {
                        GenericContainer<?> selfGenericContainer = new GenericContainer<>(imageName);
                        return TestContainerMetadataSupport.applyMetadata(md, selfGenericContainer);
                    }
                ));
            }).map(e -> {
                Integer mappedPort = e.md.getExposedPorts().get(propertyName);
                if (mappedPort != null) {
                    return String.valueOf(e.container.getMappedPort(mappedPort));
                }
                if (e.md.getHostNames().contains(propertyName)) {
                    return e.container.getHost();
                }
                return null;
            });
    }

    private static List<String> containerNamesFrom(Map<String, Object> configuration) {
        return configuration.keySet()
            .stream()
            .filter(key -> key.startsWith(TestContainerMetadataSupport.TEST_RESOURCES_CONTAINERS))
            .map(key -> key.substring(TestContainerMetadataSupport.TEST_RESOURCES_CONTAINERS.length()))
            .map(key -> key.substring(0, key.indexOf('.')))
            .distinct()
            .collect(Collectors.toList());
    }

}
