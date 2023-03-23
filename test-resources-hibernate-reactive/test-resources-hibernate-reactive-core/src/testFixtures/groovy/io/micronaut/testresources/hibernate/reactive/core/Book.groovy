package io.micronaut.testresources.hibernate.reactive.core

import io.micronaut.core.annotation.Introspected

import jakarta.persistence.Id
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType

@Entity
@Introspected
class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    String title
}
