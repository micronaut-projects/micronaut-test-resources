package io.micronaut.testresources.hazelcast

import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractHazelcastSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'hazelcast'
    }
}
