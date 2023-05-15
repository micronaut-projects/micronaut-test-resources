package io.micronaut.testresources.client

/**
 * A fixture used to undo the side effect of caching the
 * test resources client, since in this project the test
 * server is not shared between tests.
 */
trait ClientCleanup {
    def cleanupSpec() {
        TestResourcesClientFactory.cachedClient = null
    }
}
