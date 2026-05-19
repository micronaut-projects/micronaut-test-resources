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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Resolves supported missing Micronaut properties from a Testcontainers-managed Docker Compose environment.
 */
public final class ComposeTestResourcesResolver implements ToggableTestResourcesResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ComposeTestResourcesResolver.class);
    private static final int ORDER = -10;

    private final ComposeMetadataParser parser;
    private final ComposeEnvironmentManager manager;

    public ComposeTestResourcesResolver() {
        this(new ComposeMetadataParser(), new ComposeContainerManager());
    }

    ComposeTestResourcesResolver(ComposeMetadataParser parser, ComposeEnvironmentManager manager) {
        this.parser = parser;
        this.manager = manager;
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
        return ORDER;
    }

    @Override
    public boolean isEnabled(Map<String, Object> testResourcesConfig) {
        Object enabled = testResourcesConfig.get("compose.enabled");
        if (enabled instanceof Boolean b) {
            return b;
        }
        return enabled != null && Boolean.parseBoolean(String.valueOf(enabled));
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        List<String> resolvable = new ArrayList<>();
        Collection<String> datasources = propertyEntries.getOrDefault(ComposeServiceDescriptors.DATASOURCES, List.of());
        for (String datasource : datasources) {
            resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.URL));
            resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.USERNAME));
            resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.PASSWORD));
            resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.DRIVER));
        }
        Collection<String> r2dbcDatasources = propertyEntries.getOrDefault(ComposeServiceDescriptors.R2DBC_DATASOURCES, List.of());
        Stream.concat(r2dbcDatasources.stream(), datasources.stream())
            .distinct()
            .forEach(datasource -> {
                resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.URL));
                resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.USERNAME));
                resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.PASSWORD));
            });
        Collection<String> jpaDatasources = propertyEntries.getOrDefault(ComposeServiceDescriptors.JPA, List.of());
        Stream.concat(jpaDatasources.stream(), datasources.stream())
            .distinct()
            .forEach(datasource -> {
                resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.JPA, datasource, ComposeServiceDescriptors.HIBERNATE_CONNECTION + ComposeServiceDescriptors.URL));
                resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.JPA, datasource, ComposeServiceDescriptors.HIBERNATE_CONNECTION + ComposeServiceDescriptors.USERNAME));
                resolvable.add(ComposeServiceDescriptors.property(ComposeServiceDescriptors.JPA, datasource, ComposeServiceDescriptors.HIBERNATE_CONNECTION + ComposeServiceDescriptors.PASSWORD));
            });
        ComposeServiceDescriptors.SERVICES.stream()
            .flatMap(descriptor -> descriptor.properties().stream())
            .distinct()
            .forEach(resolvable::add);
        propertyEntries.getOrDefault("mongodb.servers", List.of()).stream()
            .map(server -> "mongodb.servers." + server + ".uri")
            .forEach(resolvable::add);
        return resolvable;
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of(ComposeServiceDescriptors.DATASOURCES, ComposeServiceDescriptors.R2DBC_DATASOURCES, ComposeServiceDescriptors.JPA, "mongodb.servers");
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        String datasource = ComposeServiceDescriptors.datasourceName(expression);
        if (datasource == null) {
            return List.of();
        }
        if (expression.startsWith(ComposeServiceDescriptors.R2DBC_DATASOURCES + ".")) {
            return Stream.of(
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.URL),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.TYPE),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.DIALECT),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.R2DBC_DRIVER),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.DB_NAME),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.R2DBC_DATASOURCES, datasource, ComposeServiceDescriptors.RESOURCE_NAME),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.DB_NAME)
                )
                .toList();
        }
        if (expression.startsWith(ComposeServiceDescriptors.JPA + ".")) {
            return Stream.of(
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.JPA, datasource, ComposeServiceDescriptors.HIBERNATE_CONNECTION + ComposeServiceDescriptors.TYPE),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.TYPE),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.URL),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.USERNAME),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.PASSWORD),
                    ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, ComposeServiceDescriptors.DB_NAME)
                )
                .toList();
        }
        return Stream.of(ComposeServiceDescriptors.TYPE, ComposeServiceDescriptors.DIALECT, ComposeServiceDescriptors.DB_NAME, ComposeServiceDescriptors.RESOURCE_NAME)
            .map(property -> ComposeServiceDescriptors.property(ComposeServiceDescriptors.DATASOURCES, datasource, property))
            .toList();
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        ComposeConfiguration configuration = ComposeConfiguration.from(testResourcesConfig, properties);
        if (!configuration.usable()) {
            return Optional.empty();
        }
        try {
            ComposeProject project = parser.parse(configuration);
            Optional<String> simple = resolveService(propertyName, configuration, project, properties);
            if (simple.isPresent()) {
                return simple;
            }
            Optional<String> database = resolveDatabase(propertyName, configuration, project, properties);
            if (database.isPresent()) {
                return database;
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.warn("Unable to resolve {} from Docker Compose; falling back to the normal Test Resources provider: {}", propertyName, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> resolveService(String propertyName,
                                            ComposeConfiguration configuration,
                                            ComposeProject project,
                                            Map<String, Object> properties) {
        Optional<ComposeServiceDescriptors.ServiceDescriptor> descriptor = ComposeServiceDescriptors.findServiceDescriptor(propertyName);
        if (descriptor.isEmpty()) {
            return Optional.empty();
        }
        Optional<ComposeService> service = match(project, configuration, descriptor.get()::matches, descriptor.get().serviceType());
        if (service.isEmpty()) {
            return Optional.empty();
        }
        if (descriptor.get().serviceType().equals("redis") && (service.get().labels().containsKey(ComposeLabels.PASSWORD) || service.get().environment().containsKey("REDIS_PASSWORD"))) {
            LOG.warn("Ignoring Docker Compose Redis service {} because authenticated Redis URI mapping is not supported", service.get().name());
            return Optional.empty();
        }
        return manager.endpoint(configuration, project, service.get(), descriptor.get().port(), properties)
            .map(endpoint -> descriptor.get().resolve(new ComposeServiceDescriptors.ResolutionContext(propertyName, endpoint, service.get())))
            .filter(value -> value != null && !value.isBlank());
    }

    private Optional<String> resolveDatabase(String propertyName,
                                             ComposeConfiguration configuration,
                                             ComposeProject project,
                                             Map<String, Object> properties) {
        Optional<ComposeServiceDescriptors.DatabaseDescriptor> descriptor = ComposeServiceDescriptors.findDatabaseDescriptor(propertyName, properties);
        if (descriptor.isEmpty()) {
            return Optional.empty();
        }
        String datasource = ComposeServiceDescriptors.datasourceName(propertyName);
        Optional<ComposeService> service = match(project, configuration, candidate -> descriptor.get().matches(candidate) && matchesDatasource(candidate, datasource), descriptor.get().serviceType() + " datasource '" + datasource + "'");
        if (service.isEmpty()) {
            return Optional.empty();
        }
        String endpointIndependentValue = descriptor.get().resolveWithoutEndpoint(propertyName, service.get(), properties);
        if (endpointIndependentValue != null) {
            return Optional.of(endpointIndependentValue);
        }
        return manager.endpoint(configuration, project, service.get(), descriptor.get().port(), properties)
            .map(endpoint -> descriptor.get().resolve(propertyName, endpoint, service.get(), properties))
            .filter(value -> value != null && !value.isBlank());
    }

    private Optional<ComposeService> match(ComposeProject project,
                                           ComposeConfiguration configuration,
                                           Predicate<ComposeService> predicate,
                                           String description) {
        List<ComposeService> matches = project.services().stream()
            .filter(service -> !service.ignored())
            .filter(service -> service.activeFor(configuration.profiles()))
            .filter(predicate)
            .toList();
        if (matches.isEmpty()) {
            LOG.debug("No Docker Compose service matched {}", description);
            return Optional.empty();
        }
        if (matches.size() > 1) {
            LOG.warn("Multiple Docker Compose services matched {}: {}. Falling back to the normal Test Resources provider.",
                description,
                matches.stream().map(ComposeService::redactedSummary).toList());
            return Optional.empty();
        }
        LOG.info("Matched Docker Compose service {} for {}", matches.get(0).redactedSummary(), description);
        return Optional.of(matches.get(0));
    }

    private static boolean matchesDatasource(ComposeService service, String datasource) {
        String label = service.labels().get(ComposeLabels.DATASOURCE);
        if (label != null) {
            return label.equalsIgnoreCase(datasource);
        }
        return "default".equals(datasource);
    }
}
