package io.micronaut.testresources.neo4j

import jakarta.inject.Inject
import org.neo4j.driver.Driver

@jakarta.inject.Singleton
class BookRepository {
    @Inject
    Driver driver

    void save(Book book) {
        driver.session().with {
            it.executeWrite {
                it.run("CREATE (myNode:Book { name: '${book.title}' })").consume()
            }
        }
    }

    List<Book> findAll() {
        return driver.session().with {
            it.executeRead {
                it.run("MATCH (myNode:Book) RETURN myNode")
                    .list {
                        def b = new Book()
                        b.title = it.get("title")
                        b
                    }
            }
        }
    }
}
