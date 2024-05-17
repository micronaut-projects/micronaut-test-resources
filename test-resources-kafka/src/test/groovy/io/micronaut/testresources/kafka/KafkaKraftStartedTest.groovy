package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest(environments = "kraft")
class KafkaKraftStartedTest extends AbstractKafkaSpec {

    @Inject
    ApplicationContext applicationContext

    @Override
    String getImageName() {
        'confluent-local'
    }

    def "starts Kafka using a kraft mode and confluent-local image"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        listContainers().size() == 1
        result.block() == "oh yeah!"
        with(TestContainers.listByScope("kafka").get(Scope.of("kafka"))) {
            size() == 1
            get(0).dockerImageName == KafkaTestResourceProvider.DEFAULT_KRAFT_IMAGE
        }
    }
}
