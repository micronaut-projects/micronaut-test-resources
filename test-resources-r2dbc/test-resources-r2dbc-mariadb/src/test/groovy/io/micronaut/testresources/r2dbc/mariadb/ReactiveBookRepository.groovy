package io.micronaut.testresources.r2dbc.mariadb

import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorPageableRepository
import io.micronaut.testresources.jdbc.Book

@R2dbcRepository(dialect = Dialect.MYSQL)
interface ReactiveBookRepository extends ReactorPageableRepository<Book, Long> {
}
