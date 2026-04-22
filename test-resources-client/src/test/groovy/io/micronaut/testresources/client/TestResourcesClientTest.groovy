package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.TestResourcesResolutionException
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path

import static io.micronaut.testresources.client.ConfigFinder.systemPropertyNameOf

@MicronautTest
class TestResourcesClientTest extends Specification implements ClientCleanup {

    @TempDir
    Path tempDir

    @Inject
    EmbeddedServer server

    @RestoreSystemProperties
    def "property source loader registers properties from server"() {
        def app = createApplication()

        expect:
        app.getProperty("dummy1", String).get() == 'value for dummy1'
        app.getProperty("dummy2", String).get() == 'value for dummy2'

        when:
        app.getProperty("missing", String).empty

        then:
        TestResourcesResolutionException e = thrown()
        e.message == "Test resources doesn't support resolving expression 'missing'"
    }

    @RestoreSystemProperties
    def "reasonable error message when the server throws an error"() {
        def app = createApplication()

        when:
        app.getProperty("throws", String)

        then:
        TestResourcesException e = thrown()
        e.message == "Test resources service wasn't able to resolve expression 'throws': Something bad happened"
    }

    @RestoreSystemProperties
    def "writes IntelliJ IDEA datasource export when enabled"() {
        given:
        def outputFile = tempDir.resolve("generated/intellij-idea-datasources.xml")
        def app = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': outputFile.toString()
        ])

        when:
        def defaultUrl = app.getRequiredProperty("datasources.default.url", String)
        def defaultUser = app.getRequiredProperty("datasources.default.username", String)
        def defaultPassword = app.getRequiredProperty("datasources.default.password", String)
        def defaultDriver = app.getRequiredProperty("datasources.default.driver-class-name", String)
        def analyticsUrl = app.getRequiredProperty("datasources.analytics.url", String)
        def analyticsUser = app.getRequiredProperty("datasources.analytics.username", String)
        def analyticsPassword = app.getRequiredProperty("datasources.analytics.password", String)
        def analyticsDriver = app.getRequiredProperty("datasources.analytics.driver-class-name", String)

        then:
        defaultUrl == "jdbc:postgresql://localhost:15432/demo"
        defaultUser == "demo_user"
        defaultPassword == "demo_secret"
        defaultDriver == "org.postgresql.Driver"
        analyticsUrl == "jdbc:mysql://localhost:13306/analytics"
        analyticsUser == "analytics_user"
        analyticsPassword == "analytics_secret"
        analyticsDriver == "com.mysql.cj.jdbc.Driver"
        Files.exists(outputFile)

        and:
        def output = Files.readString(outputFile)
        output.contains("#LocalDataSource: default")
        output.contains("#LocalDataSource: analytics")
        output.contains("<password>demo_secret</password>")
        output.contains("<password>analytics_secret</password>")
    }

    @RestoreSystemProperties
    def "overlapping application contexts isolate IntelliJ IDEA exporter state when reusing the cached client"() {
        given:
        def firstOutput = tempDir.resolve("generated/first/intellij-idea-datasources.xml")
        def secondOutput = tempDir.resolve("generated/second/intellij-idea-datasources.xml")
        def firstApp = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': firstOutput.toString()
        ])
        def secondApp = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': secondOutput.toString()
        ])

        when:
        firstApp.getRequiredProperty("datasources.default.url", String)
        firstApp.getRequiredProperty("datasources.default.username", String)
        def cachedClient = TestResourcesClientFactory.fromSystemProperties().orElseThrow()
        resolveDatasource(secondApp, "analytics")
        firstApp.getRequiredProperty("datasources.default.password", String)
        firstApp.getRequiredProperty("datasources.default.driver-class-name", String)
        def reusedClient = TestResourcesClientFactory.fromSystemProperties().orElseThrow()

        then:
        reusedClient.is(cachedClient)
        Files.exists(firstOutput)
        Files.exists(secondOutput)

        and:
        def firstExport = Files.readString(firstOutput)
        firstExport.contains("#LocalDataSource: default")
        !firstExport.contains("#LocalDataSource: analytics")

        and:
        def secondExport = Files.readString(secondOutput)
        secondExport.contains("#LocalDataSource: analytics")
        !secondExport.contains("#LocalDataSource: default")

        cleanup:
        secondApp?.close()
        firstApp?.close()
    }

    @RestoreSystemProperties
    def "overlapping application contexts sharing an IntelliJ IDEA output path merge live datasource exports"() {
        given:
        def sharedOutput = tempDir.resolve("generated/shared/intellij-idea-datasources.xml")
        def firstApp = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': sharedOutput.toString()
        ])
        def secondApp = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': sharedOutput.toString()
        ])

        when:
        resolveDatasource(firstApp, "default")
        def cachedClient = TestResourcesClientFactory.fromSystemProperties().orElseThrow()
        resolveDatasource(secondApp, "analytics")
        def reusedClient = TestResourcesClientFactory.fromSystemProperties().orElseThrow()

        then:
        reusedClient.is(cachedClient)
        Files.exists(sharedOutput)

        and:
        def combinedExport = Files.readString(sharedOutput)
        combinedExport.contains("#LocalDataSource: default")
        combinedExport.contains("#LocalDataSource: analytics")

        cleanup:
        secondApp?.close()
        firstApp?.close()
    }

    @RestoreSystemProperties
    def "overlapping application contexts sharing an IntelliJ IDEA output path preserve same-name datasource exports"() {
        given:
        def sharedOutput = tempDir.resolve("generated/shared-default/intellij-idea-datasources.xml")
        def firstApp = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': sharedOutput.toString()
        ])
        def secondApp = createApplication([
            'test-resources.intellij-idea.enabled'    : 'true',
            'test-resources.intellij-idea.output-path': sharedOutput.toString()
        ])

        when:
        resolveDatasource(firstApp, "default")
        def cachedClient = TestResourcesClientFactory.fromSystemProperties().orElseThrow()
        resolveDatasource(secondApp, "default")
        def reusedClient = TestResourcesClientFactory.fromSystemProperties().orElseThrow()

        then:
        reusedClient.is(cachedClient)
        Files.exists(sharedOutput)

        and:
        def combinedExport = Files.readString(sharedOutput)
        combinedExport.contains("#LocalDataSource: default\n")
        combinedExport.contains("#LocalDataSource: default (2)\n")
        combinedExport.readLines().count { it == "#BEGIN#" } == 2

        cleanup:
        secondApp?.close()
        firstApp?.close()
    }

    private ApplicationContext createApplication(Map<String, Object> properties = [:]) {
        System.setProperty(systemPropertyNameOf(TestResourcesClient.SERVER_URI), server.getURI().toString())
        def app = ApplicationContext.builder()
                .properties(['server': 'false'] + properties)
                .start()
        assert !app.findBean(TestServer).present
        return app
    }

    private static void resolveDatasource(ApplicationContext app, String name) {
        app.getRequiredProperty("datasources.${name}.url".toString(), String)
        app.getRequiredProperty("datasources.${name}.username".toString(), String)
        app.getRequiredProperty("datasources.${name}.password".toString(), String)
        app.getRequiredProperty("datasources.${name}.driver-class-name".toString(), String)
    }

}
