package io.micronaut.testresources.redis

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest
class RedisStartedTest extends AbstractRedisSpec {


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
