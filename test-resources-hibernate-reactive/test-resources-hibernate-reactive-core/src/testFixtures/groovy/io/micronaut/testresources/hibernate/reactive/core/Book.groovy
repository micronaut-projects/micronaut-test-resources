package io.micronaut.testresources.hibernate.reactive.core

import io.micronaut.core.annotation.Introspected

import javax.persistence.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType

@Entity
@Introspected
class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    String title
}
