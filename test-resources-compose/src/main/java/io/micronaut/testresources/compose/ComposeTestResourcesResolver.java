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
import java.util.ArrayList;
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
    private static final List<String> RABBITMQ_PROPERTIES = List.of("rabbitmq.uri", "rabbitmq.username", "rabbitmq.password");
    private static final List<String> DATASOURCE_PROPERTIES = List.of("url", "username", "password", "driver-class-name");

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
        List<String> properties = new ArrayList<>();
        for (String datasource : propertyEntries.getOrDefault("datasources", Collections.emptyList())) {
            for (String property : DATASOURCE_PROPERTIES) {
                properties.add(DATASOURCES_PREFIX + datasource + "." + property);
            }
        }
        properties.add("redis.uri");
        properties.addAll(RABBITMQ_PROPERTIES);
        return properties;
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of("datasources");
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (!expression.startsWith(DATASOURCES_PREFIX)) {
            return Collections.emptyList();
        }
        String remainder = expression.substring(DATASOURCES_PREFIX.length());
        int separator = remainder.indexOf('.');
        if (separator < 1) {
            return Collections.emptyList();
        }
        String datasource = remainder.substring(0, separator);
        return List.of(
            DATASOURCES_PREFIX + datasource + ".db-type",
            DATASOURCES_PREFIX + datasource + ".dialect"
        );
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
}
