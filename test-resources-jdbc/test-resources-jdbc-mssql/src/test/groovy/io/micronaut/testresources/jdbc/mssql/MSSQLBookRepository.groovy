package io.micronaut.testresources.jdbc.mssql

import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import io.micronaut.testresources.jdbc.Book

@JdbcRepository(dialect = Dialect.SQL_SERVER)
interface MSSQLBookRepository extends CrudRepository<Book, Long> {
}
