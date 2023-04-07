package io.micronaut.testresources.redis

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(environments = "cluster")
class RedisClusterStartedTest extends AbstractRedisSpec {

    @Inject
    protected RedisClusterAccess redisAccess

    def "automatically starts a Redis cluster"() {
        given:
        redisAccess.withClient {
            it.set("foo", "bar")
        }

        when:
        def value = redisAccess.withClient {
            it.get("foo")
        }

        then:
        value == 'bar'
        listContainers().size() == 1
    }

}
