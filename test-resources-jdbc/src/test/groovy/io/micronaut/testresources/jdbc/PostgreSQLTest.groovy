package io.micronaut.testresources.jdbc

import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
@Property(name = "datasources.default.dialect", value = "POSTGRES")
@Property(name = "datasources.default.driverClassName", value = "org.postgresql.Driver")
class PostgreSQLTest extends AbstractJDBCSpec {
    @Inject
    PostgreSQLBookRepository repository

    def "starts a PostgreSQL container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        "postgres"
    }
}
