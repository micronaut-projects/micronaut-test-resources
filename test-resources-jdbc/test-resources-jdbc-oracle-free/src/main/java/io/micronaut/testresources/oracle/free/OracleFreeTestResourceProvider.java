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
package io.micronaut.testresources.oracle.free;

import io.micronaut.testresources.jdbc.AbstractJdbcTestResourceProvider;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A test resource provider which will spawn an Oracle Free test container.
 *
 * @since 2.4.0
 */
public class OracleFreeTestResourceProvider extends AbstractJdbcTestResourceProvider<OracleContainer> {
    public static final String DISPLAY_NAME = "Oracle Database";
    private static final String OCID = "ocid";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (!isDatasourceExpression(expression)) {
            return Collections.emptyList();
        }
        List<String> requiredProperties = super.getRequiredProperties(expression);
        String datasource = datasourceNameFrom(expression);
        return Stream.concat(
            requiredProperties.stream(),
            Stream.of(datasourceExpressionOf(datasource, OCID))
        ).toList();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        boolean shouldAnswer = super.shouldAnswer(propertyName, requestedProperties, testResourcesConfig);
        if (shouldAnswer) {
            String datasource = datasourceNameFrom(propertyName);
            String ocid = stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, OCID)));
            if (ocid != null) {
                // https://github.com/micronaut-projects/micronaut-test-resources/issues/104
                // if the OCID property is set, then we're in a production environment
                return false;
            }
        }
        return shouldAnswer;
    }

    @Override
    protected String getSimpleName() {
        return "oracle";
    }

    @Override
    protected String getDefaultImageName() {
        return "gvenzl/oracle-free:slim-faststart";
    }

    @Override
    protected OracleContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new OracleContainer(imageName);
    }

}
