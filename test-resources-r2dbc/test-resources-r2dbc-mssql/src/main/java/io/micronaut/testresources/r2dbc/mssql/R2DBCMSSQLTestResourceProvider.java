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
package io.micronaut.testresources.r2dbc.mssql;

import io.micronaut.testresources.mssql.MSSQLTestResourceProvider;
import io.micronaut.testresources.r2dbc.core.AbstractR2DBCTestResourceProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MSSQLR2DBCDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider for reactive PostgreSQL.
 */
public class R2DBCMSSQLTestResourceProvider extends AbstractR2DBCTestResourceProvider<MSSQLServerContainer<?>> {

    @Override
    protected String getSimpleName() {
        return "mssql";
    }

    @Override
    protected String getDefaultImageName() {
        return MSSQLTestResourceProvider.DEFAULT_IMAGE_NAME;
    }

    @Override
    protected Optional<ConnectionFactoryOptions> extractOptions(GenericContainer<?> container) {
        if (container instanceof MSSQLServerContainer) {
            MSSQLServerContainer<?> mssql = (MSSQLServerContainer<?>) container;
            return Optional.of(MSSQLR2DBCDatabaseContainer.getOptions(mssql));
        }
        return Optional.empty();
    }

    @Override
    protected MSSQLServerContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return MSSQLTestResourceProvider.createMSSQLContainer(imageName, getSimpleName(), testResourcesConfiguration);
    }

}
