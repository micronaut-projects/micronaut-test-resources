package io.micronaut.testresources.jdbc.h2

import groovy.transform.ToString
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty

@MappedEntity
@ToString
@Introspected
class H2Book {
    @GeneratedValue(GeneratedValue.Type.IDENTITY)
    @MappedProperty(definition = "BIGINT GENERATED ALWAYS AS IDENTITY")
    @Id
    Long id

    String title
}
