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
import io.micronaut.testresources.r2dbc.core.R2dbcSupport;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.micronaut.testresources.r2dbc.core.R2dbcSupport.datasourceNameFrom;
import static io.micronaut.testresources.r2dbc.core.R2dbcSupport.r2dbDatasourceExpressionOf;

/**
 * A test resource provider which will spawn an Oracle Free reactive test container.
 */
public class R2DBCOracleFreeTestResourceProvider extends AbstractR2DBCTestResourceProvider<OracleContainer> {

    public static final String DISPLAY_NAME = "Oracle Database (R2DBC)";
    private static final String R2DBC_ORACLE_DRIVER = "oracle";
    private static final String OCID = "ocid";

    @Override
    public List<String> getRequiredProperties(String expression) {
        List<String> requiredProperties = super.getRequiredProperties(expression);
        String baseDatasourceExpression = R2dbcSupport.removeR2dbPrefixFrom(expression);
        String datasource = datasourceNameFrom(baseDatasourceExpression);
        return Stream.concat(
            requiredProperties.stream(),
            Stream.of(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, OCID))
        ).toList();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        boolean shouldAnswer = super.shouldAnswer(propertyName, requestedProperties, testResourcesConfig);
        if (shouldAnswer) {
            String baseDatasourceExpression = R2dbcSupport.removeR2dbPrefixFrom(propertyName);
            String datasource = datasourceNameFrom(baseDatasourceExpression);
            String ocid = stringOrNull(requestedProperties.get(r2dbDatasourceExpressionOf(datasource, OCID)));
            if (ocid != null) {
                // https://github.com/micronaut-projects/micronaut-test-resources/issues/104
                // if the OCID property is set, then we're in a production environment
                return false;
            }
        }
        return shouldAnswer;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return R2DBC_ORACLE_DRIVER;
    }

    @Override
    protected String getDefaultImageName() {
        return "gvenzl/oracle-free:slim-faststart";
    }

    @Override
    protected Optional<ConnectionFactoryOptions> extractOptions(GenericContainer<?> container) {
        if (container instanceof OracleContainer oracle) {
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
    protected OracleContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new OracleContainer(imageName);
    }

}
