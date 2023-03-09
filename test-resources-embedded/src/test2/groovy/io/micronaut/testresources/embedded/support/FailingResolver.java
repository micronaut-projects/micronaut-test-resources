package io.micronaut.testresources.embedded.support;

import io.micronaut.testresources.core.TestResourcesResolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resources resolver which will fail because it returns a camel case property.
 */
public class FailingResolver implements TestResourcesResolver {
    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        // must not use camel case
        return Collections.singletonList("myProperty");
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        return Optional.empty();
    }
}
