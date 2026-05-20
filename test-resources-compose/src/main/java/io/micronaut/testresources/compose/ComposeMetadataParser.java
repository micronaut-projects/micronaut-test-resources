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
package io.micronaut.testresources.compose;

import org.yaml.snakeyaml.Yaml;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ComposeMetadataParser {
    private final Yaml yaml = new Yaml();

    ComposeProject parse(ComposeConfiguration configuration) {
        Map<String, Map<String, Object>> services = new LinkedHashMap<>();
        for (Path file : configuration.files()) {
            parseFile(file).forEach((name, definition) ->
                services.merge(name, definition, ComposeMetadataParser::mergeService)
            );
        }
        return new ComposeProject(services.entrySet().stream()
            .map(entry -> service(entry.getKey(), entry.getValue()))
            .toList());
    }

    private Map<String, Map<String, Object>> parseFile(Path file) {
        Object loaded;
        try (InputStream inputStream = Files.newInputStream(file)) {
            loaded = yaml.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Docker Compose file " + file, e);
        }
        Map<String, Object> root = objectMap(loaded);
        Map<String, Object> serviceDefinitions = objectMap(root.get("services"));
        Map<String, Map<String, Object>> services = new LinkedHashMap<>();
        serviceDefinitions.forEach((name, value) -> services.put(name, objectMap(value)));
        return services;
    }

    private static Map<String, Object> mergeService(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> merged = new LinkedHashMap<>(base);
        override.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            switch (key) {
                case "environment", "labels" -> merged.put(key, mergeStringMap(merged.get(key), value));
                case "expose", "ports", "profiles" -> merged.put(key, mergeList(merged.get(key), value));
                default -> merged.put(key, value);
            }
        });
        return merged;
    }

    private static Map<String, String> mergeStringMap(@Nullable Object base, @Nullable Object override) {
        Map<String, String> merged = new LinkedHashMap<>(stringMap(base));
        merged.putAll(stringMap(override));
        return Map.copyOf(merged);
    }

    private static List<String> mergeList(@Nullable Object base, @Nullable Object override) {
        List<String> merged = new ArrayList<>(list(base));
        merged.addAll(list(override));
        return List.copyOf(merged);
    }

    private ComposeService service(String name, Map<String, Object> definition) {
        return new ComposeService(
            name,
            stringValue(definition.get("image")),
            stringMap(definition.get("labels")),
            stringMap(definition.get("environment")),
            ports(definition),
            list(definition.get("profiles"))
        );
    }

    private static Map<String, Object> objectMap(@Nullable Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
        return result;
    }

    private static Map<String, String> stringMap(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, String> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                if (mapValue != null) {
                    result.put(String.valueOf(key), String.valueOf(mapValue));
                }
            });
            return Map.copyOf(result);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String entry : list(value)) {
            int separator = entry.indexOf('=');
            if (separator > 0) {
                result.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }
        return Map.copyOf(result);
    }

    private static List<Integer> ports(Map<String, Object> definition) {
        List<Integer> ports = new ArrayList<>();
        list(definition.get("ports")).stream()
            .map(ComposeMetadataParser::targetPort)
            .forEach(port -> {
                if (port > 0) {
                    ports.add(port);
                }
            });
        list(definition.get("expose")).stream()
            .map(ComposeMetadataParser::targetPort)
            .forEach(port -> {
                if (port > 0) {
                    ports.add(port);
                }
            });
        return List.copyOf(ports);
    }

    @SuppressWarnings("java:S7467")
    private static int targetPort(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        String port = value;
        int protocol = port.indexOf('/');
        if (protocol >= 0) {
            port = port.substring(0, protocol);
        }
        int separator = port.lastIndexOf(':');
        if (separator >= 0) {
            port = port.substring(separator + 1);
        }
        int range = port.indexOf('-');
        if (range >= 0) {
            port = port.substring(0, range);
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static List<String> list(@Nullable Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> result = new ArrayList<>();
            iterable.forEach(item -> {
                if (item instanceof Map<?, ?> map) {
                    Object target = map.get("target");
                    if (target != null) {
                        result.add(String.valueOf(target));
                    }
                } else if (item != null) {
                    result.add(String.valueOf(item));
                }
            });
            return List.copyOf(result);
        }
        return List.of(String.valueOf(value));
    }

    private static String stringValue(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value).toLowerCase(Locale.ROOT);
    }
}
