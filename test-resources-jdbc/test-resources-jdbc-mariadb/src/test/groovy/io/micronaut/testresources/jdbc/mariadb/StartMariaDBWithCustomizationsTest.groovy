package io.micronaut.testresources.jdbc.mariadb

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import jakarta.inject.Inject

@MicronautTest(environments = ["test", "init-script"])
class StartMariaDBWithCustomizationsTest extends AbstractJDBCSpec {
    @Inject
    MariaDBBookRepository repository

    @Value("\${datasources.default.username}")
    String username

    @Value("\${datasources.default.password}")
    String password

    @Value("\${datasources.default.url}")
    String url


    def "starts a MariaDB container with customizations"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 2
        books.find { it.title == "Understanding cats" }

        and:
        username == 'sherlock'
        password == 'holmes'
        url.endsWith("/howdy")
    }

    @Override
    String getImageName() {
        "mariadb"
    }
}
