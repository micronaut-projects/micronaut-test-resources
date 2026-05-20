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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provider-owned Docker Compose property mapping.
 */
@Internal
public interface ComposeTestResourcesProvider {
    String getServiceType();

    Set<String> getAliases();

    int getPort();

    List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries);

    default List<String> getRequiredPropertyEntries() {
        return List.of();
    }

    default List<String> getRequiredProperties(String expression) {
        return List.of();
    }

    boolean supports(String propertyName, Map<String, Object> properties);

    @Nullable String resolve(ResolutionContext context);

    default boolean matches(ComposeService service) {
        return service.serviceLabel()
            .map(getAliases()::contains)
            .orElseGet(() -> getAliases().stream().anyMatch(service::imageContains) || service.exposes(getPort()));
    }

    default boolean matches(String propertyName, ComposeService service) {
        return matches(service);
    }

    static Set<String> aliases(String serviceType, Collection<String> aliases) {
        Set<String> allAliases = new LinkedHashSet<>();
        allAliases.add(normalize(serviceType));
        aliases.stream().map(ComposeTestResourcesProvider::normalize).forEach(allAliases::add);
        return Set.copyOf(allAliases);
    }

    static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    /**
     * Context available when resolving a property from a matched Compose service.
     *
     * @param propertyName The property being resolved.
     * @param endpoint The Testcontainers endpoint for the matched service.
     * @param service The matched Compose service.
     * @param properties Already-known application properties.
     */
    record ResolutionContext(String propertyName, ComposeEndpoint endpoint, ComposeService service, Map<String, Object> properties) {
        public String hostPort() {
            return endpoint.hostPort();
        }

        public String http() {
            return "http://" + hostPort();
        }

        public String labelOrEnvironment(String label, String environmentName, String defaultValue) {
            return service.labelOrEnvironment(label, environmentName, defaultValue);
        }

        public Optional<String> label(String label) {
            return Optional.ofNullable(service.labels().get(label));
        }

        public Optional<String> environment(String environmentName) {
            return Optional.ofNullable(service.environment().get(environmentName));
        }

        public String host() {
            return endpoint.host();
        }

        public int port() {
            return endpoint.port();
        }
    }
}
