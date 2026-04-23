package io.micronaut.testresources.server

import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.env.CommandLinePropertySource
import io.micronaut.context.env.EnvironmentPropertySource
import io.micronaut.context.env.SystemPropertiesPropertySource
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.Specification

class TestResourcesServiceTest extends Specification {
    def "manual bootstrap property sources preserve supported inputs"() {
        when:
        def propertySources = TestResourcesService.defaultPropertySources(["--server.port=9999"] as String[])

        then:
        propertySources*.name == ["application", "system", "env", "cli"]
        propertySources[1] instanceof SystemPropertiesPropertySource
        propertySources[2] instanceof EnvironmentPropertySource
        propertySources[3] instanceof CommandLinePropertySource
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

    def "bundled application properties are loaded for manual bootstrap"() {
        when:
        def propertySource = TestResourcesService.loadBundledApplicationPropertySource()

        then:
        propertySource.get("micronaut.application.name") == "Test resources server"
        propertySource.get("micronaut.server.port") == "-1"
    }

    def "manual bootstrap starts the embedded server with supported property sources"() {
        given:
        ApplicationContext context = null

        when:
        context = TestResourcesService.start([] as String[])
        def server = context.getBean(EmbeddedServer)

        then:
        server.isRunning()
        server.port > 0
        context.environment.activeNames.contains("test")

        cleanup:
        context?.close()
    }
}
