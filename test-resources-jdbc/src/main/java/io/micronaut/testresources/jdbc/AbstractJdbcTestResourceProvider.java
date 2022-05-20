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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Superclass for JDBC test containers providers.
 * @param <T> the type of the container
 */
public abstract class AbstractJdbcTestResourceProvider<T extends JdbcDatabaseContainer<? extends T>> extends AbstractTestContainersProvider<T> {
    public static final String URL = "datasources.default.url";
    public static final String USERNAME = "datasources.default.username";
    public static final String PASSWORD = "datasources.default.password";
    public static final String DIALECT = "datasources.default.dialect";
    public static final String DRIVER = "datasources.default.driverClassName";

    private static final List<String> SUPPORTED_LIST = Collections.unmodifiableList(
        Arrays.asList(URL, USERNAME, PASSWORD)
    );

    @Override
    public List<String> getResolvableProperties() {
        return SUPPORTED_LIST;
    }

    @Override
    public List<String> getRequiredProperties() {
        return Stream.concat(super.getRequiredProperties().stream(), Stream.of(DIALECT, DRIVER)).collect(Collectors.toList());
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> properties) {
        String dialect = String.valueOf(properties.get(DIALECT));
        if (dialect != null) {
            return dialect.equalsIgnoreCase(getSimpleName());
        }
        String driver = String.valueOf(properties.get(DRIVER));
        if (driver != null) {
            return driver.toLowerCase(Locale.US).contains(getSimpleName());
        }
        return false;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, T container) {
        String value;
        switch (propertyName) {
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
                value = resolveDbSpecificProperty(propertyName, container);
        }
        return Optional.ofNullable(value);
    }

    /**
     * Given the started container, resolves properties which are specific to
     * a particular JDBC implementation.
     * @param propertyName the property to resolve
     * @param container the started container
     * @return the resolved property, or null if not resolvable
     */
    protected String resolveDbSpecificProperty(String propertyName, JdbcDatabaseContainer<?> container) {
        return null;
    }
}
