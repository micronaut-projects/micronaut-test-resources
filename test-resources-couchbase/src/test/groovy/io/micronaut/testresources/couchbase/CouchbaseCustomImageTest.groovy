package io.micronaut.testresources.couchbase

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.couchbase.CouchbaseContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class CouchbaseCustomImageTest extends Specification {
    void cleanup() {
        TestContainers.closeAll()
    }

    def "applies custom Couchbase image metadata"() {
        given:
        def provider = new CapturingCouchbaseTestResourceProvider()
        def config = [
            'containers.couchbase.image-name': 'custom/couchbase',
            'containers.couchbase.image-tag' : '7.6.5'
        ]

        when:
        def resolved = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_URI, [(Scope.PROPERTY_KEY): 'custom-image'], config)

        then:
        resolved.orElse(null) == 'couchbase://localhost:11210'
        provider.requestedImageName.toString() == 'custom/couchbase:7.6.5'
    }

    private static final class CapturingCouchbaseTestResourceProvider extends CouchbaseTestResourceProvider {
        DockerImageName requestedImageName

        @Override
        protected CouchbaseContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
            requestedImageName = imageName
            return new CouchbaseContainer(imageName) {
                @Override
                void start() {
                }

                @Override
                void stop() {
                }

                @Override
                String getConnectionString() {
                    'couchbase://localhost:11210'
                }
            }
        }
    }
}
