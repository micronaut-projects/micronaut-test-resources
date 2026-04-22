package io.micronaut.testresources.postgres

import org.testcontainers.postgresql.PostgreSQLContainer
import spock.lang.Specification

class PostgreSQLTestResourceProviderSpec extends Specification {
    def cleanup() {
        Thread.interrupted()
    }

    void "createAdditionalDatabase restores the interrupt status"() {
        given:
        def provider = new PostgreSQLTestResourceProvider()
        def container = Mock(PostgreSQLContainer)
        container.getUsername() >> "postgres"
        container.getDatabaseName() >> "postgres"
        container.execInContainer(*_) >> { throw new InterruptedException("boom") }

        when:
        provider.createAdditionalDatabase(container, "extra_db")

        then:
        def e = thrown(IllegalStateException)
        e.cause instanceof InterruptedException
        Thread.currentThread().isInterrupted()
    }
}
