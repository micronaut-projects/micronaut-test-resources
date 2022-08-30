package io.micronaut.testresources.r2dbc.mariadb

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

@MicronautTest(environments = ["customized-jdbc"] )
class JdbcWithCustomizationMariaDBSQLTest extends AbstractJDBCSpec {

    @Inject
    ReactiveBookRepository repository

    @Value("\${r2dbc.datasources.default.username}")
    String username

    @Value("\${r2dbc.datasources.default.password}")
    String password

    @Value("\${r2dbc.datasources.default.url}")
    String url

    def "starts a reactive MariaDB container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book).block()

        when:
        def books = repository.findAll().toIterable() as List<Book>

        then:
        books.size() == 2
        books.find { it.title == "Understanding cats" }

        and:
        username == 'sherlock'
        password == 'holmes'
        url.endsWith("/howdy")
    }

    @Override
    String getImageName() {
        "mariadb"
    }
}
