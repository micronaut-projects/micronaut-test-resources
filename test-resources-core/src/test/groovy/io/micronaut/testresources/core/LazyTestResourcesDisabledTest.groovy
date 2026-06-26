package io.micronaut.testresources.core

import io.micronaut.context.env.PropertyExpressionResolver
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.io.ResourceLoader
import io.micronaut.core.value.PropertyResolver
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

import java.util.stream.Stream

class LazyTestResourcesDisabledTest extends Specification {

    def "lazy expression resolver delegates by default"() {
        given:
        def delegate = new CountingExpressionResolver(Optional.of("resolved"))
        def resolver = new LazyTestResourcesExpressionResolver(delegate)
        def propertyResolver = new TestResourceLoader([:])

        expect:
        resolver.resolve(propertyResolver, ConversionService.SHARED, "auto.test.resources.datasources.default.url", String) == Optional.of("resolved")
        delegate.calls == 1
    }

    @Unroll
    @RestoreSystemProperties
    def "lazy expression resolver does not delegate when disabled by #propertyName"() {
        given:
        if (systemProperty) {
            System.setProperty(propertyName, "false")
        }
        def delegate = new CountingExpressionResolver(Optional.of("resolved"))
        def resolver = new LazyTestResourcesExpressionResolver(delegate)
        def propertyResolver = new TestResourceLoader(systemProperty ? [:] : [(propertyName): value])

        expect:
        resolver.resolve(propertyResolver, ConversionService.SHARED, "auto.test.resources.datasources.default.url", String).empty
        delegate.calls == 0

        where:
        propertyName                         | value   | systemProperty
        "test-resources.enabled"            | false   | false
        "test-resources.enabled"            | "false"| false
        "micronaut.test.resources.enabled"  | false   | false
        "micronaut.test.resources.enabled"  | "false"| false
        "micronaut.test.resources.enabled"  | null    | true
    }

    def "lazy property source loader produces keys by default"() {
        given:
        def producer = new CountingProducer(["datasources.default.url"])
        def loader = new LazyTestResourcesPropertySourceLoader(producer)
        def resourceLoader = new TestResourceLoader([:])

        expect:
        loader.load("test-resources", resourceLoader).orElseThrow().iterator().toList() == ["datasources.default.url"]
        producer.calls == 1
    }

    @Unroll
    @RestoreSystemProperties
    def "lazy property source loader does not produce keys when disabled by #propertyName"() {
        given:
        if (systemProperty) {
            System.setProperty(propertyName, "false")
        }
        def producer = new CountingProducer(["datasources.default.url"])
        def loader = new LazyTestResourcesPropertySourceLoader(producer)
        def resourceLoader = new TestResourceLoader(systemProperty ? [:] : [(propertyName): value])

        expect:
        loader.load("test-resources", resourceLoader).orElseThrow().iterator().toList() == []
        producer.calls == 0

        where:
        propertyName                         | value   | systemProperty
        "test-resources.enabled"            | false   | false
        "test-resources.enabled"            | "false"| false
        "micronaut.test.resources.enabled"  | false   | false
        "micronaut.test.resources.enabled"  | "false"| false
        "micronaut.test.resources.enabled"  | null    | true
    }

    private static final class CountingExpressionResolver implements PropertyExpressionResolver {
        private final Optional<String> result
        int calls

        private CountingExpressionResolver(Optional<String> result) {
            this.result = result
        }

        @Override
        <T> Optional<T> resolve(PropertyResolver propertyResolver, ConversionService conversionService, String expression, Class<T> requiredType) {
            calls++
            result.flatMap(value -> conversionService.convert(value, requiredType))
        }
    }

    private static final class CountingProducer implements PropertyExpressionProducer {
        private final List<String> keys
        int calls

        private CountingProducer(List<String> keys) {
            this.keys = keys
        }

        @Override
        List<String> getPropertyEntries() {
            []
        }

        @Override
        List<String> produceKeys(ResourceLoader resourceLoader, Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            calls++
            keys
        }
    }

    private static final class TestResourceLoader implements ResourceLoader, PropertyResolver {
        private final Map<String, Object> properties

        private TestResourceLoader(Map<String, Object> properties) {
            this.properties = properties
        }

        @Override
        boolean containsProperty(String name) {
            properties.containsKey(name)
        }

        @Override
        boolean containsProperties(String name) {
            properties.keySet().any { it == name || it.startsWith("${name}.") }
        }

        @Override
        <T> Optional<T> getProperty(String name, ArgumentConversionContext<T> conversionContext) {
            def value = properties.get(name)
            if (value == null) {
                return Optional.empty()
            }
            ConversionService.SHARED.convert(value, conversionContext)
        }

        @Override
        Collection<List<String>> getPropertyPathMatches(String pathPattern) {
            []
        }

        @Override
        Collection<String> getPropertyEntries(String name) {
            properties.keySet()
                .findAll { it.startsWith("${name}.") }
                .collect { it.substring(name.length() + 1) }
        }

        @Override
        Map<String, Object> getProperties(String name) {
            properties.findAll { key, ignored -> key.startsWith("${name}.") }
                .collectEntries { key, value -> [(key.substring(name.length() + 1)): value] }
        }

        @Override
        Optional<InputStream> getResourceAsStream(String path) {
            Optional.empty()
        }

        @Override
        Optional<URL> getResource(String path) {
            Optional.empty()
        }

        @Override
        Stream<URL> getResources(String path) {
            Stream.empty()
        }

        @Override
        boolean supportsPrefix(String path) {
            false
        }

        @Override
        ResourceLoader forBase(String basePath) {
            this
        }
    }
}
