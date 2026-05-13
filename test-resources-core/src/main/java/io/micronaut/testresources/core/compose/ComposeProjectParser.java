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
package io.micronaut.testresources.core.compose;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.serde.annotation.Serdeable;

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
        Map<String, ConfigService> services = parseConfigServices(configJson);
        Map<String, List<ComposePort>> ports = parsePublishedPorts(psJson);
        return services.entrySet()
            .stream()
            .map(entry -> {
                String name = entry.getKey();
                ConfigService service = entry.getValue();
                return new ComposeService(
                    name,
                    stringValue(service.image()),
                    labels(service.labels()),
                    environment(service.environment()),
                    ports.getOrDefault(name, List.of()),
                    externallyManagedServices.contains(name)
                );
            })
            .toList();
    }

    Set<String> runningServices(String psJson) {
        return parsePsEntries(psJson).stream()
            .filter(entry -> {
                String state = stringValue(entry.stateValue());
                return state.toLowerCase(Locale.ROOT).contains("running");
            })
            .map(PsEntry::serviceName)
            .filter(name -> !name.isBlank())
            .collect(Collectors.toSet());
    }

    private Map<String, ConfigService> parseConfigServices(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return Map.of();
        }
        ConfigProject parsed;
        try {
            parsed = readJson(configJson, Argument.of(ConfigProject.class));
        } catch (ComposeCliException e) {
            return Map.of();
        }
        return parsed.services() == null ? Map.of() : parsed.services();
    }

    private Map<String, List<ComposePort>> parsePublishedPorts(String psJson) {
        return parsePsEntries(psJson).stream()
            .collect(Collectors.toMap(
                PsEntry::serviceName,
                this::ports,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private List<PsEntry> parsePsEntries(String psJson) {
        if (psJson == null || psJson.isBlank()) {
            return List.of();
        }
        try {
            return readJson(psJson, Argument.listOf(PsEntry.class));
        } catch (ComposeCliException e) {
            try {
                return List.of(readJson(psJson, Argument.of(PsEntry.class)));
            } catch (ComposeCliException ignored) {
                return parseLineDelimitedPsEntries(psJson);
            }
        }
    }

    private List<PsEntry> parseLineDelimitedPsEntries(String psJson) {
        List<PsEntry> entries = new ArrayList<>();
        for (String line : psJson.lines().map(String::trim).filter(s -> !s.isBlank()).toList()) {
            entries.add(readJson(line, Argument.of(PsEntry.class)));
        }
        return entries;
    }

    private <T> T readJson(String json, Argument<T> type) {
        try {
            return jsonMapper.readValue(json.getBytes(StandardCharsets.UTF_8), type);
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

    private List<ComposePort> ports(PsEntry psEntry) {
        List<Publisher> publishers = psEntry.publisherEntries();
        if (publishers.isEmpty()) {
            return List.of();
        }
        List<ComposePort> ports = new ArrayList<>();
        for (Publisher publisher : publishers) {
            Integer target = publisher.target();
            Integer published = publisher.published();
            if (target != null && published != null) {
                ports.add(new ComposePort(host(publisher.host()), published, target));
            }
        }
        return List.copyOf(ports);
    }

    private String host(String value) {
        String host = stringValue(value);
        if (host.isBlank() || "0.0.0.0".equals(host) || "::".equals(host)) {
            return "localhost";
        }
        return host;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Introspected
    @Serdeable.Deserializable
    public record ConfigProject(Map<String, ConfigService> services) {
    }

    @Introspected
    @Serdeable.Deserializable
    public record ConfigService(String image, Object labels, Object environment) {
    }

    @Introspected
    @Serdeable.Deserializable
    public record PsEntry(
        String Service,
        String service,
        String Name,
        String name,
        String State,
        String state,
        List<Publisher> Publishers,
        List<Publisher> publishers
    ) {
        String serviceName() {
            return firstNonBlank(Service, service, Name, name);
        }

        String stateValue() {
            return firstNonBlank(State, state);
        }

        List<Publisher> publisherEntries() {
            if (Publishers != null) {
                return Publishers;
            }
            if (publishers != null) {
                return publishers;
            }
            return List.of();
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return "";
        }
    }

    @Introspected
    @Serdeable.Deserializable
    public record Publisher(
        String URL,
        String url,
        String HostIp,
        String hostIp,
        Integer TargetPort,
        Integer targetPort,
        Integer target_port,
        Integer PublishedPort,
        Integer publishedPort,
        Integer published_port
    ) {
        String host() {
            return firstNonBlank(URL, url, HostIp, hostIp);
        }

        Integer target() {
            return firstPresent(TargetPort, targetPort, target_port);
        }

        Integer published() {
            return firstPresent(PublishedPort, publishedPort, published_port);
        }

        private static Integer firstPresent(Integer... values) {
            for (Integer value : values) {
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return "";
        }
    }
}
