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
package io.micronaut.testresources.postgres;

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves PostgreSQL properties from Docker Compose services.
 */
public final class PostgreSQLComposeTestResourceProvider extends PostgreSQLTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor POSTGRES =
        new ComposeResolverSupport.ServiceDescriptor("postgres", List.of("postgresql", "pg"), 5432);

    @Override
    public String getDisplayName() {
        return "Docker Compose PostgreSQL";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        String datasource = datasourceNameFrom(propertyName);
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            POSTGRES,
            context -> context.matchesDatasource(datasource),
            context -> switch (datasourcePropertyFrom(propertyName)) {
                case "url" -> jdbcUrl(propertyName, properties, context);
                case "username" -> username(context);
                case "password" -> password(context);
                case "driver-class-name" -> "org.postgresql.Driver";
                default -> null;
            });
    }

    private String jdbcUrl(String propertyName,
                           Map<String, Object> properties,
                           ComposeResolverSupport.ResolutionContext context) {
        String databaseName = findRequestedDatabaseName(propertyName, properties)
            .orElseGet(() -> context.labelOrEnvironment(ComposeResolverSupport.DATABASE_LABEL, "POSTGRES_DB", "postgres"));
        return "jdbc:postgresql://" + context.hostPort() + "/" + databaseName;
    }

    private static String username(ComposeResolverSupport.ResolutionContext context) {
        return context.labelOrEnvironment(ComposeResolverSupport.USERNAME_LABEL, "POSTGRES_USER", "postgres");
    }

    private static String password(ComposeResolverSupport.ResolutionContext context) {
        return context.labelOrEnvironment(ComposeResolverSupport.PASSWORD_LABEL, "POSTGRES_PASSWORD", "postgres");
    }
}
