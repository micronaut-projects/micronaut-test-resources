package io.micronaut.testresources.jdbc.free

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import io.micronaut.testresources.jdbc.Book

@JdbcRepository(dialect = Dialect.ORACLE)
interface OracleFreeBookRepository extends CrudRepository<Book, Long> {
}
