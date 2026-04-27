package io.micronaut.testresources.r2dbc.mariadb

import org.testcontainers.mariadb.MariaDBContainer
import spock.lang.Specification

class R2DBCMariaDBTestResourceProviderSpec extends Specification {
    def cleanup() {
        Thread.interrupted()
    }

    void "createAdditionalDatabase restores the interrupt status"() {
        given:
        def provider = new R2DBCMariaDBTestResourceProvider()
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
