package io.micronaut.testresources.couchbase

import com.couchbase.client.java.Cluster
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.manager.bucket.BucketSettings
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers

import java.time.Duration

class CouchbaseTestResourceProviderTest extends AbstractTestContainersSpec {
    private final CouchbaseTestResourceProvider provider = new CouchbaseTestResourceProvider()
    private final Map<String, Object> requestedProperties = [(Scope.PROPERTY_KEY): scopeName]

    def "starts Couchbase and resolves supported connection properties from the same container"() {
        Cluster cluster = null
        def bucketName = 'testresources'
        def documentId = 'doc-1'

        when:
        def uri = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_URI, requestedProperties, [:]).orElse(null)
        def username = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_USERNAME, requestedProperties, [:]).orElse(null)
        def password = provider.resolve(CouchbaseTestResourceProvider.COUCHBASE_PASSWORD, requestedProperties, [:]).orElse(null)
        def scope = Scope.of(scopeName)
        def uriContainers = TestContainers.findByRequestedProperty(scope, CouchbaseTestResourceProvider.COUCHBASE_URI)
        def usernameContainers = TestContainers.findByRequestedProperty(scope, CouchbaseTestResourceProvider.COUCHBASE_USERNAME)
        def passwordContainers = TestContainers.findByRequestedProperty(scope, CouchbaseTestResourceProvider.COUCHBASE_PASSWORD)
        cluster = Cluster.connect(uri, username, password)
        cluster.buckets().createBucket(BucketSettings.create(bucketName).ramQuotaMB(100))
        def bucket = cluster.bucket(bucketName)
        bucket.waitUntilReady(Duration.ofMinutes(2))
        def collection = bucket.defaultCollection()
        collection.upsert(documentId, JsonObject.create().put('title', 'Micronaut Test Resources'))
        def stored = collection.get(documentId)

        then:
        uri?.startsWith('couchbase://')
        username
        password
        stored.contentAsObject().getString('title') == 'Micronaut Test Resources'

        and:
        uriContainers.size() == 1
        usernameContainers == uriContainers
        passwordContainers == uriContainers
        TestContainers.listByScope(scopeName).values().flatten().size() == 1

        cleanup:
        cluster?.disconnect()
    }
}
