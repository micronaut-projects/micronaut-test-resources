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

import io.micronaut.core.value.PropertyResolver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An utility class to deal with property resolution.
 */
public class PropertyResolverSupport {
    /**
     * Resolves the required properties for a particular test resources
     * resolver.
     * @param propertyResolver the property resolver
     * @param testResourcesResolver the test resources resolver
     * @return the resolved properties
     */
    public static Map<String, Object> resolveRequiredProperties(
        PropertyResolver propertyResolver,
        TestResourcesResolver testResourcesResolver
    ) {
        List<String> requiredProperties = testResourcesResolver.getRequiredProperties();
        Map<String, Object> props = new HashMap<>(requiredProperties.size());
        for (String property : requiredProperties) {
            propertyResolver.getProperty(property, Object.class).ifPresent(value ->
                props.put(property, value)
            );
        }
        return Collections.unmodifiableMap(props);
    }
}
