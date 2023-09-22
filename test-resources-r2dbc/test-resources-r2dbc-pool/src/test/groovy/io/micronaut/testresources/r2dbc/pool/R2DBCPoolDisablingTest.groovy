package io.micronaut.testresources.r2dbc.pool

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import io.r2dbc.spi.ConnectionFactory
import jakarta.inject.Inject

@MicronautTest(environments = ["r2dbc-pool-disabled"], transactional = false )
class R2DBCPoolDisablingTest extends AbstractJDBCSpec {

    @Inject
    ReactiveBookRepository repository

    @Inject
    ConnectionFactory connectionFactory

    def "starts a reactive PostgreSQL container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book).block()

        when:
        def books = repository.findAll().toIterable() as List<Book>

        then:
        connectionFactory.class.simpleName == 'PostgresqlConnectionFactory'
        books.size() == 1
    }

    @Override
    String getImageName() {
        "postgres"
    }
}
