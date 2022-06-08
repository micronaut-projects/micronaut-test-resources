package io.micronaut.testresources.jdbc


import com.github.dockerjava.api.model.Container
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractJDBCSpec extends AbstractTestContainersSpec {

    protected List<Container> databaseContainers() {
        listContainers()
    }
}
