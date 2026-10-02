package io.micronaut.testresources.client

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import static io.micronaut.testresources.client.ConfigFinder.systemPropertyNameOf

@MicronautTest
class CachedClientDisabledTest extends Specification implements ClientCleanup {

    private static final String SERVER_URI = systemPropertyNameOf(TestResourcesClient.SERVER_URI)
    private static final String ENABLED = systemPropertyNameOf(TestResourcesClient.ENABLED)

    @Inject
    EmbeddedServer server

    def cleanup() {
        TestResourcesClientFactory.cachedClient = null
    }

    @RestoreSystemProperties
    def "enabled=false wins over a client the factory has already cached"() {
        given: 'a client created and cached through the factory'
        System.setProperty(SERVER_URI, server.URI.toString())
        def first = TestResourcesClientFactory.fromSystemProperties().orElseThrow()

        expect:
        first instanceof DefaultTestResourcesClient
        TestResourcesClientFactory.fromSystemProperties().orElseThrow().is(first)

        when: 'Test Resources is disabled programmatically'
        System.setProperty(ENABLED, 'false')

        then: 'the factory returns the no-op client'
        TestResourcesClientFactory.fromSystemProperties().orElseThrow() instanceof NoOpClient
        TestResourcesClientFactory.findByConvention().orElseThrow() instanceof NoOpClient

        when: 'it is enabled again'
        System.clearProperty(ENABLED)

        then: 'the cached client is reused'
        TestResourcesClientFactory.fromSystemProperties().orElseThrow().is(first)
    }

    @RestoreSystemProperties
    def "a second application context does not use Test Resources once it is disabled"() {
        given: 'a first context that resolves a property through Test Resources'
        System.setProperty(SERVER_URI, server.URI.toString())
        def first = ApplicationContext.builder().properties(server: 'false').start()
        assert first.getProperty('dummy1', String).get() == 'value for dummy1'

        when: 'Test Resources is disabled and a second context starts in the same JVM'
        System.setProperty(ENABLED, 'false')
        def second = ApplicationContext.builder().properties(server: 'false').start()

        then: 'the second context neither uses the client nor gets the property'
        second.getProperty('dummy1', String) == Optional.empty()
        TestResourcesClientFactory.extractFrom(second) instanceof NoOpClient

        and: 'the first context keeps the client it already has'
        first.getProperty('dummy2', String).get() == 'value for dummy2'

        cleanup:
        second?.close()
        first?.close()
    }
}
