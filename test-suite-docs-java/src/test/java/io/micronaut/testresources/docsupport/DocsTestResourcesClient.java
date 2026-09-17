package io.micronaut.testresources.docsupport;

import io.micronaut.testresources.client.TestResourcesClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A fake test resources client resolving the properties used by the examples of the guide, so that the
 * documentation test suite does not need a running test resources service.
 */
public class DocsTestResourcesClient implements TestResourcesClient {
    private static final Map<String, String> PROPERTIES = Map.of(
            "rabbitmq.uri", "amqp://localhost:5672"
    );

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return List.copyOf(PROPERTIES.keySet());
    }

    @Override
    public Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        return Optional.ofNullable(PROPERTIES.get(name));
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return List.of();
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of();
    }

    @Override
    public boolean closeAll() {
        return true;
    }

    @Override
    public boolean closeScope(String id) {
        return true;
    }
}
