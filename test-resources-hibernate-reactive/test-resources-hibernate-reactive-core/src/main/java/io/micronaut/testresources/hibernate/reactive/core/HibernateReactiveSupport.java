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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides constants and helper methods used by several test resources.
 */
public final class HibernateReactiveSupport {
    static final String JPA = "jpa";
    static final String DATASOURCES = "datasources";
    static final String CONNECTION_PREFIX = "properties.hibernate.connection.";
    static final String DB_TYPE = "db-type";
    static final String URL = "url";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String CONNECTION_URL = CONNECTION_PREFIX + URL;
    static final String CONNECTION_USERNAME = CONNECTION_PREFIX + USERNAME;
    static final String CONNECTION_PASSWORD = CONNECTION_PREFIX + PASSWORD;
    static final String CONNECTION_DB_TYPE = CONNECTION_PREFIX + DB_TYPE;

    static final List<String> RESOLVABLE_KEYS = Arrays.asList(
        CONNECTION_URL,
        CONNECTION_USERNAME,
        CONNECTION_PASSWORD
    );

    static final String JPA_PREFIX = JPA + ".";

    static final List<String> DATASOURCE_REQUIRED_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
        URL,
        USERNAME,
        PASSWORD,
        DB_TYPE
    ));
    public static final List<String> REQUIRED_PROPERTY_ENTRIES_LIST = Collections.unmodifiableList(Arrays.asList(JPA, DATASOURCES));

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateReactiveSupport.class);

    private HibernateReactiveSupport() {

    }

    public static List<String> findRequiredProperties(String expression) {
        if (expression.startsWith(JPA)) {
            // If we resolve jpa.default.properties.hibernate.connection.url, we will
            // try to find a regular datasource with the same name
            // and reuse it if it exists.
            String datasourceName = datasourceNameFrom(expression);
            List<String> requiredProperties = Stream.concat(
                Stream.of(jpaExpressionOf(datasourceName,  CONNECTION_DB_TYPE)),
                DATASOURCE_REQUIRED_PROPERTIES.stream().map(k -> datasourceExpressionOf(datasourceName, k))
            ).collect(Collectors.toList());

            LOGGER.debug("Required properties: {}", requiredProperties);
            return requiredProperties;
        }
        return Collections.emptyList();
    }

    public static List<String> findResolvableProperties(Map<String, Collection<String>> propertyEntries, List<String> resolvableKeys) {
        Collection<String> jpaDatasources = propertyEntries.getOrDefault(JPA, Collections.emptyList());
        Collection<String> datasources = propertyEntries.getOrDefault(DATASOURCES, Collections.emptyList());
        List<String> properties = Stream.concat(jpaDatasources.stream(), datasources.stream())
            .flatMap(name -> resolvableKeys.stream().map(key -> JPA + "." + name + "." + key))
            .distinct()
            .collect(Collectors.toList());
        LOGGER.debug("Resolvable properties: {}", properties);
        return properties;
    }

    public static String datasourceNameFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(0, remainder.indexOf("."));
    }

    public static String jpaExpressionOf(String datasource, String property) {
        return JPA + "." + datasource + "." + property;
    }

    public static String datasourceExpressionOf(String datasource, String property) {
        return DATASOURCES + "." + datasource + "." + property;
    }
}
