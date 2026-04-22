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
import io.micronaut.testresources.testcontainers.TestContainers;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Superclass for JDBC test containers providers.
 *
 * @param <T> the type of the container
 */
public abstract class AbstractJdbcTestResourceProvider<T extends JdbcDatabaseContainer<? extends T>> extends AbstractTestContainersProvider<T> {
    public static final String PREFIX = "datasources";
    public static final String RESOURCE_NAME = "test-resources.resource-name";
    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DIALECT = "dialect";
    private static final String DRIVER = "driver-class-name";
    private static final String DB_NAME = "db-name";
    private static final String INIT_SCRIPT = "init-script-path";

    private static final String TYPE = "db-type";

    private static final List<String> SUPPORTED_LIST = List.of(URL, USERNAME, PASSWORD, DRIVER);

    /**
     * Returns the list of db-types supported by this provider.
     * @return the list of db types
     */
    protected List<String> getDbTypes() {
        return Collections.singletonList(getSimpleName());
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        Collection<String> datasources = propertyEntries.getOrDefault(PREFIX, Collections.emptyList());
        return datasources.stream()
            .flatMap(ds -> SUPPORTED_LIST.stream().map(p -> PREFIX + "." + ds + "." + p))
            .toList();
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Collections.singletonList(PREFIX);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (!isDatasourceExpression(expression)) {
            return Collections.emptyList();
        }
        String datasource = datasourceNameFrom(expression);
        return Stream.of(
                datasourceExpressionOf(datasource, TYPE),
                datasourceExpressionOf(datasource, DIALECT),
                datasourceExpressionOf(datasource, DB_NAME),
                datasourceExpressionOf(datasource, RESOURCE_NAME)
            ).toList();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        if (!propertyName.startsWith(PREFIX)) {
            return false;
        }
        String datasource = datasourceNameFrom(propertyName);
        String type = stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, TYPE)));
        if (type != null) {
            return getDbTypes().stream().anyMatch(type::equalsIgnoreCase);
        }
        String dialect = stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, DIALECT)));
        return dialect != null && dialect.equalsIgnoreCase(getSimpleName());
    }

    @Override
    protected Optional<String> resolveProperty(String expression, T container) {
        return resolveProperty(expression, container, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    protected Optional<String> resolveProperty(String expression,
                                               T container,
                                               Map<String, Object> properties,
                                               Map<String, Object> testResourcesConfig) {
        String value = switch (datasourcePropertyFrom(expression)) {
            case URL -> resolveJdbcUrl(expression, container, properties);
            case USERNAME -> container.getUsername();
            case PASSWORD -> container.getPassword();
            case DRIVER -> container.getDriverClassName();
            default -> resolveDbSpecificProperty(expression, container, properties, testResourcesConfig);
        };
        return Optional.ofNullable(value);
    }

    @Override
    protected String getContainerOwnerKey(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if (supportsSharedContainerReuse(propertyName, properties)) {
            return getSimpleName();
        }
        return super.getContainerOwnerKey(propertyName, properties, testResourcesConfig);
    }

    @Override
    protected Map<String, Object> getContainerQuery(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        return supportsSharedContainerReuse(propertyName, properties) ? findSharedResourceName(propertyName, properties)
            .<Map<String, Object>>map(name -> Map.of(RESOURCE_NAME, name))
            .orElseGet(() -> super.getContainerQuery(propertyName, properties, testResourcesConfig)) : super.getContainerQuery(propertyName, properties, testResourcesConfig);
    }

    @Override
    protected void prepareContainer(String propertyName,
                                    T container,
                                    Map<String, Object> properties,
                                    Map<String, Object> testResourcesConfig) {
        findRequestedDatabaseName(propertyName, properties)
            .filter(databaseName -> supportsMultipleDatabases())
            .filter(databaseName -> !databaseName.equals(container.getDatabaseName()))
            .ifPresent(databaseName -> {
                synchronized (container) {
                    if (!TestContainers.hasDatabase(container, databaseName)) {
                        createAdditionalDatabase(container, databaseName);
                        TestContainers.rememberDatabase(container, databaseName);
                    }
                }
            });
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

    protected String resolveDbSpecificProperty(String propertyName,
                                               JdbcDatabaseContainer<?> container,
                                               Map<String, Object> properties,
                                               Map<String, Object> testResourcesConfig) {
        return resolveDbSpecificProperty(propertyName, container);
    }

    @Override
    protected void configureContainer(T container, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        super.configureContainer(container, properties, testResourcesConfig);
        ifPresent(INIT_SCRIPT, testResourcesConfig, container::withInitScript);
        ifPresent(USERNAME, testResourcesConfig, container::withUsername);
        ifPresent(PASSWORD, testResourcesConfig, container::withPassword);
        ifPresent(DB_NAME, testResourcesConfig, container::withDatabaseName);
    }

    private void ifPresent(String key, Map<String, Object> testResourcesConfig, Consumer<String> consumer) {
        Object value = testResourcesConfig.get("containers." + getSimpleName() + "." + key);
        if (value != null) {
            consumer.accept(value.toString());
        }
    }

    protected static boolean isDatasourceExpression(String expression) {
        return expression.startsWith(PREFIX);
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

    protected boolean supportsMultipleDatabases() {
        return false;
    }

    protected void createAdditionalDatabase(T container, String databaseName) {
        throw new UnsupportedOperationException("Additional database creation is not supported for " + getSimpleName());
    }

    protected String resolveJdbcUrl(String expression, T container, Map<String, Object> properties) {
        return findRequestedDatabaseName(expression, properties)
            .filter(databaseName -> supportsMultipleDatabases())
            .map(databaseName -> jdbcUrlFor(container, databaseName))
            .orElseGet(container::getJdbcUrl);
    }

    protected String jdbcUrlFor(T container, String databaseName) {
        return container.getJdbcUrl();
    }

    protected Optional<String> findRequestedDatabaseName(String propertyName, Map<String, Object> requestedProperties) {
        if (!isDatasourceExpression(propertyName)) {
            return Optional.empty();
        }
        String datasource = datasourceNameFrom(propertyName);
        return Optional.ofNullable(stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, DB_NAME))));
    }

    private Optional<String> findSharedResourceName(String propertyName, Map<String, Object> requestedProperties) {
        if (!isDatasourceExpression(propertyName)) {
            return Optional.empty();
        }
        String datasource = datasourceNameFrom(propertyName);
        return Optional.ofNullable(stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, RESOURCE_NAME))));
    }

    private boolean supportsSharedContainerReuse(String propertyName, Map<String, Object> properties) {
        return findSharedResourceName(propertyName, properties).isPresent();
    }
}
