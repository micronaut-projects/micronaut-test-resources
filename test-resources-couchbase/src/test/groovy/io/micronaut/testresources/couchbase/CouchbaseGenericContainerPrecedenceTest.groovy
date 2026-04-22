package io.micronaut.testresources.couchbase

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.GenericTestContainerProvider
import io.micronaut.testresources.testcontainers.TestContainers
import spock.lang.Specification

class CouchbaseGenericContainerPrecedenceTest extends Specification {
    void cleanup() {
        TestContainers.closeAll()
    }

    def "explicit generic container mappings win for supported Couchbase properties"() {
        given:
        def provider = new CouchbaseTestResourceProvider()
        def genericProvider = new GenericTestContainerProvider()
        def config = [
            'containers.explicit.image-name'   : 'couchbase/server',
            'containers.explicit.exposed-ports': [[(CouchbaseTestResourceProvider.COUCHBASE_URI): 8091]],
            'containers.explicit.hostnames'    : [
                CouchbaseTestResourceProvider.COUCHBASE_USERNAME,
                CouchbaseTestResourceProvider.COUCHBASE_PASSWORD
            ]
        ]

        expect:
        !provider.getResolvableProperties([:], config).contains(CouchbaseTestResourceProvider.COUCHBASE_URI)
        !provider.getResolvableProperties([:], config).contains(CouchbaseTestResourceProvider.COUCHBASE_USERNAME)
        !provider.getResolvableProperties([:], config).contains(CouchbaseTestResourceProvider.COUCHBASE_PASSWORD)

        and:
        genericProvider.getResolvableProperties([:], config).containsAll([
            CouchbaseTestResourceProvider.COUCHBASE_URI,
            CouchbaseTestResourceProvider.COUCHBASE_USERNAME,
            CouchbaseTestResourceProvider.COUCHBASE_PASSWORD
        ])

        and:
        provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_URI, [(Scope.PROPERTY_KEY): 'generic-precedence'], config).isEmpty()
        TestContainers.listAll().isEmpty()
    }
}
