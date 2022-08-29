package io.micronaut.testresources.mongodb

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(environments = ["test", "custom-db-name"])
class CustomDbNameMongoDBTest extends AbstractMongoDBSpec {

    @Inject
    BookRepository bookRepository

    @Value("\${mongodb.uri}")
    String mongodbUri

    def "automatically starts a MongoDB container"() {
        given:
        def book = new Book(title: "Micronaut for Spring developers")
        bookRepository.save(book)

        when:
        def books = bookRepository.findAll()

        then:
        books.size() == 1

        and:
        mongodbUri.endsWith "/custom-db-name"
    }

}
