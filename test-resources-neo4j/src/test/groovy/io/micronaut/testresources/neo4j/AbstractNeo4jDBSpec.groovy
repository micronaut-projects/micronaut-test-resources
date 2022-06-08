package io.micronaut.testresources.neo4j

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import org.testcontainers.DockerClientFactory
import spock.lang.Specification

abstract class AbstractNeo4jDBSpec extends Specification implements TestPropertyProvider {

    @Inject
    protected BookRepository bookRepository;

    Map<String, String> getProperties() {
        [(Scope.PROPERTY_KEY): 'neo4j']
    }

    void cleanupSpec() {
        TestContainers.closeScope("neo4j")
    }

    protected DockerClient dockerClient() {
        DockerClientFactory.instance().client()
    }

    protected List<Container> runningTestContainers() {
        dockerClient().listContainersCmd()
                .exec()
                .findAll {
                    it.labels['org.testcontainers'] == 'true'
                }
                .findAll {
                    println it
                    it.state == 'running'
                }
    }

    protected List<Container> neo4jContainers() {
        runningTestContainers()
                .findAll { it.image.contains('neo4j') }
    }
}
