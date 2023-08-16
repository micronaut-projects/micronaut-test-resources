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

import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.DefaultMutableConversionService;
import io.micronaut.core.convert.MutableConversionService;
import io.micronaut.runtime.converters.time.TimeConverterRegistrar;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    private static final ConversionService CONVERSION_SERVICE;
    private static final String CLASSPATH_PREFIX = "classpath:";

    static {
        MutableConversionService mcs = new DefaultMutableConversionService();
        TimeConverterRegistrar registrar = new TimeConverterRegistrar();
        registrar.register(mcs);
        CONVERSION_SERVICE = mcs;
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
        Set<String> rwTmpfsMappings = extractTmpfsMappingsFrom(prefix, testResourcesConfig, false);
        Set<String> roTmpfsMappings = extractTmpfsMappingsFrom(prefix, testResourcesConfig, true);
        Map<String, String> env = extractMapFrom(prefix, "env", testResourcesConfig);
        Map<String, String> labels = extractMapFrom(prefix, "labels", testResourcesConfig);
        List<String> command = extractListFrom(prefix, testResourcesConfig, "command");
        String workingDirectory = extractStringParameterFrom(prefix, "working-directory", testResourcesConfig);
        Duration startupTimeout = CONVERSION_SERVICE.convert(extractStringParameterFrom(prefix, "startup-timeout", testResourcesConfig), Duration.class).orElse(null);
        List<TestContainerMetadata.CopyFileToContainer> fileCopies = extractFileCopiesFrom(prefix, testResourcesConfig);
        Long memory = extractMemoryParameterFrom(prefix, testResourcesConfig, "memory");
        Long swapMemory = extractMemoryParameterFrom(prefix, testResourcesConfig, "swap-memory");
        Long sharedMemory = extractMemoryParameterFrom(prefix, testResourcesConfig, "shared-memory");
        String network = extractStringParameterFrom(prefix, "network", testResourcesConfig);
        Set<String> networkAliases = extractSetFrom(prefix, testResourcesConfig, "network-aliases");
        String networkMode = extractStringParameterFrom(prefix, "networkMode", testResourcesConfig);
        Set<String> dependsOn = extractSetFrom(prefix, testResourcesConfig, "depends-on");
        WaitStrategy waitStrategy = extractWaitStrategyFrom(prefix, testResourcesConfig);
        return Optional.of(new TestContainerMetadata(name, imageName, imageTag, exposedPorts, hostNames, rwFsBinds, roFsBinds, rwTmpfsMappings, roTmpfsMappings, command, workingDirectory, env, labels, startupTimeout, fileCopies, memory, swapMemory, sharedMemory, network, networkAliases, networkMode, waitStrategy, dependsOn));
    }

    private static Long extractMemoryParameterFrom(String prefix, Map<String, Object> testResourcesConfig, String key) {
        String asString = extractStringParameterFrom(prefix, key, testResourcesConfig);
        if (asString != null) {
            return MemoryUnitParser.parse(asString);
        }
        return null;
    }

    private static Map<String, Integer> extractExposedPortsFrom(String prefix, Map<String, Object> testResourcesConfig) {
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
        return Optional.ofNullable(testResourcesConfig.get(prefix + "exposed-ports"))
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

    private static String extractStringParameterFrom(String prefix, String key, Map<String, Object> testResourcesConfig) {
        return Optional.ofNullable(testResourcesConfig.get(prefix + key))
            .map(String::valueOf)
            .orElse(null);
    }

    private static Integer extractIntParameterFrom(String prefix, String key, Map<String, Object> testResourcesConfig) {
        return Optional.ofNullable(testResourcesConfig.get(prefix + key))
            .map(Integer.class::cast)
            .orElse(null);
    }

    private static Set<String> extractSetFrom(String prefix, Map<String, Object> testResourcesConfig, String key) {
        return Optional.ofNullable(testResourcesConfig.get(prefix + key))
            .map(o -> {
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    return list.stream().map(String::valueOf).collect(Collectors.toSet());
                }
                return Collections.singleton(String.valueOf(o));
            })
            .orElse(Collections.emptySet());
    }

    private static List<String> extractListFrom(String prefix, Map<String, Object> testResourcesConfig, String key) {
        return Optional.ofNullable(testResourcesConfig.get(prefix + key))
            .map(o -> {
                if (o instanceof List) {
                    List<Object> list = (List<Object>) o;
                    return list.stream().map(String::valueOf).collect(Collectors.toList());
                }
                return Collections.singletonList(String.valueOf(o));
            })
            .orElse(Collections.emptyList());
    }

    private static Set<String> extractHostsFrom(String prefix, Map<String, Object> testResourcesConfig) {
        return extractSetFrom(prefix, testResourcesConfig, "hostnames");
    }

    private static Set<String> extractTmpfsMappingsFrom(String prefix, Map<String, Object> testResourcesConfig, boolean readOnly) {
        return extractSetFrom(prefix, testResourcesConfig, (readOnly ? "ro-" : "rw-") + "tmpfs-mappings");
    }

    private static Map<String, String> extractFsBindsFrom(String prefix,
                                                          Map<String, Object> testResourcesConfig,
                                                          boolean readOnly) {
        return extractMapFrom(prefix, (readOnly ? "ro-" : "rw-") + "fs-bind", testResourcesConfig);
    }

    private static List<TestContainerMetadata.CopyFileToContainer> extractFileCopiesFrom(String prefix,
                                                                                         Map<String, Object> testResourcesConfig) {
        Map<String, String> copyDefinitions = extractMapFrom(prefix, "copy-to-container", testResourcesConfig);
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
                                                      Map<String, Object> testResourcesConfig) {
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
        return Optional.ofNullable(testResourcesConfig.get(prefix + key))
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

    private static WaitStrategy extractWaitStrategyFrom(String prefix, Map<String, Object> testResourcesConfig) {
        String waitStrategyPrefix = prefix + "wait-strategy.";
        List<WaitStrategy> strategies = new ArrayList<>();
        Set<String> strategyIds = testResourcesConfig.keySet()
            .stream()
            .map(k -> determineWaitStrategyIdFor(prefix, testResourcesConfig, waitStrategyPrefix, k))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String strategyId : strategyIds) {
            switch (strategyId) {
                case "log":
                    strategies.add(parseLogStrategy(waitStrategyPrefix + "log.", testResourcesConfig));
                    break;
                    case "http":
                        strategies.add(parseHttpStrategy(waitStrategyPrefix + "http.", testResourcesConfig));
                        break;
                case "port":
                    assertAllowedKeys(prefix + ".port", testResourcesConfig);
                    strategies.add(Wait.forListeningPort());
                    break;
                case "healthcheck":
                    assertAllowedKeys(prefix + ".healthcheck", testResourcesConfig);
                    strategies.add(Wait.forHealthcheck());
                    break;
                case "all":
                    strategies.add(parseAllStrategy(waitStrategyPrefix + "all.", testResourcesConfig));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown wait strategy: " + strategyId);
            }
        }
        if (strategies.size() == 1) {
            return strategies.get(0);
        }
        if (strategies.size() > 1) {
            return buildWaitAllStrategy(strategies);
        }
        return null;
    }

    private static WaitAllStrategy buildWaitAllStrategy(List<WaitStrategy> strategies) {
        WaitAllStrategy waitAllStrategy = strategies.stream()
            .filter(WaitAllStrategy.class::isInstance)
            .map(WaitAllStrategy.class::cast)
            .findFirst()
            .orElse(new WaitAllStrategy());
        for (WaitStrategy strategy : strategies) {
            if (!(strategy instanceof WaitAllStrategy)) {
                waitAllStrategy = waitAllStrategy.withStrategy(strategy);
            }
        }
        return waitAllStrategy;
    }

    private static WaitStrategy parseAllStrategy(String prefix, Map<String, Object> testResourcesConfig) {
        assertAllowedKeys(prefix, testResourcesConfig, "mode", "timeout");
        String modeStr = extractStringParameterFrom(prefix, "mode", testResourcesConfig);
        WaitAllStrategy.Mode mode = WaitAllStrategy.Mode.WITH_OUTER_TIMEOUT;
        if (modeStr != null) {
            mode = WaitAllStrategy.Mode.valueOf(modeStr.toUpperCase(Locale.US));
        }
        WaitAllStrategy waitAllStrategy = new WaitAllStrategy(mode);
        String timeoutStr = extractStringParameterFrom(prefix, "timeout", testResourcesConfig);
        if (timeoutStr != null) {
            Duration startupTimeout = CONVERSION_SERVICE.convert(timeoutStr, Duration.class).orElse(null);
            waitAllStrategy = waitAllStrategy.withStartupTimeout(startupTimeout);
        }
        return waitAllStrategy;
    }

    @Nullable
    private static String determineWaitStrategyIdFor(String prefix, Map<String, Object> testResourcesConfig, String waitStrategyPrefix, String k) {
        String simpleWaitStrategyPrefix = prefix + "wait-strategy";
        if (k.equals(simpleWaitStrategyPrefix)) {
            return String.valueOf(testResourcesConfig.get(simpleWaitStrategyPrefix));
        }
        if (k.startsWith(waitStrategyPrefix)) {
            k = k.substring(waitStrategyPrefix.length());
            if (k.contains(".")) {
                return k.substring(0, k.indexOf("."));
            }
            return k;
        }
        return null;
    }

    private static HttpWaitStrategy parseHttpStrategy(String prefix, Map<String, Object> testResourcesConfig) {
        assertAllowedKeys(prefix, testResourcesConfig, "path", "port", "tls", "status-code");
        String path = extractStringParameterFrom(prefix, "path", testResourcesConfig);
        Integer port = extractIntParameterFrom(prefix, "port", testResourcesConfig);
        String tls = extractStringParameterFrom(prefix, "tls", testResourcesConfig);
        List<String> statusCode = extractListFrom(prefix, testResourcesConfig, "status-code");
        HttpWaitStrategy httpWaitStrategy = new HttpWaitStrategy().forPath(path);
        if (port != null) {
            httpWaitStrategy = httpWaitStrategy.forPort(port);
        }
        if (tls != null && Boolean.TRUE.equals(Boolean.parseBoolean(tls))) {
            httpWaitStrategy = httpWaitStrategy.usingTls();
        }
        if (!statusCode.isEmpty()) {
            for (String status : statusCode) {
                httpWaitStrategy = httpWaitStrategy.forStatusCode(Integer.parseInt(status));
            }
        }
        return httpWaitStrategy;
    }

    private static LogMessageWaitStrategy parseLogStrategy(String prefix, Map<String, Object> testResourcesConfig) {
        assertAllowedKeys(prefix, testResourcesConfig, "regex", "times");
        String regex = extractStringParameterFrom(prefix, "regex", testResourcesConfig);
        Integer times = extractIntParameterFrom(prefix, "times", testResourcesConfig);
        return Wait.forLogMessage(regex, times != null ? times : 1);
    }

    private static void assertAllowedKeys(String prefix, Map<String, Object> testResourcesConfig, String... allowed) {
        Set<String> allowedKeys = new LinkedHashSet<>(Arrays.asList(allowed));
        List<String> disallowed = testResourcesConfig.keySet()
            .stream()
            .filter(k -> k.startsWith(prefix))
            .map(k -> k.substring(prefix.length()))
            .map(k -> {
                if (k.contains(".")) {
                    return k.substring(0, k.indexOf("."));
                }
                return k;
            })
            .filter(k -> !allowedKeys.contains(k))
            .collect(Collectors.toList());
        if (!disallowed.isEmpty()) {
            if (allowed.length == 0) {
                throw new IllegalArgumentException("Wait strategy " + prefix + " is not configurable but the following keys are set: " + disallowed);
            }
            if (disallowed.size() == 1) {
                throw new IllegalArgumentException("Wait strategy " + prefix + " does not support the following key: " + disallowed.get(0)
                                                   + ". Allowed keys are: " + allowedKeys);
            }
            throw new IllegalArgumentException("Wait strategy " + prefix + " does not support the following keys: " + disallowed
                                               + ". Allowed keys are: " + allowedKeys);
        }

    }

    static GenericContainer<?> applyMetadata(TestContainerMetadata md, GenericContainer<?> container) {
        Collection<Integer> exposedPorts = md.getExposedPorts().values();
        if (!exposedPorts.isEmpty()) {
            container.withExposedPorts(exposedPorts.toArray(new Integer[0]));
        }
        md.getRwFsBinds().forEach((hostPath, containerPath) -> applyFsBind(container, hostPath, containerPath, BindMode.READ_WRITE));
        md.getRoFsBinds().forEach((hostPath, containerPath) -> applyFsBind(container, hostPath, containerPath, BindMode.READ_ONLY));
        md.getRwTmpfsMappings().forEach((mapping) -> applyTmpFsMapping(container, mapping, BindMode.READ_WRITE));
        md.getRoTmpfsMappings().forEach((mapping) -> applyTmpFsMapping(container, mapping, BindMode.READ_ONLY));
        if (!md.getCommand().isEmpty()) {
            container.withCommand(md.getCommand().toArray(new String[0]));
        }
        container.withEnv(md.getEnv());
        container.withLabels(md.getLabels());
        md.getStartupTimeout().ifPresent(container::withStartupTimeout);
        md.getFileCopies().forEach(copy -> container.withCopyFileToContainer(copy.getFile(), copy.getDestination()));
        md.getSharedMemory().ifPresent(container::withSharedMemorySize);
        md.getMemory().ifPresent(memory -> container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemory(memory)));
        md.getSwapMemory().ifPresent(memory -> container.withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withMemorySwap(memory)));
        md.getNetwork().ifPresent(network -> container.withNetwork(TestContainers.network(network)));
        if (!md.getNetworkAliases().isEmpty()) {
            container.withNetworkAliases(md.getNetworkAliases().toArray(new String[0]));
        }
        md.getNetworkMode().ifPresent(container::withNetworkMode);
        md.getWaitStrategy().ifPresent(container::setWaitStrategy);
        return container;
    }

    static void applyFsBind(GenericContainer<?> container, String hostPath, String containerPath, BindMode bindMode) {
        if (hostPath.startsWith(CLASSPATH_PREFIX)) {
            container.withClasspathResourceMapping(hostPath.substring(CLASSPATH_PREFIX.length()), containerPath, bindMode);
        } else {
            container.withFileSystemBind(hostPath, containerPath, bindMode);
        }
    }

    static void applyTmpFsMapping(GenericContainer<?> container, String mapping, BindMode bindMode) {
        container.withTmpFs(Collections.singletonMap(mapping, bindMode.accessMode.name()));
    }
}
