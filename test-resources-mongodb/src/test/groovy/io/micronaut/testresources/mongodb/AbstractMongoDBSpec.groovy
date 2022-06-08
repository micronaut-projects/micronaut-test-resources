package io.micronaut.testresources.mongodb


import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractMongoDBSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'mongodb'
    }

}
