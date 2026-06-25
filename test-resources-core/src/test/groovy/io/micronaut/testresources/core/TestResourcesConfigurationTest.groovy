package io.micronaut.testresources.core

import io.micronaut.context.env.PropertyExpressionResolver
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

class TestResourcesConfigurationTest extends Specification {

    @RestoreSystemProperties
    void "system property disables lazy property source loading"() {
        given:
        System.setProperty(TestResourcesConfiguration.ENABLED, "false")
        def loader = new LazyTestResourcesPropertySourceLoader({ resourceLoader, propertyEntries, testResourcesConfig ->
            throw new AssertionError("producer should not be used")
        } as PropertyExpressionProducer)

        expect:
        loader.load("application", null).empty
    }

    @RestoreSystemProperties
    void "system property disables lazy expression resolution"() {
        given:
        System.setProperty(TestResourcesConfiguration.ENABLED, "false")
        def resolver = new LazyTestResourcesExpressionResolver({ propertyResolver, conversionService, expression, requiredType ->
            throw new AssertionError("delegate should not be used")
        } as PropertyExpressionResolver)

        expect:
        resolver.resolve(null, null, "${LazyTestResourcesExpressionResolver.PLACEHOLDER_PREFIX}datasources.default.url", String).empty
    }

    @RestoreSystemProperties
    void "lazy property source loading stays enabled by default"() {
        given:
        def loader = new LazyTestResourcesPropertySourceLoader({ resourceLoader, propertyEntries, testResourcesConfig ->
            []
        } as PropertyExpressionProducer)

        expect:
        loader.load("application", null).present
    }
}
