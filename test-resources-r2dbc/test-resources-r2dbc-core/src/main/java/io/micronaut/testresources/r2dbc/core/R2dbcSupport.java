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
public final class R2dbcSupport {
    public static final String DATASOURCES = "datasources";
    public static final String R2DBC_PREFIX = "r2dbc.";
    public static final String R2DBC_DATASOURCES = R2DBC_PREFIX + DATASOURCES;
    public static final String DIALECT = "dialect";
    public static final String DRIVER = "driverClassName";
    public static final String TYPE = "db-type";

    public static final List<String> REQUIRED_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
        DIALECT,
        DRIVER,
        TYPE
    ));
    public static final List<String> REQUIRED_PROPERTY_ENTRIES_LIST = Collections.unmodifiableList(Arrays.asList(R2DBC_DATASOURCES, DATASOURCES));

    private static final Logger LOGGER = LoggerFactory.getLogger(R2dbcSupport.class);

    private R2dbcSupport() {

    }

    public static List<String> findRequiredProperties(String expression) {
        if (expression.startsWith(R2dbcSupport.R2DBC_DATASOURCES)) {
            // If we resolve r2dbc.datasources.default.url, we will
            // try to find a regular datasource with the same name
            // and reuse it if it exists.
            String regularDatasource = R2dbcSupport.removeR2dbPrefixFrom(expression);
            String datasourceName = R2dbcSupport.datasourceNameFrom(regularDatasource);
            List<String> requiredProperties = Stream.concat(
                Stream.of(regularDatasource),
                REQUIRED_PROPERTIES.stream().map(k -> R2dbcSupport.r2dbDatasourceExpressionOf(datasourceName, k))
            ).collect(Collectors.toList());

            LOGGER.debug("Required properties: {}", requiredProperties);
            return requiredProperties;
        }
        return Collections.emptyList();
    }

    public static List<String> findResolvableProperties(Map<String, Collection<String>> propertyEntries, List<String> resolvableKeys) {
        Collection<String> r2dbcDatasources = propertyEntries.getOrDefault(R2dbcSupport.R2DBC_DATASOURCES, Collections.emptyList());
        Collection<String> datasources = propertyEntries.getOrDefault(R2dbcSupport.DATASOURCES, Collections.emptyList());
        List<String> properties = Stream.concat(r2dbcDatasources.stream(), datasources.stream())
            .flatMap(name -> resolvableKeys.stream().map(key -> R2dbcSupport.R2DBC_DATASOURCES + "." + name + "." + key))
            .distinct()
            .collect(Collectors.toList());
        LOGGER.debug("Resolvable properties: {}", properties);
        return properties;
    }

    public static String removeR2dbPrefixFrom(String propertyName) {
        return propertyName.substring(R2DBC_PREFIX.length());
    }

    public static String datasourceNameFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(0, remainder.indexOf("."));
    }

    public static String r2dbDatasourceExpressionOf(String datasource, String property) {
        return R2DBC_DATASOURCES + "." + datasource + "." + property;
    }
}
