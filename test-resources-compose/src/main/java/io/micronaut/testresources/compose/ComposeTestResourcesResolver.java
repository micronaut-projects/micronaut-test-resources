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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Resolves supported missing Micronaut properties from a Testcontainers-managed Docker Compose environment.
 */
public final class ComposeTestResourcesResolver implements ToggableTestResourcesResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ComposeTestResourcesResolver.class);
    private static final int ORDER = -10;

    private final ComposeMetadataParser parser;
    private final ComposeEnvironmentManager manager;
    private final List<ComposeTestResourcesProvider> providers;

    public ComposeTestResourcesResolver() {
        this(new ComposeMetadataParser(), new ComposeContainerManager(), loadProviders());
    }

    ComposeTestResourcesResolver(ComposeMetadataParser parser, ComposeEnvironmentManager manager) {
        this(parser, manager, loadProviders());
    }

    ComposeTestResourcesResolver(ComposeMetadataParser parser, ComposeEnvironmentManager manager, List<ComposeTestResourcesProvider> providers) {
        this.parser = parser;
        this.manager = manager;
        this.providers = List.copyOf(providers);
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
        Set<String> resolvable = new LinkedHashSet<>();
        providers.stream()
            .flatMap(provider -> provider.getResolvableProperties(propertyEntries).stream())
            .forEach(resolvable::add);
        return List.copyOf(resolvable);
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        Set<String> entries = new LinkedHashSet<>();
        providers.stream()
            .flatMap(provider -> provider.getRequiredPropertyEntries().stream())
            .forEach(entries::add);
        return List.copyOf(entries);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        Set<String> required = new LinkedHashSet<>();
        providers.stream()
            .flatMap(provider -> provider.getRequiredProperties(expression).stream())
            .forEach(required::add);
        return List.copyOf(required);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        ComposeConfiguration configuration = ComposeConfiguration.from(testResourcesConfig, properties);
        if (!configuration.usable()) {
            return Optional.empty();
        }
        try {
            ComposeProject project = parser.parse(configuration);
            for (ComposeTestResourcesProvider provider : providers) {
                if (provider.supports(propertyName, properties)) {
                    Optional<String> resolved = resolveProvider(propertyName, configuration, project, properties, provider);
                    if (resolved.isPresent()) {
                        return resolved;
                    }
                }
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.warn("Unable to resolve {} from Docker Compose; falling back to the normal Test Resources provider: {}", propertyName, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> resolveProvider(String propertyName,
                                             ComposeConfiguration configuration,
                                             ComposeProject project,
                                             Map<String, Object> properties,
                                             ComposeTestResourcesProvider provider) {
        Optional<ComposeService> service = match(project, configuration, candidate -> provider.matches(propertyName, candidate), provider.getServiceType());
        if (service.isEmpty()) {
            return Optional.empty();
        }
        return manager.endpoint(configuration, project, service.get(), provider.getPort(), properties)
            .map(endpoint -> provider.resolve(new ComposeTestResourcesProvider.ResolutionContext(propertyName, endpoint, service.get(), properties)))
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
        if (LOG.isInfoEnabled()) {
            LOG.info("Matched Docker Compose service {} for {}", matches.get(0).redactedSummary(), description);
        }
        return Optional.of(matches.get(0));
    }

    private static List<ComposeTestResourcesProvider> loadProviders() {
        return ServiceLoader.load(ComposeTestResourcesProvider.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .sorted(Comparator.comparing(ComposeTestResourcesProvider::getServiceType))
            .toList();
    }
}
