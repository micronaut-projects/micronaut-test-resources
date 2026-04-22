package io.micronaut.testresources.server

import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.env.CachedEnvironment
import spock.lang.Specification

import java.lang.reflect.Field
import java.util.function.UnaryOperator

class TestResourcesServiceTest extends Specification {
    def "sanitizing getenv hides MICRONAUT_CONFIG_FILES and delegates other keys"() {
        given:
        def getenv = TestResourcesService.configFilesSanitizingGetenv { String key ->
            "value-for-" + key
        } as UnaryOperator<String>

        expect:
        getenv.apply("MICRONAUT_CONFIG_FILES") == null
        getenv.apply("OTHER_ENV") == "value-for-OTHER_ENV"
    }

    def "configurer excludes MICRONAUT_CONFIG_FILES from environment variables"() {
        given:
        def builder = Mock(ApplicationContextBuilder)

        when:
        new TestResourcesService.Configurer().configure(builder)

        then:
        1 * builder.packages("io.micronaut.testresources.server") >> builder
        1 * builder.deduceEnvironment(false) >> builder
        1 * builder.environments("test") >> builder
        1 * builder.environmentVariableExcludes("MICRONAUT_CONFIG_FILES") >> builder
        1 * builder.banner(false) >> builder
        0 * _
    }

    def "installing the cached environment override replaces the getenv operator"() {
        given:
        Field getenvField = CachedEnvironment.class.getDeclaredField("getenv")
        getenvField.setAccessible(true)
        UnaryOperator<String> original = (UnaryOperator<String>) getenvField.get(null)
        UnaryOperator<String> existing = { String key -> "existing-" + key } as UnaryOperator<String>
        getenvField.set(null, existing)

        when:
        TestResourcesService.installCachedEnvironmentConfigFilesOverride()
        UnaryOperator<String> overridden = (UnaryOperator<String>) getenvField.get(null)

        then:
        overridden.apply("MICRONAUT_CONFIG_FILES") == null
        overridden.apply("OTHER_ENV") == "existing-OTHER_ENV"

        cleanup:
        getenvField.set(null, original)
    }
}
