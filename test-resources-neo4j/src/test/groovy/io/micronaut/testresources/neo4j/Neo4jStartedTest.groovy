package io.micronaut.testresources.neo4j

import io.micronaut.test.extensions.spock.annotation.MicronautTest

@MicronautTest
class Neo4jStartedTest extends AbstractNeo4jDBSpec {


    def "automatically starts a Neo4j container"() {
        given:
        def book = new Book(title: "Micronaut for Spring developers")
        bookRepository.save(book)

        when:
        def books = bookRepository.findAll()

        then:
        books.size() == 1
    }

}
