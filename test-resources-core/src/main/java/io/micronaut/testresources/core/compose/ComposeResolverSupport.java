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
package io.micronaut.testresources.core.compose;

import io.micronaut.core.annotation.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Common Docker Compose lookup support for provider-specific test resources resolvers.
 */
@Internal
public final class ComposeResolverSupport {
    public static final int ORDER = -100;
    public static final String USERNAME_LABEL = ComposeLabels.USERNAME;
    public static final String PASSWORD_LABEL = ComposeLabels.PASSWORD;
    public static final String DATABASE_LABEL = ComposeLabels.DATABASE;
    static ComposeProjectManager projectManager;

    private static final Logger LOG = LoggerFactory.getLogger(ComposeResolverSupport.class);

    private ComposeResolverSupport() {
    }

    public static boolean isEnabled(Map<String, Object> testResourcesConfig) {
        return ComposeConfiguration.from(testResourcesConfig).enabled();
    }

    public static Optional<String> resolve(String propertyName,
                                           Map<String, Object> requestedProperties,
                                           Map<String, Object> testResourcesConfig,
                                           ServiceDescriptor descriptor,
                                           Predicate<ResolutionContext> additionalFilter,
                                           Function<ResolutionContext, String> resolver) {
        ComposeConfiguration configuration = ComposeConfiguration.from(testResourcesConfig, requestedProperties);
        if (!configuration.usable()) {
            if (configuration.enabled()) {
                LOG.warn("Docker Compose test resources are enabled, but no Compose files were found or configured.");
            }
            return Optional.empty();
        }
        try {
            return findSingle(projectManager().getOrCreate(configuration), descriptor, additionalFilter)
                .map(resolver)
                .filter(value -> value != null && !value.isBlank());
        } catch (ComposeCliException ex) {
            LOG.warn("Docker Compose test resources could not resolve '{}': {}", propertyName, ex.getMessage());
            return Optional.empty();
        }
    }

    public static synchronized void close() throws IOException {
        if (projectManager != null) {
            projectManager.close();
            projectManager = null;
        }
    }

    public static String hostPort(ComposePort port) {
        return port.host() + ":" + port.publishedPort();
    }

    public static String http(ComposePort port) {
        return "http://" + hostPort(port);
    }

    private static Optional<ResolutionContext> findSingle(ComposeProject project,
                                                          ServiceDescriptor descriptor,
                                                          Predicate<ResolutionContext> additionalFilter) {
        List<ResolutionContext> candidates = project.services()
            .stream()
            .filter(service -> !service.ignored())
            .filter(service -> matchesServiceType(service, descriptor.aliases()))
            .flatMap(service -> service.publishedPort(descriptor.targetPort())
                .map(port -> new ResolutionContext(service, port))
                .stream())
            .filter(additionalFilter)
            .toList();
        if (candidates.isEmpty()) {
            LOG.debug("No Docker Compose {} service matched the requested property", descriptor.serviceType());
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            LOG.warn("Multiple Docker Compose {} services match the requested property. Add '{}' labels to make the mapping explicit. Matched services: {}",
                descriptor.serviceType(),
                ComposeLabels.SERVICE,
                candidates.stream().map(context -> context.service().name()).toList());
            return Optional.empty();
        }
        ResolutionContext context = candidates.get(0);
        LOG.info("Matched Docker Compose service '{}' as {}", context.service().name(), descriptor.serviceType());
        return Optional.of(context);
    }

    private static boolean matchesServiceType(ComposeService service, Set<String> aliases) {
        Optional<String> label = service.serviceLabel();
        if (label.isPresent()) {
            return aliases.stream().map(ComposeResolverSupport::normalize).anyMatch(normalize(label.get())::equals);
        }
        String normalizedImage = normalize(service.image());
        return aliases.stream().map(ComposeResolverSupport::normalize).anyMatch(normalizedImage::contains);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    private static synchronized ComposeProjectManager projectManager() {
        if (projectManager == null) {
            projectManager = new ComposeProjectManager();
        }
        return projectManager;
    }

    /**
     * Describes how to discover a Compose service for a provider.
     *
     * @param serviceType The canonical service type.
     * @param aliases Additional labels or image fragments that identify the service.
     * @param targetPort The container port that must be published.
     */
    public record ServiceDescriptor(String serviceType, Set<String> aliases, int targetPort) {
        public ServiceDescriptor(String serviceType, Collection<String> aliases, int targetPort) {
            this(serviceType, aliases(serviceType, aliases), targetPort);
        }

        private static Set<String> aliases(String serviceType, Collection<String> aliases) {
            Set<String> allAliases = new LinkedHashSet<>();
            allAliases.add(serviceType);
            allAliases.addAll(aliases);
            return Set.copyOf(allAliases);
        }
    }

    /**
     * Matched Compose service and published port metadata.
     *
     * @param service The matched service.
     * @param port The published port.
     */
    public record ResolutionContext(ComposeService service, ComposePort port) {
        public String host() {
            return port.host();
        }

        public int publishedPort() {
            return port.publishedPort();
        }

        public String hostPort() {
            return ComposeResolverSupport.hostPort(port);
        }

        public String http() {
            return ComposeResolverSupport.http(port);
        }

        public Optional<String> httpEndpoint(int targetPort) {
            return service.publishedPort(targetPort).map(ComposeResolverSupport::http);
        }

        public String labelOrEnvironment(String label, String environmentName, String defaultValue) {
            return service.labelOrEnvironment(label, environmentName, defaultValue);
        }

        public boolean hasEnvironment(String name) {
            return service.environment().containsKey(name);
        }

        public boolean matchesDatasource(String datasource) {
            String explicitDatasource = service.labels().get(ComposeLabels.DATASOURCE);
            if (explicitDatasource != null) {
                return explicitDatasource.equalsIgnoreCase(datasource);
            }
            return "default".equals(datasource);
        }
    }
}
