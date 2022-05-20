package io.micronaut.testresources.mysql

import groovy.transform.ToString
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity

@MappedEntity
@ToString
class Book {
    @GeneratedValue(GeneratedValue.Type.AUTO)
    @Id
    Long id

    String title
}
