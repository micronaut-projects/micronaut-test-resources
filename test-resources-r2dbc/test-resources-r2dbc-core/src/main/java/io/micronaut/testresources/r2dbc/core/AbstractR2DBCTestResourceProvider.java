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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final String DATASOURCES = "datasources";
    private static final String R2DBC_PREFIX = "r2dbc.";
    private static final String R2DBC_DATASOURCES = R2DBC_PREFIX + DATASOURCES;
    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final List<String> RESOLVABLE_KEYS = Arrays.asList(
        URL,
        USERNAME,
        PASSWORD
    );

    private static final String DIALECT = "dialect";
    private static final String DRIVER = "driverClassName";
    private static final String TYPE = "db-type";
    private static final List<String> REQUIRED_PROPERTIES = Arrays.asList(
        DIALECT,
        DRIVER,
        TYPE
    );

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Arrays.asList(R2DBC_DATASOURCES, DATASOURCES);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (expression.startsWith(R2DBC_DATASOURCES)) {
            // If we resolve r2dbc.datasources.default.url, we will
            // try to find a regular datasource with the same name
            // and reuse it if it exists.
            String regularDatasource = removeR2dbPrefixFrom(expression);
            String datasourceName = datasourceNameFrom(regularDatasource);
            List<String> requiredProperties = Stream.concat(
                Stream.of(regularDatasource),
                REQUIRED_PROPERTIES.stream().map(k -> r2dbDatasourceExpressionOf(datasourceName, k))
            ).collect(Collectors.toList());

            LOGGER.debug("Required properties: {}", requiredProperties);
            return requiredProperties;
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        Collection<String> r2dbcDatasources = propertyEntries.getOrDefault(R2DBC_DATASOURCES, Collections.emptyList());
        Collection<String> datasources = propertyEntries.getOrDefault(DATASOURCES, Collections.emptyList());
        List<String> properties = Stream.concat(r2dbcDatasources.stream(), datasources.stream())
            .flatMap(name -> RESOLVABLE_KEYS.stream().map(key -> R2DBC_DATASOURCES + "." + name + "." + key))
            .distinct()
            .collect(Collectors.toList());
        LOGGER.debug("Resolvable properties: {}", properties);
        return properties;
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties) {
        if (!propertyName.startsWith(R2DBC_PREFIX)) {
            return false;
        }
        String baseDatasourceExpression = removeR2dbPrefixFrom(propertyName);
        String datasource = datasourceNameFrom(baseDatasourceExpression);
        String type = String.valueOf(properties.get(r2dbDatasourceExpressionOf(datasource, TYPE)));
        if (type != null && type.equalsIgnoreCase(getSimpleName())) {
            return true;
        }
        String driver = String.valueOf(properties.get(r2dbDatasourceExpressionOf(datasource, DRIVER)));
        if (driver != null && driver.toLowerCase(Locale.US).contains(getSimpleName())) {
            return true;
        }
        String dialect = String.valueOf(properties.get(r2dbDatasourceExpressionOf(datasource, DIALECT)));
        if (dialect != null && dialect.equalsIgnoreCase(getSimpleName())) {
            return true;
        }
        return false;
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName, Map<String, Object> properties) {
        String name = removeR2dbPrefixFrom(propertyName);
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

    private static String removeR2dbPrefixFrom(String propertyName) {
        return propertyName.substring(R2DBC_PREFIX.length());
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

    protected static String datasourceNameFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(0, remainder.indexOf("."));
    }

    protected static String r2dbDatasourceExpressionOf(String datasource, String property) {
        return R2DBC_DATASOURCES + "." + datasource + "." + property;
    }
}
