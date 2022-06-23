package io.micronaut.testresources.testcontainers

import org.testcontainers.containers.GenericContainer
import spock.lang.Specification

class AbstractTestContainersProviderTest extends Specification {
    def "test resources configuration is passed to the shouldAnswer and createContainer method"() {
        def provider = Mock(AbstractTestContainersProvider) {
            getDefaultImageName() >> 'my-image'
        }

        when:
        provider.resolve('foo', [request: 'value'], ['test-resources.foo': 'config'])

        then:
        1 * provider.createContainer(_, [request: 'value'], ['test-resources.foo': 'config']) >> Stub(GenericContainer)
        1 * provider.resolveWithoutContainer('foo', [request: 'value'], ['test-resources.foo': 'config']) >> Optional.empty()
        1 * provider.shouldAnswer('foo', [request: 'value'], ['test-resources.foo': 'config']) >> true
    }
}
