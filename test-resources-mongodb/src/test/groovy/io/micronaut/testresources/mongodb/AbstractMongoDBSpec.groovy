package io.micronaut.testresources.mongodb

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.DockerClientFactory
import spock.lang.Specification

abstract class AbstractMongoDBSpec extends Specification implements TestPropertyProvider {

    Map<String, String> getProperties() {
        [(Scope.PROPERTY_KEY): 'mongodb']
    }

    void cleanupSpec() {
        TestContainers.closeScope("mongodb")
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

    protected List<Container> mongodbContainers() {
        runningTestContainers()
                .findAll { it.image.contains('mongodb') }
    }
}
