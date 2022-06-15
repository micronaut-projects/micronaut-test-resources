package io.micronaut.testresources.redis


import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject

abstract class AbstractRedisSpec extends AbstractTestContainersSpec {

    @Inject
    protected RedisAccess redisAccess

    @Override
    String getScopeName() {
        'redis'
    }

}
