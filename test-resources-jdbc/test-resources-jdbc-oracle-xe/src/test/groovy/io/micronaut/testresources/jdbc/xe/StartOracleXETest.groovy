package io.micronaut.testresources.jdbc.xe

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

@MicronautTest
class StartOracleXETest extends AbstractJDBCSpec {
    @Inject
    OracleXEBookRepository repository

    def "starts an Oracle container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        "oracle-xe"
    }
}
