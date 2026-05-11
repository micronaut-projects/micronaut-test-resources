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
package io.micronaut.testresources.compose;

import io.micronaut.testresources.core.ToggableTestResourcesResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves supported properties from Docker Compose services.
 */
public class ComposeTestResourcesResolver implements ToggableTestResourcesResolver, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ComposeTestResourcesResolver.class);
    private static final int ORDER_BEFORE_SPECIFIC_TESTCONTAINERS = -100;
    private static final String DATASOURCES_PREFIX = "datasources.";
    private static final String R2DBC_DATASOURCES_PREFIX = "r2dbc.datasources.";
    private static final String JPA_PREFIX = "jpa.";
    private static final String DB_TYPE_SUFFIX = ".db-type";
    private static final String DB_NAME_SUFFIX = ".db-name";

    private final ComposeProjectManager projectManager;
    private final ComposePropertyMapper propertyMapper;

    public ComposeTestResourcesResolver() {
        this(new ComposeProjectManager(), new ComposePropertyMapper());
    }

    ComposeTestResourcesResolver(ComposeProjectManager projectManager, ComposePropertyMapper propertyMapper) {
        this.projectManager = projectManager;
        this.propertyMapper = propertyMapper;
    }

    @Override
    public String getName() {
        return "compose";
    }

    @Override
    public String getDisplayName() {
        return "Docker Compose";
    }

    @Override
    public int getOrder() {
        return ORDER_BEFORE_SPECIFIC_TESTCONTAINERS;
    }

    @Override
    public boolean isEnabled(Map<String, Object> testResourcesConfig) {
        return ComposeConfiguration.from(testResourcesConfig).enabled();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        ComposeConfiguration configuration = ComposeConfiguration.from(testResourcesConfig);
        if (!configuration.usable()) {
            return Collections.emptyList();
        }
        return propertyMapper.resolvableProperties(propertyEntries);
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of("datasources", "r2dbc.datasources", "jpa", "mongodb.servers");
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (expression.startsWith(DATASOURCES_PREFIX)) {
            String datasource = datasourceName(expression, DATASOURCES_PREFIX);
            if (datasource == null) {
                return Collections.emptyList();
            }
            return List.of(
                DATASOURCES_PREFIX + datasource + DB_TYPE_SUFFIX,
                DATASOURCES_PREFIX + datasource + ".dialect",
                DATASOURCES_PREFIX + datasource + DB_NAME_SUFFIX,
                DATASOURCES_PREFIX + datasource + ".test-resources.resource-name"
            );
        }
        if (expression.startsWith(R2DBC_DATASOURCES_PREFIX)) {
            String datasource = datasourceName(expression, R2DBC_DATASOURCES_PREFIX);
            if (datasource == null) {
                return Collections.emptyList();
            }
            return List.of(
                R2DBC_DATASOURCES_PREFIX + datasource + DB_TYPE_SUFFIX,
                R2DBC_DATASOURCES_PREFIX + datasource + ".dialect",
                R2DBC_DATASOURCES_PREFIX + datasource + ".driverClassName",
                R2DBC_DATASOURCES_PREFIX + datasource + DB_NAME_SUFFIX,
                R2DBC_DATASOURCES_PREFIX + datasource + ".test-resources.resource-name",
                DATASOURCES_PREFIX + datasource + DB_NAME_SUFFIX
            );
        }
        if (expression.startsWith(JPA_PREFIX)) {
            String datasource = datasourceName(expression, JPA_PREFIX);
            if (datasource == null) {
                return Collections.emptyList();
            }
            return List.of(
                JPA_PREFIX + datasource + ".properties.hibernate.connection" + DB_TYPE_SUFFIX,
                DATASOURCES_PREFIX + datasource + DB_TYPE_SUFFIX,
                DATASOURCES_PREFIX + datasource + ".url",
                DATASOURCES_PREFIX + datasource + ".username",
                DATASOURCES_PREFIX + datasource + ".password"
            );
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        ComposeConfiguration configuration = ComposeConfiguration.from(testResourcesConfig, properties);
        if (!configuration.usable()) {
            if (configuration.enabled()) {
                LOG.warn("Docker Compose test resources are enabled, but no Compose files were found or configured.");
            }
            return Optional.empty();
        }
        try {
            return propertyMapper.resolve(propertyName, properties, projectManager.getOrCreate(configuration));
        } catch (ComposeCliException ex) {
            LOG.warn("Docker Compose test resources could not resolve '{}': {}", propertyName, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void close() throws IOException {
        projectManager.close();
    }

    private static String datasourceName(String expression, String prefix) {
        String remainder = expression.substring(prefix.length());
        int separator = remainder.indexOf('.');
        if (separator < 1) {
            return null;
        }
        return remainder.substring(0, separator);
    }
}
