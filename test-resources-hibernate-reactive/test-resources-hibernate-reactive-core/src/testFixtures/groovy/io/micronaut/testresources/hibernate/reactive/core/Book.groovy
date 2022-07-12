package io.micronaut.testresources.hibernate.reactive.core

import javax.persistence.Id
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType

@Entity
class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id

    String title
}
