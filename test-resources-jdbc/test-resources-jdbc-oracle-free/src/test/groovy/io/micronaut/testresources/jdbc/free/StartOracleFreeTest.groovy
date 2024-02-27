package io.micronaut.testresources.jdbc.free

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

@MicronautTest
class StartOracleFreeTest extends AbstractJDBCSpec {
    @Inject
    OracleFreeBookRepository repository

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
        "oracle-free"
    }
}
