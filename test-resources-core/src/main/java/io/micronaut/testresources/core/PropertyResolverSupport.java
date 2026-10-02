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

import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.core.value.PropertyResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An utility class to deal with property resolution.
 */
public class PropertyResolverSupport {
    /**
     * Builds the test resources configuration map for the given property resolver.
     * <p>
     * Micronaut normalizes an indexed property key (e.g. {@code containers.foo.env[0].SOME_VAR}) in
     * full before splitting it into the aggregated {@code List<Map<String, String>>} values consumed
     * by test resources resolvers, so the map key {@code SOME_VAR} arrives lower-cased and hyphenated
     * as {@code some-var}. That matters for case-sensitive values such as container environment
     * variable names. This method overlays the {@link StringConvention#RAW} catalog's aggregated
     * lists, which spell those keys as the user wrote them, onto the generated catalog, which
     * {@link io.micronaut.testresources.testcontainers.TestContainerMetadataSupport} needs for
     * structural keys like {@code image-name}. The raw aggregate is keyed by the verbatim base name,
     * so it is hyphenated to line up with the generated catalog's key before overlaying.
     *
     * @param propertyResolver the property resolver
     * @return the test resources configuration map, with map keys of indexed entries spelled as written
     */
    public static Map<String, Object> resolveTestResourcesConfiguration(PropertyResolver propertyResolver) {
        Map<String, Object> generated = propertyResolver.getProperties(TestResourcesResolver.TEST_RESOURCES_PROPERTY);
        Map<String, Object> raw = propertyResolver.getProperties(TestResourcesResolver.TEST_RESOURCES_PROPERTY, StringConvention.RAW);
        if (raw.isEmpty()) {
            return generated;
        }
        Map<String, Object> merged = new HashMap<>(generated);
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() instanceof List<?> rawList) {
                String generatedKey = NameUtils.hyphenate(entry.getKey(), true);
                Object existing = merged.get(generatedKey);
                merged.put(generatedKey, existing instanceof List<?> existingList
                    ? overlayList(existingList, rawList) : rawList);
            }
        }
        return merged;
    }

    /**
     * Overlays a RAW aggregated list, position by position, onto the equivalent GENERATED list.
     * Two different spellings of one base name produce two partial RAW aggregates, so a single
     * spelling's element must not blindly replace the merged, complete GENERATED element at that
     * position: only the keys the RAW element actually supplies are respelled, and every other key
     * in the GENERATED element, contributed by another spelling, is kept.
     * <p>
     * With {@link io.micronaut.context.env.PropertySourcePropertyResolver} the RAW list can never be
     * longer than the GENERATED one, since GENERATED holds the union of every spelling. The trailing
     * branch is kept for other {@link PropertyResolver} implementations, which this method accepts
     * and whose catalogs carry no such guarantee.
     *
     * @param generatedList the complete list from the GENERATED catalog
     * @param rawList the (possibly partial) list from the RAW catalog for one spelling
     * @return a list combining GENERATED's completeness with the keys as RAW spells them
     */
    private static List<Object> overlayList(List<?> generatedList, List<?> rawList) {
        List<Object> result = new ArrayList<>(generatedList);
        for (int i = 0; i < rawList.size(); i++) {
            Object rawValue = rawList.get(i);
            if (rawValue != null) {
                Object mergedValue;
                if (i < result.size() && result.get(i) instanceof Map<?, ?> generatedMap && rawValue instanceof Map<?, ?> rawMap) {
                    mergedValue = overlayMap(generatedMap, rawMap);
                } else {
                    mergedValue = rawValue;
                }
                if (i < result.size()) {
                    result.set(i, mergedValue);
                } else {
                    result.add(mergedValue);
                }
            }
        }
        return result;
    }

    /**
     * Overlays a RAW aggregated element map onto the equivalent GENERATED element map, at key
     * granularity. GENERATED's map is complete (it already contains the keys contributed by every
     * spelling at this position), so it is the base; for each key RAW supplies, its hyphenated
     * (GENERATED) counterpart is removed and replaced with the RAW key and value, restoring the
     * spelling the user wrote without discarding keys contributed by a different spelling.
     *
     * @param generatedMap the complete element map from the GENERATED catalog at one position
     * @param rawMap the (possibly partial) element map from the RAW catalog for one spelling
     * @return a map combining GENERATED's completeness with the keys as RAW spells them
     */
    private static Map<Object, Object> overlayMap(Map<?, ?> generatedMap, Map<?, ?> rawMap) {
        Map<Object, Object> result = new HashMap<>(generatedMap);
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Object rawKey = entry.getKey();
            if (rawKey instanceof String rawKeyString) {
                result.remove(NameUtils.hyphenate(rawKeyString, true));
            }
            result.put(rawKey, entry.getValue());
        }
        return result;
    }

    /**
     * Resolves the required properties for a particular test resources
     * resolver.
     * @param expression the expression which is being resolved
     * @param propertyResolver the property resolver
     * @param testResourcesResolver the test resources resolver
     * @return the resolved properties
     */
    public static Map<String, Object> resolveRequiredProperties(
        String expression,
        PropertyResolver propertyResolver,
        TestResourcesResolver testResourcesResolver
    ) {
        List<String> requiredProperties = testResourcesResolver.getRequiredProperties(expression);
        Map<String, Object> props = new HashMap<>(requiredProperties.size() + 1);
        propertyResolver.getProperty(Scope.PROPERTY_KEY, String.class).ifPresent(scope -> props.put(Scope.PROPERTY_KEY, scope));
        for (String property : requiredProperties) {
            propertyResolver.getProperty(property, Object.class).ifPresent(value ->
                props.put(property, value)
            );
        }
        return Collections.unmodifiableMap(props);
    }

    /**
     * Determines if a test resources resolver can resolve a particular
     * expression.
     *
     * @param propertyResolver the property resolver
     * @param testResourcesResolver the test resources resolver
     * @param expression the expression
     * @param testProperties the test resources configuration map
     * @return the resolved property entries
     */
    public static boolean canResolveExpression(
        PropertyResolver propertyResolver,
        TestResourcesResolver testResourcesResolver,
        String expression,
        Map<String, Object> testProperties) {
        List<String> requiredProperties = testResourcesResolver.getRequiredPropertyEntries();
        Map<String, Collection<String>> props = new HashMap<>(requiredProperties.size());
        for (String property : requiredProperties) {
            Collection<String> entries = propertyResolver.getPropertyEntries(property);
            props.put(property, entries);
        }
        return testResourcesResolver.getResolvableProperties(props, testProperties).contains(expression);
    }
}
