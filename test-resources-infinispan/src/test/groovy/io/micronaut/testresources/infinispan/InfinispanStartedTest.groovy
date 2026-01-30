package io.micronaut.testresources.infinispan

import io.micronaut.cache.CacheManager
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.infinispan.testcontainers.InfinispanContainer

@MicronautTest
class InfinispanStartedTest extends AbstractInfinispanSpec {

    @Value('${infinispan.client.hotrod.server.host}')
    String host

    @Value('${infinispan.client.hotrod.server.port}')
    int port

    @Value('${infinispan.client.hotrod.security.authentication.username}')
    String username

    @Value('${infinispan.client.hotrod.security.authentication.password}')
    String password

    @Inject
    CacheManager<?> cacheManager


    def "test infinispan starts"() {
        expect:
        host != null
        port > 0
        username == InfinispanContainer.DEFAULT_USERNAME
        password == InfinispanContainer.DEFAULT_PASSWORD

        when:
        cacheManager.getCache("my-cache").put("foo", "bar")

        then:
        cacheManager.getCache("my-cache").get("foo", String).get() == "bar"
    }
}
