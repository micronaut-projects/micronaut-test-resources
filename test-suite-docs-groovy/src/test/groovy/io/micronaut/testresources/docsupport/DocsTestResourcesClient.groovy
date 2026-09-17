package io.micronaut.testresources.docsupport

import io.micronaut.testresources.client.TestResourcesClient

/**
 * A fake test resources client resolving the properties used by the examples of the guide, so that the
 * documentation test suite does not need a running test resources service.
 */
class DocsTestResourcesClient implements TestResourcesClient {
    private static final Map<String, String> PROPERTIES = [
            "rabbitmq.uri": "amqp://localhost:5672"
    ]

    @Override
    List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return PROPERTIES.keySet().toList()
    }

    @Override
    Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        return Optional.ofNullable(PROPERTIES[name])
    }

    @Override
    List<String> getRequiredProperties(String expression) {
        return []
    }

    @Override
    List<String> getRequiredPropertyEntries() {
        return []
    }

    @Override
    boolean closeAll() {
        return true
    }

    @Override
    boolean closeScope(String id) {
        return true
    }
}
