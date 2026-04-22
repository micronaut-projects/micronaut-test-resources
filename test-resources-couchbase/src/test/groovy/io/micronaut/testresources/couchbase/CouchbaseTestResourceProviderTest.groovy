package io.micronaut.testresources.couchbase

import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers

class CouchbaseTestResourceProviderTest extends AbstractTestContainersSpec {
    private final CouchbaseTestResourceProvider provider = new CouchbaseTestResourceProvider()
    private final Map<String, Object> requestedProperties = [(Scope.PROPERTY_KEY): scopeName]

    def "starts Couchbase and resolves supported connection properties from the same container"() {
        when:
        def uri = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_URI, requestedProperties, [:]).orElse(null)
        def username = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_USERNAME, requestedProperties, [:]).orElse(null)
        def password = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_PASSWORD, requestedProperties, [:]).orElse(null)
        def scope = Scope.of(scopeName)
        def uriContainers = TestContainers.findByRequestedProperty(scope, CouchbaseTestResourceProvider.COUCHBASE_URI)
        def usernameContainers = TestContainers.findByRequestedProperty(scope, CouchbaseTestResourceProvider.COUCHBASE_USERNAME)
        def passwordContainers = TestContainers.findByRequestedProperty(scope, CouchbaseTestResourceProvider.COUCHBASE_PASSWORD)

        then:
        uri?.startsWith('couchbase://')
        username
        password

        and:
        uriContainers.size() == 1
        usernameContainers == uriContainers
        passwordContainers == uriContainers
        TestContainers.listByScope(scopeName).values().flatten().size() == 1
    }
}
