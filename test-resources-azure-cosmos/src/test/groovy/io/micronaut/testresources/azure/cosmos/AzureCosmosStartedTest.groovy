package io.micronaut.testresources.azure.cosmos

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class AzureCosmosStartedTest extends AbstractAzureCosmosSpec {
    @Inject
    CosmosBookRepository repository

    def "starts an Azure Cosmos emulator container"() {
        given:
        def book = new CosmosBook()
        book.title = "Micronaut in Action"
        book.totalPages = 320

        when:
        repository.save(book)
        def books = repository.findAll().toList()

        then:
        listContainers().size() == 1
        books.size() == 1
        books[0].title == "Micronaut in Action"
        books[0].totalPages == 320
        books[0].id != null
    }
}
