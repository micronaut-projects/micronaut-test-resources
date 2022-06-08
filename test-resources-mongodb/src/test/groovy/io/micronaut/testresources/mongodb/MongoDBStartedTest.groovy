package io.micronaut.testresources.mongodb


import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class MongoDBStartedTest extends AbstractMongoDBSpec {

    @Inject
    BookRepository bookRepository

    def "automatically starts a MongoDB container"() {
        given:
        def book = new Book(title: "Micronaut for Spring developers")
        bookRepository.save(book)

        when:
        def books = bookRepository.findAll()

        then:
        books.size() == 1
    }

}
