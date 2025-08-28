/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.testresources.oracle.testpilot;

import io.micronaut.testresources.core.ToggableTestResourcesResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A test resource provider which will integrate with Oracle Test Pilot for Third-Party Software.
 *
 * @author Loïc Lefèvre
 * @since 2.9.0
 */
public class OracleTestPilotTestResourceProvider implements ToggableTestResourcesResolver {
    public static final String DISPLAY_NAME = "Oracle Test Pilot Database";

    public static final String PREFIX = "datasources";

    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DIALECT = "dialect";
    private static final String DRIVER = "driver-class-name";
    private static final String DB_TYPE = "db-type";
    private static final String SCHEMA_GENERATE = "schema-generate";

    private static final List<String> SUPPORTED_LIST = List.of(URL, USERNAME, PASSWORD, DRIVER, DIALECT, SCHEMA_GENERATE);

    private Function<String, String> propertySupplier = System::getenv;

    public OracleTestPilotTestResourceProvider() {
    }

    public OracleTestPilotTestResourceProvider(final Function<String, String> propertySupplier) {
        this.propertySupplier = propertySupplier;
    }

    @Override
    public final String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public final String getName() {
        return "oracle-test-pilot";
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Collections.singletonList(PREFIX);
    }

    protected static boolean isDatasourceExpression(String expression) {
        return expression.startsWith(PREFIX);
    }

    protected static String datasourceNameFrom(final String expression) {
        final String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(0, remainder.indexOf('.'));
    }

    protected static String datasourceExpressionOf(final String datasource, final String property) {
        return String.format("%s.%s.%s", PREFIX, datasource, property);
    }

    @Override
    public final List<String> getRequiredProperties(final String expression) {
        if (!isDatasourceExpression(expression)) {
            return Collections.emptyList();
        }
        final String datasource = datasourceNameFrom(expression);
        return Stream.of(
            datasourceExpressionOf(datasource, DB_TYPE),
            datasourceExpressionOf(datasource, DIALECT)
        ).toList();
    }

    @Override
    public final List<String> getResolvableProperties(final Map<String, Collection<String>> propertyEntries, final Map<String, Object> testResourcesConfig) {
        Collection<String> datasources = propertyEntries.getOrDefault(PREFIX, Collections.emptyList());
        return datasources.stream()
            .flatMap(ds -> SUPPORTED_LIST.stream().map(p -> String.format("%s.%s.%s", PREFIX, ds, p)))
            .toList();
    }

    protected static String datasourcePropertyFrom(final String expression) {
        final String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(1 + remainder.indexOf('.'));
    }

    @Override
    public final Optional<String> resolve(final String propertyName, final Map<String, Object> properties, final Map<String, Object> testResourcesConfig) {
        final String value = switch (datasourcePropertyFrom(propertyName)) {
            case URL ->
                "jdbc:oracle:thin:@" + propertySupplier.apply("TESTPILOT_CONNECTION_STRING_SUFFIX").replace("\"", "");
            case USERNAME -> propertySupplier.apply("TESTPILOT_USERNAME");
            case PASSWORD -> propertySupplier.apply("TESTPILOT_PASSWORD");
            case DRIVER -> "oracle.jdbc.OracleDriver";
            case DIALECT -> "ORACLE";
            case SCHEMA_GENERATE -> "CREATE_DROP";
            default -> null;
        };
        return Optional.ofNullable(value);
    }
}
