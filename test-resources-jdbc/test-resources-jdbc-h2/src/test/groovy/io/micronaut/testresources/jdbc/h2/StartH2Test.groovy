package io.micronaut.testresources.jdbc.h2

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

import javax.sql.DataSource

@MicronautTest
class StartH2Test extends spock.lang.Specification {
    @Inject
    H2BookRepository repository

    @Inject
    DataSource dataSource

    def "starts a containerless H2 server"() {
        given:
        def book = new Book(title: "Micronaut Test Resources")
        repository.save(book)

        when:
        def books = repository.findAll()
        def connection = dataSource.connection

        then:
        books.size() == 1
        connection.metaData.URL.startsWith('jdbc:h2:tcp://localhost:')

        cleanup:
        connection?.close()
    }
}
