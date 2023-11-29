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
package io.micronaut.testresources.controlpanel;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.server.PropertyResolutionListener;
import jakarta.inject.Singleton;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This resolution listener will record all properties resolved by a test
 * resource, so that we can display the resolved values in the appropriate
 * cards.
 */
@Singleton
public class ControlPanelPropertyResolutionListener implements PropertyResolutionListener {
    private final Map<String, Set<Resolution>> resolvedProperties = new HashMap<>();
    private final Map<String, Set<ResolutionError>> errors = new HashMap<>();

    @Override
    public void resolved(String property, String resolvedValue, TestResourcesResolver resolver,
                         Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        var resolution = new Resolution(
            property,
            resolvedValue,
            asStringMap(properties),
            asStringMap(testResourcesConfig)
        );
        resolvedProperties.computeIfAbsent(resolver.getId(), k -> new HashSet<>())
            .add(resolution);
    }

    @Override
    public void errored(String property, TestResourcesResolver resolver, Throwable exception) {
        var error = ResolutionError.of(property, exception);
        errors.computeIfAbsent(resolver.getId(), k -> new HashSet<>())
            .add(error);
    }

    /**
     * Returns the resolutions performed by a particular resolver.
     * @param resolver the resolver
     * @return the resolutions
     */
    public List<Resolution> findByResolver(TestResourcesResolver resolver) {
        return findById(resolver.getId());
    }

    /**
     * Returns the resolutions performed by a particular resolver.
     * @param id the resolver id
     * @return the resolutions
     */
    public List<Resolution> findById(String id) {
        return resolvedProperties.getOrDefault(id, Set.of())
            .stream()
            .sorted(Comparator.comparing(Resolution::property))
            .toList();
    }

    /**
     * Returns the resolutions performed by a particular resolver.
     * @param id the resolver id
     * @return the resolutions
     */
    public List<ResolutionError> findErrorsById(String id) {
        return errors.getOrDefault(id, Set.of())
            .stream()
            .sorted(Comparator.comparing(ResolutionError::property))
            .toList();
    }

    private static Map<String, String> asStringMap(Map<String, Object> input) {
        return input.entrySet()
            .stream()
            .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> String.valueOf(entry.getValue())
        ));
    }

    /**
     * A property resolution.
     *
     * @param property the property which was resolved
     * @param resolvedValue the resolved value as a string
     * @param properties the properties which were used for the resolution context
     * @param testResourcesConfig the test resources configuration of the application
     */
    @Introspected
    public record Resolution(
        String property,
        String resolvedValue,
        Map<String, String> properties,
        Map<String, String> testResourcesConfig
    ) {

    }

    /**
     * A property resolution error.
     *
     * @param property the property which was resolved
     * @param stackTrace the associated resolution error
     */
    @Introspected
    public record ResolutionError(
        String property,
        String stackTrace
    ) {

        /**
         * Builds a resolution error.
         * @param property the property
         * @param exception the exception
         * @return a resolution error
         */
        public static ResolutionError of(String property, Throwable exception) {
            var stackTrace = new StringWriter();
            try (var writer = new PrintWriter(stackTrace)) {
                exception.printStackTrace(writer);
            }
            return new ResolutionError(property, stackTrace.toString());
        }
    }
}
