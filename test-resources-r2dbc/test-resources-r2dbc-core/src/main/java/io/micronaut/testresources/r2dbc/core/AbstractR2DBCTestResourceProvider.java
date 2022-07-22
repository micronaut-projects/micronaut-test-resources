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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.Collection;
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
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        if (!propertyName.startsWith(R2dbcSupport.R2DBC_PREFIX)) {
            return false;
        }
        String baseDatasourceExpression = R2dbcSupport.removeR2dbPrefixFrom(propertyName);
        String datasource = R2dbcSupport.datasourceNameFrom(baseDatasourceExpression);
        String type = stringOrNull(requestedProperties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.TYPE)));
        if (type != null && type.equalsIgnoreCase(getSimpleName())) {
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
    protected Optional<String> resolveWithoutContainer(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        String name = R2dbcSupport.removeR2dbPrefixFrom(propertyName);
        if (properties.containsKey(name)) {
            return resolveUsingExistingContainer(propertyName, properties, name);
        }
        return Optional.empty();
    }

    @Override
    protected final Optional<String> resolveProperty(String expression, T container) {
        Optional<ConnectionFactoryOptions> options = extractOptions(container);
        if (options.isPresent()) {
            String propertyName = expression.substring(expression.lastIndexOf(".") + 1);
            return Optional.ofNullable(resolveFromConnectionOptions(propertyName, options.get()));
        }
        return Optional.empty();
    }

    private Optional<String> resolveUsingExistingContainer(String propertyName, Map<String, Object> properties, String name) {
        // Look for a container with JDBC
        LOGGER.debug("Resolving property: {} with properties {}", propertyName, properties);
        List<GenericContainer<?>> containers = TestContainers.findByRequestedProperty(Scope.from(properties), name);
        LOGGER.debug("Found containers providing {} : {}", name, containers);
        if (containers.size() > 1) {
            LOGGER.warn("More than one container provides {}. Will use the first one.", name);
        }
        return containers.stream()
            .findFirst()
            .flatMap(this::extractOptions)
            .map(options -> resolveFromConnectionOptions(propertyName, options));
    }

    private String resolveFromConnectionOptions(String propertyName, ConnectionFactoryOptions options) {
        String property = propertyName.substring(propertyName.lastIndexOf(".") + 1);
        switch (property) {
            case URL:
                Object db = options.getValue(ConnectionFactoryOptions.DATABASE);
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

}
