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
package io.micronaut.testresources.r2dbc.postgres;

import io.micronaut.testresources.r2dbc.core.AbstractR2DBCTestResourceProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.postgresql.PostgreSQLR2DBCDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider for reactive PostgreSQL.
 */
public class R2DBCPostgreSQLTestResourceProvider extends AbstractR2DBCTestResourceProvider<PostgreSQLContainer> {
    private static final List<String> SUPPORTED_DB_TYPES = Collections.unmodifiableList(
        Arrays.asList("postgresql", "postgres", "pg")
    );
    public static final String DISPLAY_NAME = "PostgreSQL (R2DBC)";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return "postgres";
    }

    @Override
    protected String getDefaultImageName() {
        return "postgres:18.3";
    }

    @Override
    protected List<String> getDbTypes() {
        return SUPPORTED_DB_TYPES;
    }

    @Override
    protected boolean supportsMultipleDatabases() {
        return true;
    }

    @Override
    protected void createAdditionalDatabase(PostgreSQLContainer container, String databaseName) {
        executeInContainer("Failed to create PostgreSQL database '" + databaseName + "'", () ->
            container.execInContainer(
                "psql",
                "-v",
                "ON_ERROR_STOP=1",
                "-U",
                container.getUsername(),
                "-d",
                container.getDatabaseName(),
                "-c",
                "CREATE DATABASE " + quoteDatabaseName(databaseName)
            )
        );
    }

    @Override
    protected Optional<String> extractDefaultDatabaseName(PostgreSQLContainer container) {
        return Optional.of(container.getDatabaseName());
    }

    @Override
    protected Optional<ConnectionFactoryOptions> extractOptions(GenericContainer<?> container) {
        if (container instanceof PostgreSQLContainer) {
            return Optional.of(PostgreSQLR2DBCDatabaseContainer.getOptions((PostgreSQLContainer) container));
        }
        return Optional.empty();
    }

    @Override
    protected PostgreSQLContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new PostgreSQLContainer(imageName);
    }

    private static String quoteDatabaseName(String databaseName) {
        return "\"" + databaseName.replace("\"", "\"\"") + "\"";
    }
}
