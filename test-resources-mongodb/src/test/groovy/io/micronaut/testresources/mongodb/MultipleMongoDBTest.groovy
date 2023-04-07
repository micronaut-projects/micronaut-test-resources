package io.micronaut.testresources.mongodb

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest(environments = "multiple-servers")
class MultipleMongoDBTest extends AbstractMongoDBSpec {

    @Inject
    BookRepository bookRepository

    @Inject
    UserRepository userRepository

    @Value("\${mongodb.servers.default.uri}")
    String mongodbDefaultUri

    @Value("\${mongodb.servers.another.uri}")
    String mongodbAnotherUri

    def "automatically starts a MongoDB container"() {
        given:
        def book = new Book(title: "Micronaut for Spring developers")
        bookRepository.save(book)

        def user = new User(name: "Cédric Champeau")
        userRepository.save(user)

        when:
        def books = bookRepository.findAll()
        def users = userRepository.findAll()

        then:
        books.size() == 1
        users.size() == 1

        and:
        mongodbDefaultUri.endsWith('/default')
        mongodbAnotherUri.endsWith('/another')
    }

}
