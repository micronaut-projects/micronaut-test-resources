package io.micronaut.testresources.mongodb

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
@Introspected
class Book {
    @GeneratedValue
    @Id
    String id

    String title
}
