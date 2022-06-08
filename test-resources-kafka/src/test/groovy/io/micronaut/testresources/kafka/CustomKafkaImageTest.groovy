package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest
class CustomKafkaImageTest extends AbstractKafkaSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
                "test-resources.containers.kafka.image-name": "confluentinc/cp-kafka:6.2.2",
                "test-resources.containers.kafka.exposed-ports": [["foo": "9092"]]

        ]
    }

    @Inject
    ApplicationContext applicationContext

    def "starts Kafka using a custom image"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        listContainers().size() == 1
        result.block() == "oh yeah!"
        with(TestContainers.listByScope("kafka").get(Scope.of("kafka"))) {
            size() == 1
            get(0).dockerImageName == "confluentinc/cp-kafka:6.2.2"
        }
    }

}
