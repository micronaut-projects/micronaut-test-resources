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

import io.micronaut.testresources.core.Scope;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record ComposeConfiguration(
    boolean enabled,
    Path workingDirectory,
    List<Path> files,
    List<String> profiles,
    Duration startupTimeout,
    boolean localCompose,
    String dockerImageName,
    String projectName
) {
    private static final String PREFIX = "compose.";
    private static final List<String> DEFAULT_FILES = List.of(
        "compose.yml",
        "compose.yaml",
        "docker-compose.yml",
        "docker-compose.yaml"
    );

    static ComposeConfiguration from(Map<String, Object> testResourcesConfig, Map<String, Object> requestedProperties) {
        boolean enabled = booleanValue(testResourcesConfig, "enabled", false);
        Path workingDirectory = Optional.ofNullable(stringValue(testResourcesConfig, "working-directory"))
            .map(Path::of)
            .orElseGet(() -> Path.of(System.getProperty("user.dir")))
            .toAbsolutePath()
            .normalize();
        List<Path> files = files(testResourcesConfig, workingDirectory);
        List<String> profiles = listValue(testResourcesConfig.get(PREFIX + "profiles"));
        Duration startupTimeout = durationValue(testResourcesConfig, "startup-timeout", Duration.ofSeconds(60));
        boolean localCompose = booleanValue(testResourcesConfig, "local-compose", false);
        String dockerImageName = Optional.ofNullable(stringValue(testResourcesConfig, "docker-image-name"))
            .filter(s -> !s.isBlank())
            .orElse("docker");
        String projectName = Optional.ofNullable(stringValue(testResourcesConfig, "project-name"))
            .filter(s -> !s.isBlank())
            .orElseGet(() -> defaultProjectName(workingDirectory, requestedProperties.get(Scope.PROPERTY_KEY)));
        return new ComposeConfiguration(enabled, workingDirectory, files, profiles, startupTimeout, localCompose, dockerImageName, projectName);
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

    private static @Nullable String stringValue(Map<String, Object> testResourcesConfig, String key) {
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
        String text = String.valueOf(value).trim();
        if (text.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(text.substring(0, text.length() - 2)));
        }
        if (text.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(text.substring(0, text.length() - 1)));
        }
        if (text.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(text.substring(0, text.length() - 1)));
        }
        return Duration.parse(text);
    }

    private static List<String> listValue(@Nullable Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(v -> addValue(result, v));
        } else {
            for (String item : String.valueOf(value).split(",")) {
                addValue(result, item);
            }
        }
        return List.copyOf(result);
    }

    private static void addValue(List<String> values, @Nullable Object value) {
        if (value != null) {
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) {
                values.add(text);
            }
        }
    }

    private static String defaultProjectName(Path workingDirectory, @Nullable Object scope) {
        String directory = Objects.toString(workingDirectory.getFileName(), "micronaut-test-resources");
        String suffix = scope == null || String.valueOf(scope).isBlank() ? "" : "-" + scope;
        String normalized = (directory + suffix).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return "mn-tr-" + normalized;
    }
}
