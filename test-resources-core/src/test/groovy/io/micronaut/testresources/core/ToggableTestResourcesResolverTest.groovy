package io.micronaut.testresources.core

import spock.lang.Specification
import spock.lang.Unroll

class ToggableTestResourcesResolverTest extends Specification {
    private final ToggableTestResourcesResolver resolver = new TestResolver()

    void "missing config keeps provider enabled"() {
        expect:
        resolver.isEnabled([:])
    }

    @Unroll
    void "false value #value disables provider"() {
        expect:
        !resolver.isEnabled(["kafka.enabled": value])

        where:
        value << [false, "false"]
    }

    @Unroll
    void "truthy value #value keeps provider enabled"() {
        expect:
        resolver.isEnabled(["kafka.enabled": value])

        where:
        value << [true, "true"]
    }

    private static final class TestResolver implements ToggableTestResourcesResolver {
        @Override
        String getName() {
            return "kafka"
        }

        @Override
        String getDisplayName() {
            return "Apache Kafka"
        }

        @Override
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return []
        }

        @Override
        Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            return Optional.empty()
        }
    }
}
