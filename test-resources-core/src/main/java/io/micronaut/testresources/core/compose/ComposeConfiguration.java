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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for Compose-backed test resources.
 *
 * @param enabled Whether Compose support is enabled.
 * @param workingDirectory The directory where Compose commands should run.
 * @param files The Compose files to use.
 * @param profiles The Compose profiles to activate.
 * @param projectName The Compose project name.
 * @param start Whether Test Resources should start Compose services.
 * @param stopManaged Whether Test Resources should stop services it started.
 * @param startupTimeout The bounded timeout for Compose commands.
 */
@Internal
record ComposeConfiguration(
    boolean enabled,
    Path workingDirectory,
    List<Path> files,
    List<String> profiles,
    String projectName,
    boolean start,
    boolean stopManaged,
    Duration startupTimeout
) {
    private static final String PREFIX = "compose.";
    private static final List<String> DEFAULT_FILES = List.of(
        "compose.yml",
        "compose.yaml",
        "docker-compose.yml",
        "docker-compose.yaml"
    );
    private static final String SCOPE_PROPERTY = "micronaut.test.resources.scope";

    static ComposeConfiguration from(Map<String, Object> testResourcesConfig) {
        return from(testResourcesConfig, Map.of());
    }

    static ComposeConfiguration from(Map<String, Object> testResourcesConfig, Map<String, Object> requestedProperties) {
        boolean enabled = booleanValue(testResourcesConfig, "enabled", false);
        Path workingDirectory = Optional.ofNullable(stringValue(testResourcesConfig, "working-directory"))
            .map(Path::of)
            .orElseGet(() -> Path.of(System.getProperty("user.dir")))
            .toAbsolutePath()
            .normalize();
        List<Path> files = files(testResourcesConfig, workingDirectory);
        List<String> profiles = listValue(testResourcesConfig.get(PREFIX + "profiles"));
        String projectName = Optional.ofNullable(explicitProjectName(testResourcesConfig))
            .filter(s -> !s.isBlank())
            .orElseGet(() -> defaultProjectName(workingDirectory, requestedProperties.get(SCOPE_PROPERTY)));
        boolean start = booleanValue(testResourcesConfig, "start", true);
        boolean stopManaged = booleanValue(testResourcesConfig, "stop-managed", true);
        Duration timeout = durationValue(testResourcesConfig, "startup-timeout", Duration.ofSeconds(60));
        return new ComposeConfiguration(enabled, workingDirectory, files, profiles, projectName, start, stopManaged, timeout);
    }

    boolean usable() {
        return enabled && !files.isEmpty();
    }

    private static List<Path> files(Map<String, Object> testResourcesConfig, Path workingDirectory) {
        List<String> configured = listValue(testResourcesConfig.get(PREFIX + "files"));
        if (configured.isEmpty()) {
            return DEFAULT_FILES.stream()
                .map(workingDirectory::resolve)
                .filter(Files::isRegularFile)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .toList();
        }
        return configured.stream()
            .map(Path::of)
            .map(path -> path.isAbsolute() ? path : workingDirectory.resolve(path))
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .toList();
    }

    private static boolean booleanValue(Map<String, Object> testResourcesConfig, String key, boolean defaultValue) {
        Object value = testResourcesConfig.get(PREFIX + key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static String explicitProjectName(Map<String, Object> testResourcesConfig) {
        return stringValue(testResourcesConfig, "project-name");
    }

    private static String stringValue(Map<String, Object> testResourcesConfig, String key) {
        Object value = testResourcesConfig.get(PREFIX + key);
        return value == null ? null : String.valueOf(value);
    }

    private static Duration durationValue(Map<String, Object> testResourcesConfig, String key, Duration defaultValue) {
        Object value = testResourcesConfig.get(PREFIX + key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue());
        }
        String asString = String.valueOf(value).trim();
        if (asString.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(asString.substring(0, asString.length() - 2)));
        }
        if (asString.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(asString.substring(0, asString.length() - 1)));
        }
        if (asString.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(asString.substring(0, asString.length() - 1)));
        }
        return Duration.parse(asString);
    }

    private static List<String> listValue(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(v -> addListValue(result, v));
        } else {
            for (String item : String.valueOf(value).split(",")) {
                addListValue(result, item);
            }
        }
        return List.copyOf(result);
    }

    private static void addListValue(List<String> values, Object value) {
        if (value != null) {
            String asString = String.valueOf(value).trim();
            if (!asString.isBlank()) {
                values.add(asString);
            }
        }
    }

    private static String defaultProjectName(Path workingDirectory, Object scope) {
        String directory = Objects.toString(workingDirectory.getFileName(), "micronaut-test-resources");
        String suffix = scope == null || String.valueOf(scope).isBlank() ? "" : "-" + scope;
        String normalized = (directory + suffix).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return "mn-tr-" + normalized;
    }
}
