package io.micronaut.testresources.jdbc.h2

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import io.micronaut.testresources.jdbc.Book

@JdbcRepository(dialect = Dialect.H2)
interface H2BookRepository extends CrudRepository<Book, Long> {
}
