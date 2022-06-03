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

import io.micronaut.core.io.ResourceLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementations are responsible for generating the list
 * of keys that a test resource resolver is able to resolve.
 */
public interface PropertyExpressionProducer {
    /**
     * The list of property entries that the producer needs to know
     * about in order to provide the list of keys that it can resolve.
     * For example, a resolver may need to know about the "datasources"
     * names. For this, it would return a list containing the string
     * "datasources".
     * @return the property entries needed by the resolver
     */
    default List<String> getPropertyEntries() {
        return Collections.emptyList();
    }

    /**
     * Returns the list of keys the resolver is able to generate. This is
     * called with the list of resolved property entries. The property entries
     * map therefore contains one entry for each property returned by the
     * {@link #getPropertyEntries()} method. For example, if the {@link #getPropertyEntries()}
     * method returns a single-entry list with "datasources", then the map will
     * contain a single entry with "datasources" as the key, and the datasource
     * names as the value. The producer can then generate a list of keys for each
     * datasource name.
     *
     * @param resourceLoader the resource loader
     * @param propertyEntries the map of resolved property entries
     * @param testResourcesConfig the test resources configuration
     * @return the list of keys
     */
    List<String> produceKeys(ResourceLoader resourceLoader, Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig);
}
