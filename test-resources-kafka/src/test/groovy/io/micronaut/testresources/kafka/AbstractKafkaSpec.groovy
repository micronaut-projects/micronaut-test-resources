package io.micronaut.testresources.kafka

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.micronaut.configuration.kafka.annotation.KafkaClient
import io.micronaut.configuration.kafka.annotation.Topic
import io.micronaut.context.annotation.Prototype
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.DockerClientFactory
import reactor.core.publisher.Mono
import spock.lang.Specification

abstract class AbstractKafkaSpec extends Specification {

    void cleanupSpec() {
        TestContainers.closeAll()
    }

    @Prototype
    static class SomeBean {
        String message = 'hello'
    }

    @KafkaClient
    static interface AnalyticsClient {
        @Topic("analytics")
        Mono<String> updateAnalytics(String book);
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

    protected List<Container> kafkaContainers() {
        runningTestContainers()
                .findAll { it.image.contains('cp-kafka') }
    }
}
