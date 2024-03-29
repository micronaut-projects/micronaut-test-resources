package io.micronaut.testresources.redis

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class RedisStartedTest extends AbstractRedisSpec {

    @Inject
    protected RedisAccess redisAccess

    def "automatically starts a Redis container"() {
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
