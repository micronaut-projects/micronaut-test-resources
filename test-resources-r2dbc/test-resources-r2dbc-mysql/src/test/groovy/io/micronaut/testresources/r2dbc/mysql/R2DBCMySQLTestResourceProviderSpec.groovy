package io.micronaut.testresources.r2dbc.mysql

import org.testcontainers.mysql.MySQLContainer
import spock.lang.Specification

class R2DBCMySQLTestResourceProviderSpec extends Specification {
    def cleanup() {
        Thread.interrupted()
    }

    void "createAdditionalDatabase restores the interrupt status"() {
        given:
        def provider = new R2DBCMySQLTestResourceProvider()
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
