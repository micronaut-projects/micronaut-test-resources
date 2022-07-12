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
package io.micronaut.testresources.hibernate.reactive.core;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.CONNECTION_DB_TYPE;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.CONNECTION_PASSWORD;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.CONNECTION_URL;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.CONNECTION_USERNAME;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.DB_TYPE;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.JPA;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.PASSWORD;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.RESOLVABLE_KEYS;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.URL;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.USERNAME;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.datasourceExpressionOf;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.datasourceNameFrom;
import static io.micronaut.testresources.hibernate.reactive.core.HibernateReactiveSupport.jpaExpressionOf;

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
public abstract class AbstractHibernateReactiveTestResourceProvider<T extends JdbcDatabaseContainer<? extends T>> extends AbstractTestContainersProvider<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHibernateReactiveTestResourceProvider.class);

    @Override
    public List<String> getRequiredPropertyEntries() {
        return HibernateReactiveSupport.REQUIRED_PROPERTY_ENTRIES_LIST;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return HibernateReactiveSupport.findRequiredProperties(expression);
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return HibernateReactiveSupport.findResolvableProperties(propertyEntries, RESOLVABLE_KEYS);
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        if (!propertyName.startsWith(JPA)) {
            return false;
        }
        String datasource = datasourceNameFrom(propertyName);
        String type = stringOrNull(requestedProperties.get(jpaExpressionOf(datasource, CONNECTION_DB_TYPE)));
        if (type != null) {
            return type.equalsIgnoreCase(getSimpleName());
        }
        type = stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, DB_TYPE)));
        return type != null && type.equalsIgnoreCase(getSimpleName());
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        String datasourceName = datasourceNameFrom(propertyName);
        if (propertyName.endsWith(CONNECTION_URL)) {
            Object url = properties.get(datasourceExpressionOf(datasourceName, URL));
            if (url != null) {
                return Optional.of(String.valueOf(url));
            }
        }
        if (propertyName.endsWith(CONNECTION_USERNAME)) {
            Object username = properties.get(datasourceExpressionOf(datasourceName, USERNAME));
            if (username != null) {
                return Optional.of(String.valueOf(username));
            }
        }
        if (propertyName.endsWith(CONNECTION_PASSWORD)) {
            Object pwd = properties.get(datasourceExpressionOf(datasourceName, PASSWORD));
            if (pwd != null) {
                return Optional.of(String.valueOf(pwd));
            }
        }
        return super.resolveWithoutContainer(propertyName, properties, testResourcesConfiguration);
    }

    @Override
    protected final Optional<String> resolveProperty(String expression, T container) {
        if (expression.endsWith(CONNECTION_URL)) {
            return Optional.of(container.getJdbcUrl());
        }
        if (expression.endsWith(CONNECTION_USERNAME)) {
            return Optional.of(container.getUsername());
        }
        if (expression.endsWith(CONNECTION_PASSWORD)) {
            return Optional.of(container.getPassword());
        }
        return Optional.empty();
    }


}
