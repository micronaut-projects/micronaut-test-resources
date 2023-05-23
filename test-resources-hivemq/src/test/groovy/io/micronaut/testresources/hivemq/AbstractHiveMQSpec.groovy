package io.micronaut.testresources.hivemq


import io.micronaut.context.annotation.Prototype
import io.micronaut.mqtt.annotation.MqttSubscriber
import io.micronaut.mqtt.annotation.Topic
import io.micronaut.mqtt.annotation.v5.MqttPublisher
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

import java.util.concurrent.LinkedBlockingDeque

abstract class AbstractHiveMQSpec extends AbstractTestContainersSpec {
    @Override
    String getScopeName() {
        'hivemq'
    }

    @Override
    String getImageName() {
        'hivemq/hivemq'
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
