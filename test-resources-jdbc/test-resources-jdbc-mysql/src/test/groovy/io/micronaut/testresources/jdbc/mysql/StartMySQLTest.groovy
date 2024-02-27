package io.micronaut.testresources.jdbc.mysql

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import io.micronaut.testresources.mysql.MySQLTestResourceProvider
import jakarta.inject.Inject

@MicronautTest
class StartMySQLTest extends AbstractJDBCSpec {
    @Inject
    MySQLBookRepository repository

    def "starts a MySQL container"() {
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 1
    }

    @Override
    String getImageName() {
        MySQLTestResourceProvider.MYSQL_OFFICIAL_IMAGE
    }
}
