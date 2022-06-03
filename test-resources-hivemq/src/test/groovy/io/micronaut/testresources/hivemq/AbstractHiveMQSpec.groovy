package io.micronaut.testresources.hivemq

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.micronaut.context.annotation.Prototype
import io.micronaut.mqtt.annotation.MqttSubscriber
import io.micronaut.mqtt.annotation.Topic
import io.micronaut.mqtt.v5.annotation.MqttPublisher
import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.DockerClientFactory
import spock.lang.Specification

import java.util.concurrent.LinkedBlockingDeque

abstract class AbstractHiveMQSpec extends Specification implements TestPropertyProvider {

    Map<String, String> getProperties() {
        [(Scope.PROPERTY_KEY): 'hivemq']
    }

    void cleanupSpec() {
        TestContainers.closeScope("hivemq")
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

    protected List<Container> hivemqContainers() {
        runningTestContainers()
                .findAll { it.image.contains('hivemq/hivemq') }
    }

    @Prototype
    static class SomeBean {
        String message = 'hello'
    }

    @MqttPublisher
    static interface Publisher {
        @Topic("/test/topic")
        void emit(byte[] message)
    }

    @MqttSubscriber
    static class Client {
        private final LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>()

        @Topic("/test/topic")
        void receive(byte[] message) {
            messages.add(new String(message))
        }

        String getMessage() {
            messages.take()
        }
    }
}
