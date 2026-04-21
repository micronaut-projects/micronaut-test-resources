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
package io.micronaut.testresources.client;

import io.micronaut.core.annotation.Internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Internal
final class IntellijIdeaDatasourceExporter {
    static final String ENABLED = "intellij-idea.enabled";
    static final String OUTPUT_PATH = "intellij-idea.output-path";
    static final String DEFAULT_OUTPUT_PATH = ".micronaut/test-resources/intellij-idea-datasources.xml";

    private static final String DATASOURCES_PREFIX = "datasources.";
    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DRIVER_CLASS_NAME = "driver-class-name";

    private final Path projectDirectory;
    private final Map<String, ExportSessionState> sessions = new LinkedHashMap<>();

    IntellijIdeaDatasourceExporter() {
        this(Path.of("").toAbsolutePath().normalize());
    }

    IntellijIdeaDatasourceExporter(Path projectDirectory) {
        this.projectDirectory = projectDirectory.toAbsolutePath().normalize();
    }

    synchronized void export(String propertyName, String value, Map<String, Object> testResourcesConfig) {
        export(propertyName, value, testResourcesConfig, "default");
    }

    synchronized void export(String propertyName, String value, Map<String, Object> testResourcesConfig, String sessionId) {
        var config = ExportConfiguration.from(testResourcesConfig, projectDirectory);
        if (!config.enabled()) {
            return;
        }
        JdbcDatasourceProperty.parse(propertyName)
            .ifPresent(property -> {
                var sessionState = sessions.computeIfAbsent(sessionId, unused -> new ExportSessionState(config.outputPath()));
                var previousOutputPath = sessionState.outputPath;
                sessionState.outputPath = config.outputPath();
                var state = sessionState.datasources.computeIfAbsent(property.datasourceName(), JdbcDatasourceState::new);
                state.record(property.propertyName(), value);
                if (previousOutputPath != null && !previousOutputPath.equals(sessionState.outputPath)) {
                    rewriteExport(previousOutputPath);
                }
                rewriteExport(sessionState.outputPath);
            });
    }

    synchronized void clearSession(String sessionId) {
        var sessionState = sessions.remove(sessionId);
        if (sessionState != null) {
            rewriteExport(sessionState.outputPath);
        }
    }

    private List<JdbcDatasourceState> completeDatasources(Path outputPath) {
        var datasources = new LinkedHashMap<String, JdbcDatasourceState>();
        for (ExportSessionState sessionState : sessions.values()) {
            if (outputPath.equals(sessionState.outputPath)) {
                sessionState.datasources.forEach(datasources::put);
            }
        }
        return datasources.values()
            .stream()
            .filter(JdbcDatasourceState::isComplete)
            .sorted(Comparator.comparing(JdbcDatasourceState::name))
            .toList();
    }

    private void rewriteExport(Path outputPath) {
        writeExport(outputPath, completeDatasources(outputPath));
    }

    private void writeExport(Path outputPath, List<JdbcDatasourceState> completeDatasources) {
        try {
            if (completeDatasources.isEmpty()) {
                Files.deleteIfExists(outputPath);
                return;
            }
            var parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, render(completeDatasources), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TestResourcesException("Failed to write IntelliJ IDEA datasource export to " + outputPath, e);
        }
    }

    private String render(List<JdbcDatasourceState> completeDatasources) {
        var sb = new StringBuilder("#DataSourceSettings#\n");
        for (JdbcDatasourceState datasource : completeDatasources) {
            var driverMetadata = DriverMetadata.from(datasource.url, datasource.driverClassName);
            sb.append("#LocalDataSource: ").append(datasource.name).append('\n');
            sb.append("#BEGIN#\n");
            sb.append("<data-source source=\"LOCAL\" name=\"")
                .append(escape(datasource.name))
                .append("\" uuid=\"")
                .append(datasource.uuid())
                .append("\">");
            driverMetadata.dbms()
                .ifPresent(dbms -> sb.append("<database-info product=\"\" version=\"\" jdbc-version=\"\" driver-name=\"\" driver-version=\"\" dbms=\"")
                    .append(escape(dbms))
                    .append("\"/>"));
            sb.append("<driver-ref>")
                .append(escape(driverMetadata.driverRef()))
                .append("</driver-ref>")
                .append("<synchronize>true</synchronize>")
                .append("<jdbc-driver>")
                .append(escape(datasource.driverClassName))
                .append("</jdbc-driver>")
                .append("<jdbc-url>")
                .append(escape(datasource.url))
                .append("</jdbc-url>")
                .append("<user-name>")
                .append(escape(datasource.username))
                .append("</user-name>")
                .append("<password>")
                .append(escape(datasource.password))
                .append("</password>")
                .append("<working-dir>$ProjectFileDir$</working-dir>")
                .append("</data-source>\n")
                .append("#END#\n");
        }
        return sb.toString();
    }

    private static String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static final class ExportSessionState {
        private Path outputPath;
        private final Map<String, JdbcDatasourceState> datasources = new LinkedHashMap<>();

        private ExportSessionState(Path outputPath) {
            this.outputPath = outputPath;
        }
    }

    private record ExportConfiguration(boolean enabled, Path outputPath) {
        private static ExportConfiguration from(Map<String, Object> testResourcesConfig, Path projectDirectory) {
            boolean enabled = booleanValue(testResourcesConfig, ENABLED, false);
            Path outputPath = stringValue(testResourcesConfig, OUTPUT_PATH)
                .map(Path::of)
                .map(path -> path.isAbsolute() ? path : projectDirectory.resolve(path).normalize())
                .orElse(projectDirectory.resolve(DEFAULT_OUTPUT_PATH).normalize());
            return new ExportConfiguration(enabled, outputPath);
        }

        private static boolean booleanValue(Map<String, Object> testResourcesConfig, String key, boolean defaultValue) {
            Object value = testResourcesConfig.get(key);
            if (value == null) {
                value = System.getProperty(ConfigFinder.systemPropertyNameOf(key));
            }
            if (value == null) {
                return defaultValue;
            }
            if (value instanceof Boolean bool) {
                return bool;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }

        private static Optional<String> stringValue(Map<String, Object> testResourcesConfig, String key) {
            Object value = testResourcesConfig.get(key);
            if (value == null) {
                value = System.getProperty(ConfigFinder.systemPropertyNameOf(key));
            }
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(String.valueOf(value));
        }
    }

    private record JdbcDatasourceProperty(String datasourceName, String propertyName) {
        private static Optional<JdbcDatasourceProperty> parse(String propertyName) {
            if (!propertyName.startsWith(DATASOURCES_PREFIX)) {
                return Optional.empty();
            }
            String remainder = propertyName.substring(DATASOURCES_PREFIX.length());
            int separator = remainder.indexOf('.');
            if (separator < 0) {
                return Optional.empty();
            }
            String datasourceName = remainder.substring(0, separator);
            String datasourceProperty = remainder.substring(separator + 1);
            if (URL.equals(datasourceProperty)
                || USERNAME.equals(datasourceProperty)
                || PASSWORD.equals(datasourceProperty)
                || DRIVER_CLASS_NAME.equals(datasourceProperty)) {
                return Optional.of(new JdbcDatasourceProperty(datasourceName, datasourceProperty));
            }
            return Optional.empty();
        }
    }

    private static final class JdbcDatasourceState {
        private final String name;
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        private JdbcDatasourceState(String name) {
            this.name = name;
        }

        private void record(String propertyName, String value) {
            switch (propertyName) {
                case URL -> this.url = value;
                case USERNAME -> this.username = value;
                case PASSWORD -> this.password = value;
                case DRIVER_CLASS_NAME -> this.driverClassName = value;
                default -> {
                }
            }
        }

        private boolean isComplete() {
            return url != null && username != null && password != null && driverClassName != null;
        }

        private String name() {
            return name;
        }

        private String uuid() {
            return UUID.nameUUIDFromBytes(("intellij-idea-datasource:" + name).getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private record DriverMetadata(String driverRef, Optional<String> dbms) {
        private static DriverMetadata from(String jdbcUrl, String driverClassName) {
            String lowerDriver = driverClassName.toLowerCase();
            String lowerUrl = jdbcUrl.toLowerCase();
            if (lowerDriver.contains("postgresql") || lowerUrl.startsWith("jdbc:postgresql:")) {
                return new DriverMetadata("postgresql", Optional.of("POSTGRES"));
            }
            if (lowerDriver.contains("mariadb") || lowerUrl.startsWith("jdbc:mariadb:")) {
                return new DriverMetadata("mariadb", Optional.of("MARIADB"));
            }
            if (lowerDriver.contains("mysql") || lowerUrl.startsWith("jdbc:mysql:")) {
                return new DriverMetadata("mysql", Optional.of("MYSQL"));
            }
            if (lowerDriver.contains("oracle") || lowerUrl.startsWith("jdbc:oracle:")) {
                return new DriverMetadata("oracle", Optional.of("ORACLE"));
            }
            if (lowerDriver.contains("sqlserver") || lowerUrl.startsWith("jdbc:sqlserver:")) {
                return new DriverMetadata("sqlserver", Optional.of("MSSQL"));
            }
            return new DriverMetadata(driverClassName, Optional.empty());
        }
    }
}
