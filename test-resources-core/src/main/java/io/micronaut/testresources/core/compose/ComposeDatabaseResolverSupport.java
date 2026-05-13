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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Common Docker Compose database mapping support for provider-specific resolvers.
 */
@Internal
public final class ComposeDatabaseResolverSupport {
    public static final String URL = "url";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DRIVER_CLASS_NAME = "driver-class-name";

    private static final String DATASOURCES = "datasources";
    private static final String DATASOURCES_PREFIX = DATASOURCES + ".";
    private static final String R2DBC_DATASOURCES = "r2dbc.datasources";
    private static final String R2DBC_DATASOURCES_PREFIX = R2DBC_DATASOURCES + ".";
    private static final String DB_NAME = "db-name";

    private ComposeDatabaseResolverSupport() {
    }

    public static Optional<String> resolveJdbc(String propertyName,
                                               Map<String, Object> properties,
                                               Map<String, Object> testResourcesConfig,
                                               DatabaseDescriptor database) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName, DATASOURCES_PREFIX);
        if (datasourceProperty == null) {
            return Optional.empty();
        }
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            database.serviceDescriptor(),
            context -> context.matchesDatasource(datasourceProperty.datasource()),
            context -> switch (datasourceProperty.property()) {
                case URL -> jdbcUrl(database, context, properties, datasourceProperty.datasource());
                case USERNAME -> username(database, context);
                case PASSWORD -> password(database, context);
                case DRIVER_CLASS_NAME -> database.driverClassName();
                default -> null;
            });
    }

    public static Optional<String> resolveR2dbc(String propertyName,
                                                Map<String, Object> properties,
                                                Map<String, Object> testResourcesConfig,
                                                DatabaseDescriptor database) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName, R2DBC_DATASOURCES_PREFIX);
        if (datasourceProperty == null) {
            return Optional.empty();
        }
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            database.serviceDescriptor(),
            context -> context.matchesDatasource(datasourceProperty.datasource()),
            context -> switch (datasourceProperty.property()) {
                case URL -> r2dbcUrl(database, context, properties, datasourceProperty.datasource());
                case USERNAME -> username(database, context);
                case PASSWORD -> password(database, context);
                default -> null;
            });
    }

    public static Optional<String> resolveHibernateReactive(String propertyName,
                                                            Map<String, Object> properties,
                                                            Map<String, Object> testResourcesConfig,
                                                            DatabaseDescriptor database) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName, "jpa.");
        if (datasourceProperty == null) {
            return Optional.empty();
        }
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            database.serviceDescriptor(),
            context -> context.matchesDatasource(datasourceProperty.datasource()),
            context -> switch (datasourceProperty.property()) {
                case "properties.hibernate.connection.url" -> jdbcUrl(database, context, properties, datasourceProperty.datasource());
                case "properties.hibernate.connection.username" -> username(database, context);
                case "properties.hibernate.connection.password" -> password(database, context);
                default -> null;
            });
    }

    private static String jdbcUrl(DatabaseDescriptor database,
                                  ComposeResolverSupport.ResolutionContext context,
                                  Map<String, Object> requestedProperties,
                                  String datasource) {
        String databaseName = databaseName(database, context, requestedProperties, datasource);
        return switch (database.serviceType()) {
            case "mssql" -> database.jdbcPrefix() + "://" + context.hostPort() + ";databaseName=" + databaseName + ";encrypt=false";
            case "oracle" -> database.jdbcPrefix() + ":@" + context.hostPort() + "/" + databaseName;
            default -> database.jdbcPrefix() + "://" + context.hostPort() + "/" + databaseName;
        };
    }

    private static String r2dbcUrl(DatabaseDescriptor database,
                                   ComposeResolverSupport.ResolutionContext context,
                                   Map<String, Object> requestedProperties,
                                   String datasource) {
        return "r2dbc:" + database.r2dbcDriver() + "://" + context.hostPort() + "/" + databaseName(database, context, requestedProperties, datasource);
    }

    private static String username(DatabaseDescriptor database, ComposeResolverSupport.ResolutionContext context) {
        return context.labelOrEnvironment(ComposeResolverSupport.USERNAME_LABEL, database.userEnv(), database.defaultUsername());
    }

    private static String password(DatabaseDescriptor database, ComposeResolverSupport.ResolutionContext context) {
        return context.labelOrEnvironment(ComposeResolverSupport.PASSWORD_LABEL, database.passwordEnv(), database.defaultPassword());
    }

    private static String databaseName(DatabaseDescriptor database,
                                       ComposeResolverSupport.ResolutionContext context,
                                       Map<String, Object> requestedProperties,
                                       String datasource) {
        Object requestedDbName = requestedProperties.get(DATASOURCES_PREFIX + datasource + "." + DB_NAME);
        if (requestedDbName == null) {
            requestedDbName = requestedProperties.get(R2DBC_DATASOURCES_PREFIX + datasource + "." + DB_NAME);
        }
        if (requestedDbName != null) {
            return String.valueOf(requestedDbName);
        }
        String defaultDatabase = database.defaultDatabase() == null ? username(database, context) : database.defaultDatabase();
        return context.labelOrEnvironment(ComposeResolverSupport.DATABASE_LABEL, database.databaseEnv(), defaultDatabase);
    }

    /**
     * Compose metadata needed by database providers.
     *
     * @param serviceType The canonical Compose service type.
     * @param aliases Additional labels or image fragments that identify the service.
     * @param port The container port that must be published.
     * @param jdbcPrefix The JDBC URL prefix.
     * @param r2dbcDriver The R2DBC driver name.
     * @param driverClassName The JDBC driver class name.
     * @param userEnv The environment variable that contains the username.
     * @param passwordEnv The environment variable that contains the password.
     * @param databaseEnv The environment variable that contains the database name.
     * @param defaultUsername The default username.
     * @param defaultPassword The default password.
     * @param defaultDatabase The default database name.
     */
    public record DatabaseDescriptor(
        String serviceType,
        Set<String> aliases,
        int port,
        String jdbcPrefix,
        String r2dbcDriver,
        String driverClassName,
        String userEnv,
        String passwordEnv,
        String databaseEnv,
        String defaultUsername,
        String defaultPassword,
        String defaultDatabase
    ) {
        ComposeResolverSupport.ServiceDescriptor serviceDescriptor() {
            return new ComposeResolverSupport.ServiceDescriptor(serviceType, aliases, port);
        }
    }

    private record DatasourceProperty(String datasource, String property) {
        static DatasourceProperty parse(String propertyName, String prefix) {
            if (!propertyName.startsWith(prefix)) {
                return null;
            }
            String remainder = propertyName.substring(prefix.length());
            int separator = remainder.indexOf('.');
            if (separator < 1 || separator == remainder.length() - 1) {
                return null;
            }
            return new DatasourceProperty(remainder.substring(0, separator), remainder.substring(separator + 1));
        }
    }
}
