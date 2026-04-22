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
package io.micronaut.testresources.r2dbc.mysql;

import io.micronaut.testresources.r2dbc.core.AbstractR2DBCTestResourceProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.mysql.MySQLR2DBCDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider for reactive MySQL.
 */
public class R2DBCMySQLTestResourceProvider extends AbstractR2DBCTestResourceProvider<MySQLContainer> {
    public static final String DISPLAY_NAME = "MySQL (R2DBC)";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return "mysql";
    }

    @Override
    protected String getDefaultImageName() {
        return "mysql:8.4.5";
    }

    @Override
    protected boolean supportsMultipleDatabases() {
        return true;
    }

    @Override
    protected void createAdditionalDatabase(MySQLContainer container, String databaseName) {
        try {
            var result = container.execInContainer(
                "mysql",
                "-h127.0.0.1",
                "-u",
                container.getUsername(),
                "-p" + container.getPassword(),
                "-e",
                "CREATE DATABASE " + quoteDatabaseName(databaseName)
            );
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(result.getStderr());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create MySQL database '" + databaseName + "'", e);
        }
    }

    @Override
    protected Optional<String> extractDefaultDatabaseName(MySQLContainer container) {
        return Optional.of(container.getDatabaseName());
    }

    @Override
    protected Optional<ConnectionFactoryOptions> extractOptions(GenericContainer<?> container) {
        if (container instanceof MySQLContainer) {
            return Optional.of(MySQLR2DBCDatabaseContainer.getOptions((MySQLContainer) container));
        }
        return Optional.empty();
    }

    @Override
    protected MySQLContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new MySQLContainer(imageName);
    }

    private static String quoteDatabaseName(String databaseName) {
        return "`" + databaseName.replace("`", "``") + "`";
    }
}
