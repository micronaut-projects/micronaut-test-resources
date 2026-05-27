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
package io.micronaut.testresources.r2dbc.core;

import io.micronaut.testresources.core.Scope;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import io.micronaut.testresources.testcontainers.TestContainers;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for R2DBC test resources. Unlike JDBC test resources, this
 * resolver is capable of reusing an existing JDBC test resources and
 * expose it via R2DBC: this can be useful for Flyway database migrations
 * which work over JDBC for example. For this to work, a datasource of
 * the same name must exist, in which case it would be resolved first.
 *
 * If no such datasource exists, then a new container will be created.
 *
 * @param <T> the container type
 */
public abstract class AbstractR2DBCTestResourceProvider<T extends GenericContainer<? extends T>> extends AbstractTestContainersProvider<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractR2DBCTestResourceProvider.class);

    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final List<String> RESOLVABLE_KEYS = Arrays.asList(
        URL,
        USERNAME,
        PASSWORD
    );

    /**
     * Returns the list of db-types supported by this provider.
     * @return the list of db types
     */
    protected List<String> getDbTypes() {
        return Collections.singletonList(getSimpleName());
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return R2dbcSupport.REQUIRED_PROPERTY_ENTRIES_LIST;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return R2dbcSupport.findRequiredProperties(expression);
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return R2dbcSupport.findResolvableProperties(propertyEntries, RESOLVABLE_KEYS);
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        if (!propertyName.startsWith(R2dbcSupport.R2DBC_PREFIX)) {
            return false;
        }
        String baseDatasourceExpression = R2dbcSupport.removeR2dbPrefixFrom(propertyName);
        String datasource = R2dbcSupport.datasourceNameFrom(baseDatasourceExpression);
        String type = stringOrNull(requestedProperties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.TYPE)));
        if (type != null && getDbTypes().stream().anyMatch(type::equalsIgnoreCase)) {
            return true;
        }
        String driver = stringOrNull(requestedProperties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.DRIVER)));
        if (driver != null && driver.toLowerCase(Locale.US).contains(getSimpleName())) {
            return true;
        }
        String dialect = stringOrNull(requestedProperties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.DIALECT)));
        if (dialect != null && dialect.equalsIgnoreCase(getSimpleName())) {
            return true;
        }
        return false;
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        String name = R2dbcSupport.removeR2dbPrefixFrom(propertyName);
        if (properties.containsKey(name)) {
            return resolveUsingExistingContainer(propertyName, properties, testResourcesConfig, name);
        }
        return Optional.empty();
    }

    @Override
    protected final Optional<String> resolveProperty(String expression, T container) {
        return resolveProperty(expression, container, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    protected final Optional<String> resolveProperty(String expression,
                                                    T container,
                                                    Map<String, Object> properties,
                                                    Map<String, Object> testResourcesConfig) {
        Optional<ConnectionFactoryOptions> options = extractOptions(container);
        if (options.isPresent()) {
            String propertyName = expression.substring(expression.lastIndexOf(".") + 1);
            return Optional.ofNullable(resolveFromConnectionOptions(expression, propertyName, options.get(), properties));
        }
        return Optional.empty();
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
            .<Map<String, Object>>map(name -> Map.of(R2dbcSupport.RESOURCE_NAME, name))
            .orElseGet(() -> super.getContainerQuery(propertyName, properties, testResourcesConfig)) : super.getContainerQuery(propertyName, properties, testResourcesConfig);
    }

    @Override
    protected void prepareContainer(String propertyName,
                                    T container,
                                    Map<String, Object> properties,
                                    Map<String, Object> testResourcesConfig) {
        if (!propertyName.endsWith("." + URL)) {
            return;
        }
        findRequestedDatabaseName(propertyName, properties)
            .filter(databaseName -> supportsMultipleDatabases())
            .filter(databaseName -> extractDefaultDatabaseName(container).map(defaultDatabase -> !defaultDatabase.equals(databaseName)).orElse(true))
            .ifPresent(databaseName -> TestContainers.createDatabaseIfMissing(
                container,
                databaseName,
                () -> createAdditionalDatabase(container, databaseName)
            ));
    }

    private Optional<String> resolveUsingExistingContainer(String propertyName,
                                                           Map<String, Object> properties,
                                                           Map<String, Object> testResourcesConfig,
                                                           String name) {
        // Look for a container with JDBC
        LOGGER.debug("Resolving property: {} with properties {}", propertyName, properties);
        List<GenericContainer<?>> containers = TestContainers.findByRequestedProperty(Scope.from(properties), name);
        LOGGER.debug("Found containers providing {} : {}", name, containers);
        if (containers.size() > 1) {
            LOGGER.warn("More than one container provides {}. Will use the first one.", name);
        }
        return containers.stream()
            .findFirst()
            .flatMap(container -> prepareAndExtractOptions(propertyName, properties, testResourcesConfig, container))
            .map(options -> resolveFromConnectionOptions(propertyName, propertyName, options, properties));
    }

    private Optional<ConnectionFactoryOptions> prepareAndExtractOptions(String propertyName,
                                                                        Map<String, Object> properties,
                                                                        Map<String, Object> testResourcesConfig,
                                                                        GenericContainer<?> container) {
        Optional<ConnectionFactoryOptions> options = extractOptions(container);
        if (options.isEmpty()) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        T typedContainer = (T) container;
        prepareContainer(propertyName, typedContainer, properties, testResourcesConfig);
        return options;
    }

    private @Nullable String resolveFromConnectionOptions(String expression,
                                                          String propertyName,
                                                          ConnectionFactoryOptions options,
                                                          Map<String, Object> properties) {
        String property = propertyName.substring(propertyName.lastIndexOf(".") + 1);
        switch (property) {
            case URL:
                Object db = findRequestedDatabaseName(expression, properties)
                    .filter(databaseName -> supportsMultipleDatabases())
                    .orElseGet(() -> options.getValue(ConnectionFactoryOptions.DATABASE) == null ? null : String.valueOf(options.getValue(ConnectionFactoryOptions.DATABASE)));
                String url;
                if (db != null) {
                    url = String.format(
                        "r2dbc:%s://%s:%s/%s",
                        options.getValue(ConnectionFactoryOptions.DRIVER),
                        options.getValue(ConnectionFactoryOptions.HOST),
                        options.getValue(ConnectionFactoryOptions.PORT),
                        db
                    );
                } else {
                    url = String.format(
                        "r2dbc:%s://%s:%s",
                        options.getValue(ConnectionFactoryOptions.DRIVER),
                        options.getValue(ConnectionFactoryOptions.HOST),
                        options.getValue(ConnectionFactoryOptions.PORT)
                    );
                }
                LOGGER.debug("Resolved property: {} with value: {}", propertyName, url);
                return url;
            case USERNAME:
                return String.valueOf(options.getValue(ConnectionFactoryOptions.USER));
            case PASSWORD:
                return String.valueOf(options.getValue(ConnectionFactoryOptions.PASSWORD));
            default:
        }
        return null;
    }

    protected abstract Optional<ConnectionFactoryOptions> extractOptions(GenericContainer<?> container);

    /**
     * Indicates whether this provider can create additional logical databases inside the
     * same running container. Subclasses overriding this method must also implement the
     * matching database-creation hooks for the same container type.
     *
     * @return {@code true} when additional logical databases are supported
     */
    protected boolean supportsMultipleDatabases() {
        return false;
    }

    /**
     * Creates an additional logical database inside an already started container.
     * This is only invoked when {@link #supportsMultipleDatabases()} returns {@code true},
     * so subclasses should override both hooks together.
     *
     * @param container the running container
     * @param databaseName the database to create
     */
    protected void createAdditionalDatabase(T container, String databaseName) {
        throw new UnsupportedOperationException("Additional database creation is not supported for " + getSimpleName());
    }

    /**
     * Extracts the default logical database name already configured on the running
     * container. Subclasses may override when the provider can compare requested
     * databases against a provider-specific default.
     *
     * @param container the running container
     * @return the default logical database name, if one can be derived
     */
    protected Optional<String> extractDefaultDatabaseName(T container) {
        return Optional.empty();
    }

    private Optional<String> findSharedResourceName(String propertyName, Map<String, Object> properties) {
        if (!propertyName.startsWith(R2dbcSupport.R2DBC_PREFIX)) {
            return Optional.empty();
        }
        String datasource = R2dbcSupport.datasourceNameFrom(R2dbcSupport.removeR2dbPrefixFrom(propertyName));
        return Optional.ofNullable(stringOrNull(properties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.RESOURCE_NAME))));
    }

    /**
     * Extracts the logical database name requested by the caller from the R2DBC
     * datasource properties. Subclasses may override when the provider supports
     * alternative configuration keys for database selection.
     *
     * @param propertyName the property being resolved
     * @param properties the resolved properties for the request
     * @return the requested logical database name, if present
     */
    protected Optional<String> findRequestedDatabaseName(String propertyName, Map<String, Object> properties) {
        if (!propertyName.startsWith(R2dbcSupport.R2DBC_PREFIX)) {
            return Optional.empty();
        }
        String datasource = R2dbcSupport.datasourceNameFrom(R2dbcSupport.removeR2dbPrefixFrom(propertyName));
        String r2dbcDatabaseName = stringOrNull(properties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.DB_NAME)));
        if (r2dbcDatabaseName != null) {
            return Optional.of(r2dbcDatabaseName);
        }
        return Optional.ofNullable(stringOrNull(properties.get(R2dbcSupport.datasourceExpressionOf(datasource, R2dbcSupport.DB_NAME))));
    }

    private boolean supportsSharedContainerReuse(String propertyName, Map<String, Object> properties) {
        return findSharedResourceName(propertyName, properties).isPresent();
    }

}
