package io.micronaut.testresources.redis


import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject

abstract class AbstractRedisSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'redis'
    }

}
