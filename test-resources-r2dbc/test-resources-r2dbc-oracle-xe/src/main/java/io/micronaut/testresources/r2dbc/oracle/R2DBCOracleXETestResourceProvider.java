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
package io.micronaut.testresources.r2dbc.oracle;

import io.micronaut.testresources.r2dbc.core.AbstractR2DBCTestResourceProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn an Oracle XE reactive test container.
 */
public class R2DBCOracleXETestResourceProvider extends AbstractR2DBCTestResourceProvider<OracleContainer> {

    private static final String R2DBC_ORACLE_DRIVER = "oracle";

    @Override
    protected String getSimpleName() {
        return R2DBC_ORACLE_DRIVER;
    }

    @Override
    protected String getDefaultImageName() {
        return "gvenzl/oracle-xe";
    }

    @Override
    protected Optional<ConnectionFactoryOptions> extractOptions(GenericContainer<?> container) {
        if (container instanceof OracleContainer) {
            OracleContainer oracle = (OracleContainer) container;
            ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.USER, oracle.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, oracle.getPassword())
                .option(ConnectionFactoryOptions.HOST, oracle.getHost())
                .option(ConnectionFactoryOptions.PORT, oracle.getOraclePort())
                .option(ConnectionFactoryOptions.DATABASE, oracle.getDatabaseName())
                .option(ConnectionFactoryOptions.DRIVER, R2DBC_ORACLE_DRIVER)
                .build();
            return Optional.of(options);
        }
        return Optional.empty();
    }

    @Override
    protected OracleContainer createContainer(DockerImageName imageName, Map<String, Object> properties) {
        return new OracleContainer(imageName);
    }

}
