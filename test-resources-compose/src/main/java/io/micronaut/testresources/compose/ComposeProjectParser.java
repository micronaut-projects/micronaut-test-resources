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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts Docker Compose JSON output into the normalized service model.
 */
@Internal
final class ComposeProjectParser {
    private final JsonMapper jsonMapper;

    ComposeProjectParser() {
        this(JsonMapper.createDefault());
    }

    ComposeProjectParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    List<ComposeService> parse(String configJson, String psJson, Set<String> externallyManagedServices) {
        Map<String, Map<String, Object>> services = parseConfigServices(configJson);
        Map<String, List<ComposePort>> ports = parsePublishedPorts(psJson);
        return services.entrySet()
            .stream()
            .map(entry -> {
                String name = entry.getKey();
                Map<String, Object> service = entry.getValue();
                return new ComposeService(
                    name,
                    stringValue(service.get("image")),
                    labels(service.get("labels")),
                    environment(service.get("environment")),
                    ports.getOrDefault(name, List.of()),
                    externallyManagedServices.contains(name)
                );
            })
            .toList();
    }

    Set<String> runningServices(String psJson) {
        return parsePsEntries(psJson).stream()
            .filter(entry -> {
                String state = stringValue(firstPresent(entry, "State", "state"));
                return state.toLowerCase(Locale.ROOT).contains("running");
            })
            .map(entry -> stringValue(firstPresent(entry, "Service", "service", "Name", "name")))
            .filter(name -> !name.isBlank())
            .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> parseConfigServices(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return Map.of();
        }
        Object parsed = readJson(configJson);
        if (!(parsed instanceof Map<?, ?> root)) {
            return Map.of();
        }
        Object services = root.get("services");
        if (!(services instanceof Map<?, ?> servicesMap)) {
            return Map.of();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : servicesMap.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> service) {
                result.put(String.valueOf(entry.getKey()), (Map<String, Object>) service);
            }
        }
        return result;
    }

    private Map<String, List<ComposePort>> parsePublishedPorts(String psJson) {
        return parsePsEntries(psJson).stream()
            .collect(Collectors.toMap(
                entry -> stringValue(firstPresent(entry, "Service", "service", "Name", "name")),
                this::ports,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parsePsEntries(String psJson) {
        if (psJson == null || psJson.isBlank()) {
            return List.of();
        }
        Object parsed = readJson(psJson);
        if (parsed instanceof List<?> list) {
            return list.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(map -> (Map<String, Object>) map)
                .toList();
        }
        if (parsed instanceof Map<?, ?> map) {
            return List.of((Map<String, Object>) map);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (String line : psJson.lines().map(String::trim).filter(s -> !s.isBlank()).toList()) {
            Object lineParsed = readJson(line);
            if (lineParsed instanceof Map<?, ?> map) {
                entries.add((Map<String, Object>) map);
            }
        }
        return entries;
    }

    private Object readJson(String json) {
        try {
            return jsonMapper.readValue(json.getBytes(StandardCharsets.UTF_8), Argument.OBJECT_ARGUMENT);
        } catch (IOException e) {
            throw new ComposeCliException("Unable to parse Docker Compose JSON output", e);
        }
    }

    private Map<String, String> labels(Object labels) {
        if (labels instanceof Collection<?> collection) {
            return keyValueCollection(collection);
        }
        return stringMap(labels);
    }

    private Map<String, String> environment(Object environment) {
        if (environment instanceof Collection<?> collection) {
            return keyValueCollection(collection);
        }
        return stringMap(environment);
    }

    private Map<String, String> keyValueCollection(Collection<?> collection) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Object item : collection) {
            String entry = String.valueOf(item);
            int separator = entry.indexOf('=');
            if (separator > 0) {
                result.put(entry.substring(0, separator), entry.substring(separator + 1));
            }
        }
        return result;
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ComposePort> ports(Map<String, Object> psEntry) {
        Object publishers = firstPresent(psEntry, "Publishers", "publishers");
        if (publishers instanceof Collection<?> collection) {
            List<ComposePort> ports = new ArrayList<>();
            for (Object item : collection) {
                if (item instanceof Map<?, ?> publisher) {
                    Integer target = integerValue(firstPresent((Map<String, Object>) publisher, "TargetPort", "targetPort", "target_port"));
                    Integer published = integerValue(firstPresent((Map<String, Object>) publisher, "PublishedPort", "publishedPort", "published_port"));
                    if (target != null && published != null) {
                        ports.add(new ComposePort(host(firstPresent((Map<String, Object>) publisher, "URL", "url", "HostIp", "hostIp")), published, target));
                    }
                }
            }
            return List.copyOf(ports);
        }
        return List.of();
    }

    private String host(Object value) {
        String host = stringValue(value);
        if (host.isBlank() || "0.0.0.0".equals(host) || "::".equals(host)) {
            return "localhost";
        }
        return host;
    }

    private static Object firstPresent(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String asString = String.valueOf(value);
        if (asString.isBlank()) {
            return null;
        }
        return Integer.parseInt(asString);
    }
}
