package io.micronaut.testresources.jdbc.mariadb

import io.micronaut.testresources.mariadb.MariaDBTestResourceProvider
import org.testcontainers.mariadb.MariaDBContainer
import spock.lang.Specification

class MariaDBTestResourceProviderSpec extends Specification {
    def cleanup() {
        Thread.interrupted()
    }

    void "createAdditionalDatabase restores the interrupt status"() {
        given:
        def provider = new MariaDBTestResourceProvider()
        def container = Mock(MariaDBContainer)
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
