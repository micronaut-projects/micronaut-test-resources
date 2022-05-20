package io.micronaut.testresources.mysql

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class StartMySQLTest extends AbstractMySQLSpec {
    @Inject
    BookRepository repository

    def "starts a MySQL container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 1
    }
}
