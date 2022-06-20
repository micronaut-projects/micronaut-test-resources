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

import io.micronaut.core.convert.DefaultConversionService;
import io.micronaut.runtime.converters.time.TimeConverterRegistrar;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Collection;
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

    private static final DefaultConversionService CONVERSION_SERVICE;
    private static final String CLASSPATH_PREFIX = "classpath:";

    static {
        CONVERSION_SERVICE = new DefaultConversionService();
        TimeConverterRegistrar registrar = new TimeConverterRegistrar();
        registrar.register(CONVERSION_SERVICE);
    }

    private TestContainerMetadataSupport() {

    }

    static Stream<TestContainerMetadata> containerMetadataFor(List<String> containerNames, Map<String, Object> testResourcesConfig) {
        return containerNames.stream()
            .map(name -> convertToMetadata(testResourcesConfig, name))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    static Optional<TestContainerMetadata> convertToMetadata(Map<String, Object> testResourcesConfig, String name) {
        String prefix = TEST_RESOURCES_CONTAINERS + name + ".";
        String imageName = extractStringParameterFrom(prefix, "image-name", testResourcesConfig);
        String imageTag = extractStringParameterFrom(prefix, "image-tag", testResourcesConfig);
        Map<String, Integer> exposedPorts = extractExposedPortsFrom(prefix, testResourcesConfig);
        Set<String> hostNames = extractHostsFrom(prefix, testResourcesConfig);
        Map<String, String> rwFsBinds = extractFsBindsFrom(prefix, testResourcesConfig, false);
        Map<String, String> roFsBinds = extractFsBindsFrom(prefix, testResourcesConfig, true);
        Map<String, String> env = extractMapFrom(prefix, "env", testResourcesConfig);
        Map<String, String> labels = extractMapFrom(prefix, "labels", testResourcesConfig);
        String command = extractStringParameterFrom(prefix, "command", testResourcesConfig);
        String workingDirectory = extractStringParameterFrom(prefix, "working-directory", testResourcesConfig);
        Duration startupTimeout = CONVERSION_SERVICE.convert(extractStringParameterFrom(prefix, "startup-timeout", testResourcesConfig), Duration.class).orElse(null);
        List<TestContainerMetadata.CopyFileToContainer> fileCopies = extractFileCopiesFrom(prefix, testResourcesConfig);
        Long memory = extractMemoryParameterFrom(prefix, testResourcesConfig, "memory");
        Long swapMemory = extractMemoryParameterFrom(prefix, testResourcesConfig, "swap-memory");
        Long sharedMemory = extractMemoryParameterFrom(prefix, testResourcesConfig, "shared-memory");
        return Optional.of(new TestContainerMetadata(name, imageName, imageTag, exposedPorts, hostNames, rwFsBinds, roFsBinds, command, workingDirectory, env, labels, startupTimeout, fileCopies, memory, swapMemory, sharedMemory));
    }

    private static Long extractMemoryParameterFrom(String prefix, Map<String, Object> testResourcesConfig, String key) {
        String asString = extractStringParameterFrom(prefix, key, testResourcesConfig);
        if (asString != null) {
            return MemoryUnitParser.parse(asString);
        }
        return null;
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

    private static String extractStringParameterFrom(String prefix, String key, Map<String, Object> testResourcesConfiguration) {
        return Optional.ofNullable(testResourcesConfiguration.get(prefix + key))
            .map(String::valueOf)
            .orElse(null);
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
        return extractMapFrom(prefix, (readOnly ? "ro-" : "rw-") + "fs-bind", testResourcesConfiguration);
    }

    private static List<TestContainerMetadata.CopyFileToContainer> extractFileCopiesFrom(String prefix,
        Map<String, Object> testResourcesConfiguration) {
        Map<String, String> copyDefinitions = extractMapFrom(prefix, "copy-to-container", testResourcesConfiguration);
        return copyDefinitions.entrySet()
            .stream()
            .map(e -> {
                String source = e.getKey();
                String destination = e.getValue();
                if (source.startsWith(CLASSPATH_PREFIX)) {
                    String classpath = source.substring(CLASSPATH_PREFIX.length());
                    return new TestContainerMetadata.CopyFileToContainer(MountableFile.forClasspathResource(classpath), destination);
                }
                return new TestContainerMetadata.CopyFileToContainer(MountableFile.forHostPath(source), destination);
            })
            .collect(Collectors.toList());
    }

    private static Map<String, String> extractMapFrom(String prefix,
                                                      String key,
                                                      Map<String, Object> testResourcesConfiguration) {
        class StringEntry {
            final String key;
            final String value;

            StringEntry(Object key, Object value) {
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
        return Optional.ofNullable(testResourcesConfiguration.get(prefix + key))
            .map(o -> {
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    return list.stream()
                        .flatMap(definition -> {
                            if (definition instanceof Map) {
                                return ((Map<?, ?>) definition).entrySet()
                                    .stream()
                                    .map(e -> new StringEntry(e.getKey(), e.getValue()));
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toMap(StringEntry::getKey, StringEntry::getValue));
                }
                return Collections.<String, String>emptyMap();
            })
            .orElse(Collections.emptyMap());
    }

    static GenericContainer<?> applyMetadata(TestContainerMetadata md, GenericContainer<?> container) {
        Collection<Integer> exposedPorts = md.getExposedPorts().values();
        if (!exposedPorts.isEmpty()) {
            container.withExposedPorts(exposedPorts.toArray(new Integer[0]));
        }
        md.getRwFsBinds().forEach(container::withFileSystemBind);
        md.getRoFsBinds().forEach((hostPath, containerPath) -> container.withFileSystemBind(hostPath, containerPath, BindMode.READ_ONLY));
        md.getCommand().ifPresent(container::withCommand);
        container.withEnv(md.getEnv());
        container.withLabels(md.getLabels());
        md.getStartupTimeout().ifPresent(container::withStartupTimeout);
        md.getFileCopies().forEach(copy -> container.withCopyFileToContainer(copy.getFile(), copy.getDestination()));
        md.getSharedMemory().ifPresent(container::withSharedMemorySize);
        md.getMemory().ifPresent(memory -> container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(memory)));
        md.getSwapMemory().ifPresent(memory -> container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemorySwap(memory)));
        return container;
    }
}
