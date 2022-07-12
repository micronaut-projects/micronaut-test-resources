package io.micronaut.testresources.hibernate.reactive.core

import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.reactive.ReactorCrudRepository

@Repository
interface BookRepository extends ReactorCrudRepository<Book, Long> {
}
