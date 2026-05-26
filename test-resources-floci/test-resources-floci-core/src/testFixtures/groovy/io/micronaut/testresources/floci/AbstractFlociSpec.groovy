package io.micronaut.testresources.floci


import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractFlociSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'floci'
    }

    @Override
    String getImageName() {
        'floci'
    }

}
