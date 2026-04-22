package io.micronaut.testresources.couchbase

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import spock.lang.Specification

class CouchbaseTestResourceProviderTest extends Specification {
    private final CouchbaseTestResourceProvider provider = new CouchbaseTestResourceProvider()
    private final Map<String, Object> requestedProperties = [(Scope.PROPERTY_KEY): 'couchbase']

    void cleanupSpec() {
        TestContainers.closeScope('couchbase')
    }

    def "starts Couchbase and resolves supported connection properties from the same container"() {
        when:
        def uri = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_URI, requestedProperties, [:]).orElse(null)
        def username = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_USERNAME, requestedProperties, [:]).orElse(null)
        def password = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_PASSWORD, requestedProperties, [:]).orElse(null)
        def uriContainers = TestContainers.findByRequestedProperty(Scope.of('couchbase'), CouchbaseTestResourceProvider.COUCHBASE_URI)
        def usernameContainers = TestContainers.findByRequestedProperty(Scope.of('couchbase'), CouchbaseTestResourceProvider.COUCHBASE_USERNAME)
        def passwordContainers = TestContainers.findByRequestedProperty(Scope.of('couchbase'), CouchbaseTestResourceProvider.COUCHBASE_PASSWORD)

        then:
        uri?.startsWith('couchbase://')
        username
        password

        and:
        uriContainers.size() == 1
        usernameContainers == uriContainers
        passwordContainers == uriContainers
        TestContainers.listByScope('couchbase').values().flatten().size() == 1
    }
}
