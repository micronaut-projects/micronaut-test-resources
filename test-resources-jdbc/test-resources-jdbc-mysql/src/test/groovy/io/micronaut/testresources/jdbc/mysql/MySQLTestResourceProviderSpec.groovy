package io.micronaut.testresources.jdbc.mysql

import io.micronaut.testresources.mysql.MySQLTestResourceProvider
import org.testcontainers.mysql.MySQLContainer
import spock.lang.Specification

class MySQLTestResourceProviderSpec extends Specification {
    def cleanup() {
        Thread.interrupted()
    }

    void "createAdditionalDatabase restores the interrupt status"() {
        given:
        def provider = new MySQLTestResourceProvider()
        def container = Mock(MySQLContainer)
        container.getUsername() >> "root"
        container.getPassword() >> "secret"
        container.execInContainer(*_) >> { throw new InterruptedException("boom") }

        when:
        provider.createAdditionalDatabase(container, "extra_db")

        then:
        def e = thrown(IllegalStateException)
        e.cause instanceof InterruptedException
        Thread.currentThread().isInterrupted()
    }
}
