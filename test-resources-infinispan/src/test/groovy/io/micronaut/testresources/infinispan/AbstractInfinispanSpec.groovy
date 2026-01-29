package io.micronaut.testresources.infinispan

import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec;

abstract class AbstractInfinispanSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'infinispan'
    }
}
