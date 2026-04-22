package io.micronaut.testresources.pulsar

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionInitialPosition

import java.util.UUID
import java.util.concurrent.TimeUnit

@MicronautTest
class PulsarStartedTest extends AbstractPulsarSpec {

    @Inject
    ApplicationContext applicationContext

    def "automatically starts a Pulsar container"() {
        given:
        def client = applicationContext.getBean(PulsarClient)
        def topicName = "persistent://public/default/test-${UUID.randomUUID()}"
        def subscriptionName = "subscription-${UUID.randomUUID()}"
        def consumer = client.newConsumer(Schema.STRING)
            .topic(topicName)
            .subscriptionName(subscriptionName)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe()
        def producer = client.newProducer(Schema.STRING)
            .topic(topicName)
            .create()

        when:
        producer.send("hello pulsar")
        def message = consumer.receive(30, TimeUnit.SECONDS)

        then:
        listContainers().size() == 1
        message != null
        message.value == "hello pulsar"
        with(TestContainers.listByScope(PulsarTestResourceProvider.SIMPLE_NAME).get(Scope.of(PulsarTestResourceProvider.SIMPLE_NAME))) {
            size() == 1
            get(0).dockerImageName == PulsarTestResourceProvider.DEFAULT_IMAGE
        }

        cleanup:
        consumer?.close()
        producer?.close()
        client?.close()
    }
}
