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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Base class for provider-owned Compose database mappings.
 */
public abstract class AbstractComposeDatabaseTestResourcesProvider implements ComposeTestResourcesProvider {
    public static final String DATASOURCES = "datasources";
    public static final String R2DBC_DATASOURCES = "r2dbc.datasources";
    public static final String JPA = "jpa";
    public static final String URL = "url";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DRIVER = "driver-class-name";
    public static final String DB_NAME = "db-name";
    public static final String TYPE = "db-type";
    public static final String DIALECT = "dialect";
    public static final String RESOURCE_NAME = "test-resources.resource-name";
    public static final String R2DBC_DRIVER = "driverClassName";
    public static final String HIBERNATE_CONNECTION = "properties.hibernate.connection.";

    private final Set<String> aliases;
    private final Metadata metadata;
    private final Kind kind;

    protected AbstractComposeDatabaseTestResourcesProvider(Kind kind, Metadata metadata) {
        this.kind = kind;
        this.metadata = metadata;
        this.aliases = ComposeTestResourcesProvider.aliases(metadata.serviceType(), metadata.aliases());
    }

    @Override
    public final String getServiceType() {
        return metadata.serviceType();
    }

    @Override
    public final Set<String> getAliases() {
        return aliases;
    }

    @Override
    public final int getPort() {
        return metadata.port();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries) {
        List<String> resolvable = new ArrayList<>();
        switch (kind) {
            case JDBC -> propertyEntries.getOrDefault(DATASOURCES, List.of()).forEach(datasource -> {
                resolvable.add(property(DATASOURCES, datasource, URL));
                resolvable.add(property(DATASOURCES, datasource, USERNAME));
                resolvable.add(property(DATASOURCES, datasource, PASSWORD));
                resolvable.add(property(DATASOURCES, datasource, DRIVER));
            });
            case R2DBC -> Stream.concat(
                    propertyEntries.getOrDefault(R2DBC_DATASOURCES, List.of()).stream(),
                    propertyEntries.getOrDefault(DATASOURCES, List.of()).stream()
                )
                .distinct()
                .forEach(datasource -> {
                    resolvable.add(property(R2DBC_DATASOURCES, datasource, URL));
                    resolvable.add(property(R2DBC_DATASOURCES, datasource, USERNAME));
                    resolvable.add(property(R2DBC_DATASOURCES, datasource, PASSWORD));
                });
            case HIBERNATE_REACTIVE -> Stream.concat(
                    propertyEntries.getOrDefault(JPA, List.of()).stream(),
                    propertyEntries.getOrDefault(DATASOURCES, List.of()).stream()
                )
                .distinct()
                .forEach(datasource -> {
                    resolvable.add(property(JPA, datasource, HIBERNATE_CONNECTION + URL));
                    resolvable.add(property(JPA, datasource, HIBERNATE_CONNECTION + USERNAME));
                    resolvable.add(property(JPA, datasource, HIBERNATE_CONNECTION + PASSWORD));
                });
            default -> throw new IllegalStateException("Unsupported Compose database provider kind: " + kind);
        }
        return resolvable;
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return switch (kind) {
            case JDBC -> List.of(DATASOURCES);
            case R2DBC -> List.of(DATASOURCES, R2DBC_DATASOURCES);
            case HIBERNATE_REACTIVE -> List.of(DATASOURCES, JPA);
        };
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        @Nullable String datasource = datasourceName(expression);
        if (datasource == null) {
            return List.of();
        }
        if (expression.startsWith(R2DBC_DATASOURCES + ".")) {
            return Stream.of(
                    property(DATASOURCES, datasource, URL),
                    property(R2DBC_DATASOURCES, datasource, TYPE),
                    property(R2DBC_DATASOURCES, datasource, DIALECT),
                    property(R2DBC_DATASOURCES, datasource, R2DBC_DRIVER),
                    property(R2DBC_DATASOURCES, datasource, DB_NAME),
                    property(R2DBC_DATASOURCES, datasource, RESOURCE_NAME),
                    property(DATASOURCES, datasource, DB_NAME)
                )
                .toList();
        }
        if (expression.startsWith(JPA + ".")) {
            return Stream.of(
                    property(JPA, datasource, HIBERNATE_CONNECTION + TYPE),
                    property(DATASOURCES, datasource, TYPE),
                    property(DATASOURCES, datasource, URL),
                    property(DATASOURCES, datasource, USERNAME),
                    property(DATASOURCES, datasource, PASSWORD),
                    property(DATASOURCES, datasource, DB_NAME)
                )
                .toList();
        }
        return Stream.of(TYPE, DIALECT, DB_NAME, RESOURCE_NAME)
            .map(property -> property(DATASOURCES, datasource, property))
            .toList();
    }

    @Override
    public boolean matches(String propertyName, ComposeService service) {
        @Nullable String datasource = datasourceName(propertyName);
        if (datasource == null || !ComposeTestResourcesProvider.super.matches(service)) {
            return false;
        }
        String label = service.labels().get(ComposeLabels.DATASOURCE);
        if (label != null) {
            return label.equalsIgnoreCase(datasource);
        }
        return "default".equals(datasource);
    }

    @Override
    public boolean supports(String propertyName, Map<String, Object> properties) {
        @Nullable String datasource = datasourceName(propertyName);
        return datasource != null && requested(datasource, propertyName, properties);
    }

    @Override
    public @Nullable String resolve(ResolutionContext context) {
        @Nullable String datasource = datasourceName(context.propertyName());
        String leaf = propertyName(context.propertyName());
        return switch (leaf) {
            case URL -> context.propertyName().startsWith(R2DBC_DATASOURCES + ".") ? r2dbcUrl(context, datasource) : jdbcUrl(context, datasource);
            case HIBERNATE_CONNECTION + URL -> jdbcUrl(context, datasource);
            case USERNAME, HIBERNATE_CONNECTION + USERNAME -> username(context);
            case PASSWORD, HIBERNATE_CONNECTION + PASSWORD -> password(context);
            case DRIVER -> metadata.driverClassName();
            default -> null;
        };
    }

    private boolean requested(String datasource, String propertyName, Map<String, Object> properties) {
        String type = stringValue(properties.get(property(DATASOURCES, datasource, TYPE)));
        if (type == null && propertyName.startsWith(R2DBC_DATASOURCES + ".")) {
            type = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, TYPE)));
        }
        if (type == null && propertyName.startsWith(JPA + ".")) {
            type = stringValue(properties.get(property(JPA, datasource, HIBERNATE_CONNECTION + TYPE)));
        }
        if (type != null && aliases.contains(ComposeTestResourcesProvider.normalize(type))) {
            return true;
        }
        String dialect = stringValue(properties.get(property(DATASOURCES, datasource, DIALECT)));
        if (dialect == null && propertyName.startsWith(R2DBC_DATASOURCES + ".")) {
            dialect = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, DIALECT)));
        }
        if (dialect != null && aliases.contains(ComposeTestResourcesProvider.normalize(dialect))) {
            return true;
        }
        String driver = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, R2DBC_DRIVER)));
        return driver != null && aliases.stream().anyMatch(ComposeTestResourcesProvider.normalize(driver)::contains);
    }

    private String jdbcUrl(ResolutionContext context, @Nullable String datasource) {
        String database = database(context, datasource);
        return switch (metadata.serviceType()) {
            case "mssql" -> metadata.jdbcScheme() + "://" + context.hostPort() + ";databaseName=" + database;
            case "oracle" -> metadata.jdbcScheme() + ":@" + "//" + context.hostPort() + "/" + database;
            default -> metadata.jdbcScheme() + "://" + context.hostPort() + "/" + database;
        };
    }

    private String r2dbcUrl(ResolutionContext context, @Nullable String datasource) {
        return "r2dbc:" + metadata.r2dbcScheme() + "://" + context.hostPort() + "/" + database(context, datasource);
    }

    private String username(ResolutionContext context) {
        return context.labelOrEnvironment(ComposeLabels.USERNAME, metadata.userEnvironment(), metadata.defaultUser());
    }

    private String password(ResolutionContext context) {
        return context.labelOrEnvironment(ComposeLabels.PASSWORD, metadata.passwordEnvironment(), metadata.defaultPassword());
    }

    private String database(ResolutionContext context, @Nullable String datasource) {
        @Nullable String configured = null;
        if (datasource != null) {
            configured = stringValue(context.properties().get(property(DATASOURCES, datasource, DB_NAME)));
            if (configured == null) {
                configured = stringValue(context.properties().get(property(R2DBC_DATASOURCES, datasource, DB_NAME)));
            }
        }
        return configured == null ? context.labelOrEnvironment(ComposeLabels.DATABASE, metadata.databaseEnvironment(), metadata.defaultDatabase()) : configured;
    }

    static @Nullable String datasourceName(String expression) {
        if (expression.startsWith(DATASOURCES + ".")) {
            return nameAfterPrefix(expression, DATASOURCES);
        }
        if (expression.startsWith(R2DBC_DATASOURCES + ".")) {
            return nameAfterPrefix(expression, R2DBC_DATASOURCES);
        }
        if (expression.startsWith(JPA + ".")) {
            return nameAfterPrefix(expression, JPA);
        }
        return null;
    }

    static String property(String prefix, String datasource, String property) {
        return prefix + "." + datasource + "." + property;
    }

    static String propertyName(String expression) {
        @Nullable String datasource = datasourceName(expression);
        if (datasource == null) {
            return expression;
        }
        return expression.substring(expression.indexOf(datasource) + datasource.length() + 1);
    }

    private static @Nullable String nameAfterPrefix(String expression, String prefix) {
        int start = prefix.length() + 1;
        int end = expression.indexOf('.', start);
        return end > start ? expression.substring(start, end) : null;
    }

    private static @Nullable String stringValue(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Database property family handled by a provider-owned Compose mapping.
     */
    public enum Kind {
        JDBC,
        R2DBC,
        HIBERNATE_REACTIVE
    }

    /**
     * Database service metadata needed to map Compose endpoints to Micronaut properties.
     *
     * @param serviceType The canonical Compose service type.
     * @param aliases Additional service labels or image fragments.
     * @param port The service container port.
     * @param jdbcScheme The JDBC URL scheme.
     * @param r2dbcScheme The R2DBC URL scheme.
     * @param driverClassName The JDBC driver class name.
     * @param userEnvironment The standard image environment variable for the username.
     * @param passwordEnvironment The standard image environment variable for the password.
     * @param databaseEnvironment The standard image environment variable for the database name.
     * @param defaultUser The provider default username.
     * @param defaultPassword The provider default password.
     * @param defaultDatabase The provider default database name.
     */
    public record Metadata(
        String serviceType,
        Set<String> aliases,
        int port,
        String jdbcScheme,
        String r2dbcScheme,
        String driverClassName,
        String userEnvironment,
        String passwordEnvironment,
        String databaseEnvironment,
        String defaultUser,
        String defaultPassword,
        String defaultDatabase
    ) {
    }
}
