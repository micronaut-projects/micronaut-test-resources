package io.micronaut.testresources.mongodb

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
 class Book {
    @GeneratedValue
    @Id
    String id

    String title
}
