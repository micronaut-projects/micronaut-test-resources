package io.micronaut.testresources.azure.cosmos

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject

@MicronautTest
class CosmosStartedTest extends AbstractTestContainersSpec {
    @Inject
    CosmosBookRepository repository

    def "starts a CosmosDB container"() {
        def book = new CosmosBook(null, "Micronaut for Spring developers", 50, "1")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        "cosmosdb"
    }
}
