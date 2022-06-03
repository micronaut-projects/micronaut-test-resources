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
package io.micronaut.testresources.core;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resources resolver is responsible for resolving test
 * resources. This should be done when the {@link #resolve(String, Map, Map)}
 * method is called. This method is called with a property, corresponding
 * to a property which doesn't exist in the user configuration.
 * For example, if the "jdbc.driver" property is missing, a resolver
 * may declare that it can resolve that property.
 * As part of the process, it may start a test container for example.
 */
public interface TestResourcesResolver {
    /**
     * The name of the top-level test resources property configuration key.
     */
    String TEST_RESOURCES_PROPERTY = "test-resources";

    /**
     * Returns the list of properties that this resolver
     * is able to support. The property entries map is
     * a map which keys are the property entries retrieved
     * from calling {@link #getRequiredPropertyEntries()} and
     * values are those property entries resolved.
     *
     * @param propertyEntries the property entries
     * @param testResourcesConfig the test resources configuration
     * @return the list of properties
     */
    List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig);

    /**
     * Returns the list of properties which should be read
     * before resolving an expression: this can be used if the resolver
     * itself needs some configuration properties.
     *
     * @param expression the expression which needs to be resolved.
     *
     * @return the list of configuration properties this
     * resolver requires
     */
    default List<String> getRequiredProperties(String expression) {
        return Collections.emptyList();
    }

    default List<String> getRequiredPropertyEntries() {
        return Collections.emptyList();
    }

    /**
     * Resolves the given property.
     *
     * @param propertyName the property to resolve
     * @param properties the resolved required properties
     * @param testResourcesConfiguration the test resources configuration
     * @return the resolved property or empty if not found
     */
    Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration);
}
