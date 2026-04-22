package io.micronaut.testresources.pulsar

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Value
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Singleton
import org.apache.pulsar.client.api.PulsarClient

abstract class AbstractPulsarSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'pulsar'
    }

    @Override
    String getImageName() {
        'apachepulsar/pulsar'
    }

    @Prototype
    static class SomeBean {
        String message = 'hello'
    }

    @Factory
    static class PulsarClientFactory {
        @Singleton
        PulsarClient pulsarClient(@Value('${pulsar.service-url}') String serviceUrl) throws Exception {
            PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build()
        }
    }
}
