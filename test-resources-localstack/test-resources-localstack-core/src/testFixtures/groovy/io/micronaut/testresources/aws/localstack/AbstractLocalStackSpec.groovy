package io.micronaut.testresources.aws.localstack


import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractLocalStackSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'localstack'
    }

    @Override
    String getImageName() {
        'localstack'
    }

}
