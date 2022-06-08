package io.micronaut.testresources.jdbc

import groovy.transform.ToString
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
@ToString
class Book {
    @GeneratedValue(GeneratedValue.Type.IDENTITY)
    @Id
    Long id

    String title
}
