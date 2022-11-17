package io.micronaut.testresources.jdbc

import groovy.transform.ToString
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
@ToString
@Introspected
class Book {
    @GeneratedValue(GeneratedValue.Type.IDENTITY)
    @Id
    Long id

    String title
}
