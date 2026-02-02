package io.micronaut.testresources.hazelcast

import io.micronaut.cache.CacheManager
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class HazelcastStartedTest extends AbstractHazelcastSpec {

    @Value('${hazelcast.client.network.addresses}')
    List<String> addresses

    @Inject
    CacheManager<?> cacheManager

    def "test hazelcast starts"() {
        expect:
        addresses != null
        !addresses.isEmpty()
        addresses[0].contains(':')

        when:
        cacheManager.getCache("my-cache").put("foo", "bar")

        then:
        cacheManager.getCache("my-cache").get("foo", String).get() == "bar"
    }
}
