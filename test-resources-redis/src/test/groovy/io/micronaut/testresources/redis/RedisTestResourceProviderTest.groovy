package io.micronaut.testresources.redis

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.containers.wait.strategy.WaitStrategy
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class RedisTestResourceProviderTest extends Specification {

    def "single-node Redis defaults to a listening-port wait strategy"() {
        given:
        def provider = new RedisTestResourceProvider()

        when:
        def container = provider.createContainer(DockerImageName.parse(RedisTestResourceProvider.DEFAULT_IMAGE), [:], [:])

        then:
        waitStrategyOf(container) instanceof HostPortWaitStrategy
    }

    def "explicit Redis wait strategy overrides the provider default"() {
        given:
        def provider = new RedisTestResourceProvider()
        def config = [
                'containers.redis.wait-strategy.log.regex': '.*Ready to accept connections.*'
        ]

        when:
        def container = provider.createContainer(DockerImageName.parse(RedisTestResourceProvider.DEFAULT_IMAGE), [:], config)
        applyRedisMetadata(container, config)

        then:
        waitStrategyOf(container) instanceof LogMessageWaitStrategy
    }

    private static void applyRedisMetadata(GenericContainer<?> container, Map<String, Object> config) {
        def metadataSupportClass = Class.forName('io.micronaut.testresources.testcontainers.TestContainerMetadataSupport')
        def metadataClass = Class.forName('io.micronaut.testresources.testcontainers.TestContainerMetadata')
        def convertToMetadata = metadataSupportClass.getDeclaredMethod('convertToMetadata', Map, String)
        convertToMetadata.accessible = true
        def metadata = convertToMetadata.invoke(null, config, RedisTestResourceProvider.SIMPLE_NAME).get()
        def applyMetadata = metadataSupportClass.getDeclaredMethod('applyMetadata', metadataClass, GenericContainer)
        applyMetadata.accessible = true
        applyMetadata.invoke(null, metadata, container)
    }

    private static WaitStrategy waitStrategyOf(GenericContainer<?> container) {
        def getWaitStrategy = GenericContainer.getDeclaredMethod('getWaitStrategy')
        getWaitStrategy.accessible = true
        getWaitStrategy.invoke(container) as WaitStrategy
    }
}
