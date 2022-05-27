package io.micronaut.testresources.jdbc

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.POSTGRES)
interface PostgreSQLBookRepository extends CrudRepository<Book, Long> {
}
