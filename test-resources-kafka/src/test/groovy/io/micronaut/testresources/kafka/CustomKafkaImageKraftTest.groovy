package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.TestContainers
import jakarta.inject.Inject

@MicronautTest(environments = "kraft")
class CustomKafkaImageKraftTest extends AbstractKafkaSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
                "test-resources.containers.kafka.image-name": "confluentinc/confluent-local:7.7.1"
        ]
    }

    @Override
    String getImageName() {
        'confluent-local'
    }

    @Inject
    ApplicationContext applicationContext

    /**
     * Note this test will fail if not launched in kraft mode (i.e. environments = kraft) as no zookeeper was configured.
     * So there are no assertions in the test to check if kraft mode was enabled.
     *
     * Example Error:
     *
     * 10:22:21.672 [main] ERROR t.confluentinc/confluent-local:7.7.1 - Could not start container java.lang.IllegalStateException: Wait strategy failed. Container exited with code 1
     *
     * Docker Logs:
     *  2024-10-11 10:21:22 [2024-10-11 14:21:22,411] ERROR Exiting Kafka due to fatal exception (kafka.Kafka$)
     *  2024-10-11 10:21:22 java.lang.IllegalArgumentException: requirement failed: controller.listener.names must contain at least one value appearing in the 'listeners' configuration when running the KRaft controller role
     */
    def "starts Kafka using a custom image with Kraft"() {
        when:
        def client = applicationContext.getBean(AnalyticsClient)
        def result = client.updateAnalytics("oh yeah!")

        then:
        listContainers().size() == 1
        result.block() == "oh yeah!"
        with(TestContainers.listByScope("kafka").get(Scope.of("kafka"))) {
            size() == 1
            get(0).dockerImageName == "confluentinc/confluent-local:7.7.1"
        }
    }

}
