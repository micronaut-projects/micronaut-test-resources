package io.micronaut.testresources.testcontainers

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class AbstractTestContainersProviderTest extends Specification {
    def "test resources configuration is passed to the shouldAnswer and createContainer method"() {
        def provider = Mock(AbstractTestContainersProvider) {
            getDefaultImageName() >> 'my-image'
            getSimpleName() >> 'test'
            getContainerOwnerKey(_, _, _) >> AbstractTestContainersProviderTest.name
            getContainerQuery(_, _, _) >> [request: 'value']
            getDefaultStartupTimeout(_, _) >> Optional.empty()
        }

        when:
        provider.resolve('foo', [request: 'value'], ['test-resources.foo': 'config'])

        then:
        1 * provider.createContainer(_, [request: 'value'], ['test-resources.foo': 'config']) >> Stub(GenericContainer)
        1 * provider.resolveWithoutContainer('foo', [request: 'value'], ['test-resources.foo': 'config']) >> Optional.empty()
        1 * provider.shouldAnswer('foo', [request: 'value'], ['test-resources.foo': 'config']) >> true
    }

    def "compose configuration does not disable provider unless compose module advertises replacement"() {
        given:
        def provider = new DefaultTestResourceProvider()

        expect:
        provider.isEnabled(['compose.enabled': true])
    }

    private static class DefaultTestResourceProvider extends AbstractTestContainersProvider<GenericContainer> {

        @Override
        protected String getSimpleName() {
            "default"
        }

        @Override
        protected String getDefaultImageName() {
            "default:latest"
        }

        @Override
        protected GenericContainer createContainer(DockerImageName imageName,
                                                   Map<String, Object> requestedProperties,
                                                   Map<String, Object> testResourcesConfig) {
            throw new UnsupportedOperationException()
        }

        @Override
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                             Map<String, Object> testResourcesConfig) {
            []
        }

        @Override
        protected Optional<String> resolveProperty(String propertyName, GenericContainer container) {
            Optional.empty()
        }
    }

    private static class DefaultComposeTestResourceProvider extends DefaultTestResourceProvider implements ComposeAwareTestResourcesResolver {
    }
}
