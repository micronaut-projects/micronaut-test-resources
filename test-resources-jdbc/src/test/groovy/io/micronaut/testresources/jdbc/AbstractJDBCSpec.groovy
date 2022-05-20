package io.micronaut.testresources.jdbc

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import org.testcontainers.DockerClientFactory
import spock.lang.Specification

abstract class AbstractJDBCSpec extends Specification {

    abstract String getImageName()

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

    protected List<Container> kafkaContainers() {
        runningTestContainers()
                .findAll { it.image.contains(imageName) }
    }
}
