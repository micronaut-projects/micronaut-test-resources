package io.micronaut.testresources.neo4j


import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject

abstract class AbstractNeo4jDBSpec extends AbstractTestContainersSpec {

    @Inject
    protected BookRepository bookRepository

    @Override
    String getScopeName() {
        'neo4j'
    }

}
