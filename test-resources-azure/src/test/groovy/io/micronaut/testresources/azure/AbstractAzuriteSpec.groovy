package io.micronaut.testresources.azure

import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractAzuriteSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'azurite'
    }
}
