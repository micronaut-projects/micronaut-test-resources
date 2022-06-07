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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility methods to deal with test containers metadata.
 */
final class TestContainerMetadataSupport {
    static final String TEST_RESOURCES_CONTAINERS = "containers.";

    static final int GENERIC_ORDER = 1000;
    static final int SPECIFIC_ORDER = 0;

    private TestContainerMetadataSupport() {

    }

    static Stream<TestContainerMetadata> containerMetadataFor(List<String> containerNames, Map<String, Object> testResourcesConfig) {
        return containerNames.stream()
            .map(name -> {
                String prefix = TEST_RESOURCES_CONTAINERS + name + ".";
                String imageName = (String) testResourcesConfig.get(prefix + "image-name");
                if (imageName != null) {
                    Map<String, Integer> exposedPorts = extractExposedPortsFrom(prefix, testResourcesConfig);
                    Set<String> hostNames = extractHostsFrom(prefix, testResourcesConfig);
                    Map<String, String> rwFsBinds = extractFsBindsFrom(prefix, testResourcesConfig, false);
                    Map<String, String> roFsBinds = extractFsBindsFrom(prefix, testResourcesConfig, true);
                    return Optional.of(new TestContainerMetadata(name, imageName, exposedPorts, hostNames, rwFsBinds, roFsBinds));
                }
                return Optional.<TestContainerMetadata>empty();
            })
            .filter(Optional::isPresent)
            .map(Optional::get);
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

    private static Map<String, String> extractFsBindsFrom(String prefix,
                                                          Map<String, Object> testResourcesConfiguration,
                                                          boolean readOnly) {
        class FsBind {
            final String key;
            final String value;

            FsBind(Object key, Object value) {
                this.key = String.valueOf(key);
                this.value = String.valueOf(value);
            }

            public String getKey() {
                return key;
            }

            public String getValue() {
                return value;
            }
        }
        String key = prefix + (readOnly ? "ro-" : "rw-") + "fs-bind";
        return Optional.ofNullable(testResourcesConfiguration.get(key))
            .map(o -> {
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    return list.stream()
                        .flatMap(definition -> {
                            if (definition instanceof Map) {
                                return ((Map<?, ?>) definition).entrySet()
                                    .stream()
                                    .map(e -> new FsBind(e.getKey(), e.getValue()));
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toMap(FsBind::getKey, FsBind::getValue));
                }
                return Collections.<String, String>emptyMap();
            })
            .orElse(Collections.emptyMap());
    }
}
