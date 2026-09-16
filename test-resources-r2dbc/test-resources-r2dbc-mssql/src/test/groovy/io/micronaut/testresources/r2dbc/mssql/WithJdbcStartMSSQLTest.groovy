package io.micronaut.testresources.r2dbc.mssql

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject
import spock.lang.IgnoreIf

// r2dbc-mssql 1.0.4 fails TLS certificate hostname validation during login on Java 25.
@IgnoreIf({ System.getProperty("java.specification.version").toBigDecimal() >= 25 })
@MicronautTest(environments = ["jdbc"], transactional = false )
class WithJdbcStartMSSQLTest extends AbstractJDBCSpec {

    @Inject
    ReactiveBookRepository repository

    def "starts a reactive MS SQL container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book).block()

        when:
        def books = repository.findAll().toIterable() as List<Book>

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        "mssql"
    }
}
