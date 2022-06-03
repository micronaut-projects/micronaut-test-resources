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
package io.micronaut.testresources.jdbc;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Superclass for JDBC test containers providers.
 *
 * @param <T> the type of the container
 */
public abstract class AbstractJdbcTestResourceProvider<T extends JdbcDatabaseContainer<? extends T>> extends AbstractTestContainersProvider<T> {
    private static final String PREFIX = "datasources";
    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DIALECT = "dialect";
    private static final String DRIVER = "driverClassName";

    private static final List<String> SUPPORTED_LIST = Collections.unmodifiableList(
        Arrays.asList(URL, USERNAME, PASSWORD)
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        Collection<String> datasources = propertyEntries.getOrDefault(PREFIX, Collections.emptyList());
        return datasources.stream()
            .flatMap(ds -> SUPPORTED_LIST.stream().map(p -> PREFIX + "." + ds + "." + p))
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Collections.singletonList(PREFIX);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        String datasource = datasourceNameFrom(expression);
        return Stream.concat(super.getRequiredProperties(expression).stream(), Stream.of(
                datasourceExpressionOf(datasource, DIALECT),
                datasourceExpressionOf(datasource, DRIVER)))
            .collect(Collectors.toList());
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties) {
        String datasource = datasourceNameFrom(propertyName);
        String dialect = String.valueOf(properties.get(datasourceExpressionOf(datasource, DIALECT)));
        if (dialect != null) {
            return dialect.equalsIgnoreCase(getSimpleName());
        }
        String driver = String.valueOf(properties.get(datasourceExpressionOf(datasource, DRIVER)));
        if (driver != null) {
            return driver.toLowerCase(Locale.US).contains(getSimpleName());
        }
        return false;
    }

    @Override
    protected Optional<String> resolveProperty(String expression, T container) {
        String value;
        switch (datasourcePropertyFrom(expression)) {
            case URL:
                value = container.getJdbcUrl();
                break;
            case USERNAME:
                value = container.getUsername();
                break;
            case PASSWORD:
                value = container.getPassword();
                break;
            default:
                value = resolveDbSpecificProperty(expression, container);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Given the started container, resolves properties which are specific to
     * a particular JDBC implementation.
     *
     * @param propertyName the property to resolve
     * @param container the started container
     * @return the resolved property, or null if not resolvable
     */
    protected String resolveDbSpecificProperty(String propertyName, JdbcDatabaseContainer<?> container) {
        return null;
    }

    protected static String datasourceNameFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(0, remainder.indexOf("."));
    }

    protected static String datasourcePropertyFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(1 + remainder.indexOf("."));
    }

    protected static String datasourceExpressionOf(String datasource, String property) {
        return PREFIX + "." + datasource + "." + property;
    }
}
