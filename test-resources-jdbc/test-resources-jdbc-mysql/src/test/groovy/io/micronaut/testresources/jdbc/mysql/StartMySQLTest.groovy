package io.micronaut.testresources.jdbc.mysql

import com.mysql.cj.xdevapi.SessionFactory
import io.micronaut.context.env.Environment
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.jdbc.AbstractJDBCSpec
import io.micronaut.testresources.jdbc.Book
import io.micronaut.testresources.mysql.MySQLTestResourceProvider
import jakarta.inject.Inject

@MicronautTest
class StartMySQLTest extends AbstractJDBCSpec {

    @Inject
    MySQLBookRepository repository

    @Inject
    Environment environment

    void "starts a MySQL container"() {
        given:
        def book = new Book(title: "Micronaut for Spring developers")
        repository.save(book)

        when:
        def books = repository.findAll()

        then:
        books.size() == 1
    }

    void "resolves the X Protocol URL"() {
        given:
        def xProtocolUrl = environment.getRequiredProperty("datasources.default.x-protocol-url", String)

        when:
        def session = new SessionFactory().getSession(xProtocolUrl)

        then:
        session.isOpen()

        cleanup:
        session.close()
    }

    @Override
    String getImageName() {
        MySQLTestResourceProvider.MYSQL_OFFICIAL_IMAGE
    }
}
