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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String TEST_RESOURCES_CONTAINERS = "containers.";

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
    private Stream<GenericContainerMetadata> containerMetadataFrom(Map<String, Object> testResourcesConfig) {
        List<String> genericContainers = containerNamesFrom(testResourcesConfig);

        return genericContainers.stream()
            .map(name -> {
                String prefix = TEST_RESOURCES_CONTAINERS + name + ".";
                String containerName = (String) testResourcesConfig.get(prefix + "image-name");
                if (containerName != null) {
                    Map<String, Integer> exposedPorts = extractExposedPortsFrom(prefix, testResourcesConfig);
                    Set<String> hostNames = extractHostsFrom(prefix, testResourcesConfig);
                    return Optional.of(new GenericContainerMetadata(name, containerName, exposedPorts, hostNames));
                }
                return Optional.<GenericContainerMetadata>empty();
            })
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return TestResourcesResolver.super.getRequiredProperties(expression);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        class MappedContainer {
            private final GenericContainerMetadata md;
            private final GenericContainer<?> container;

            MappedContainer(GenericContainerMetadata md, GenericContainer<?> container) {
                this.md = md;
                this.container = container;
            }
        }
        return containerMetadataFrom(testResourcesConfiguration)
            .filter(e -> e.getExposedPorts().containsKey(propertyName) || e.getHostNames().contains(propertyName))
            .findFirst()
            .map(md -> {
                DockerImageName imageName = DockerImageName.parse(md.getImageName());
                return new MappedContainer(md, TestContainers.getOrCreate(GenericTestContainerProvider.class,
                    md.getId(),
                    properties,
                    () -> {
                        GenericContainer<?> selfGenericContainer = new GenericContainer<>(imageName);
                        Collection<Integer> exposedPorts = md.exposedPorts.values();
                        selfGenericContainer.withExposedPorts(exposedPorts.toArray(new Integer[0]));
                        return selfGenericContainer;
                    }
                ));
            }).map(e -> {
                Integer mappedPort = e.md.exposedPorts.get(propertyName);
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
            .filter(key -> key.startsWith(TEST_RESOURCES_CONTAINERS))
            .map(key -> key.substring(TEST_RESOURCES_CONTAINERS.length()))
            .map(key -> key.substring(0, key.indexOf('.')))
            .distinct()
            .collect(Collectors.toList());
    }

    private static Map<String, Integer> extractExposedPortsFrom(String prefix, Map<String, Object> testResourcesConfiguration) {
        class MappedPort {
            final String key;
            final int value;

            MappedPort(Object key, Object value) {
                this.key = String.valueOf(key);
                this.value = Integer.parseInt(String.valueOf(value));
            }

            public String getKey() {
                return key;
            }

            public int getValue() {
                return value;
            }
        }
        return Optional.ofNullable(testResourcesConfiguration.get(prefix + "exposed-ports"))
            .map(o -> {
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    return list.stream()
                        .flatMap(definition -> {
                            if (definition instanceof Map) {
                                return ((Map<?, ?>) definition).entrySet()
                                    .stream()
                                    .map(e -> new MappedPort(e.getKey(), e.getValue()));
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toMap(MappedPort::getKey, MappedPort::getValue));
                }
                return Collections.<String, Integer>emptyMap();
            })
            .orElse(Collections.emptyMap());
    }

    private static Set<String> extractHostsFrom(String prefix, Map<String, Object> testResourcesConfiguration) {
        return Optional.ofNullable(testResourcesConfiguration.get(prefix + "hostnames"))
            .map(o -> {
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    return list.stream().map(String::valueOf).collect(Collectors.toSet());
                }
                return Collections.singleton(String.valueOf(o));
            })
            .orElse(Collections.emptySet());
    }

    private static final class GenericContainerMetadata {
        private final String id;
        private final String imageName;
        private final Map<String, Integer> exposedPorts;
        private final Set<String> hostNames;

        private GenericContainerMetadata(String id,
                                         String imageName,
                                         Map<String, Integer> exposedPorts,
                                         Set<String> hostNames) {
            this.id = id;
            this.imageName = imageName;
            this.exposedPorts = exposedPorts;
            this.hostNames = hostNames;
        }

        public String getId() {
            return id;
        }

        public String getImageName() {
            return imageName;
        }

        public Map<String, Integer> getExposedPorts() {
            return exposedPorts;
        }

        public Set<String> getHostNames() {
            return hostNames;
        }
    }
}
