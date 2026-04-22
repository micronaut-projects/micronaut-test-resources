package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.TopicDescription
import spock.lang.Specification

import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractKafkaTopicProvisioningSpec extends AbstractKafkaSpec {
    private static final long ADMIN_TIMEOUT_SECONDS = 30

    @Inject
    ApplicationContext applicationContext

    protected String resolveBootstrapServers() {
        applicationContext.environment
            .getProperty(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, String)
            .orElseThrow()
    }

    protected static Map<String, TopicDescription> describeTopics(String bootstrapServers, String... topicNames) {
        Properties properties = new Properties()
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        try (AdminClient adminClient = AdminClient.create(properties)) {
            return adminClient.describeTopics(topicNames.toList()).allTopicNames().get(ADMIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (TimeoutException e) {
            throw new AssertionError("Timed out after ${ADMIN_TIMEOUT_SECONDS}s describing Kafka topics ${topicNames.toList()} for ${bootstrapServers}", e)
        }
    }
}

@MicronautTest
class KafkaTopicProvisioningTest extends AbstractKafkaTopicProvisioningSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
            "test-resources.containers.kafka.topics"    : "orders",
            "test-resources.containers.kafka.partitions": "3"
        ]
    }

    def "creates configured Kafka topics with configured partitions and repeated resolution stays safe"() {
        when:
        def bootstrapServers = resolveBootstrapServers()
        def repeatedBootstrapServers = resolveBootstrapServers()
        def topics = describeTopics(bootstrapServers, "orders")

        then:
        bootstrapServers == repeatedBootstrapServers
        topics.orders.partitions().size() == 3
        listContainers().size() == 1
    }
}

@MicronautTest
class KafkaDefaultTopicPartitionsTest extends AbstractKafkaTopicProvisioningSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
            "test-resources.containers.kafka.topics": "audit-events"
        ]
    }

    def "defaults Kafka topic partitions to one when not configured"() {
        when:
        def bootstrapServers = resolveBootstrapServers()
        def topics = describeTopics(bootstrapServers, "audit-events")

        then:
        topics["audit-events"].partitions().size() == 1
    }
}

@MicronautTest
class KafkaPartitionsWithoutTopicsTest extends AbstractKafkaTopicProvisioningSpec {

    @Override
    Map<String, String> getProperties() {
        super.properties + [
            "test-resources.containers.kafka.partitions": "0"
        ]
    }

    def "ignores Kafka topic partitions when no topics are configured"() {
        when:
        def bootstrapServers = resolveBootstrapServers()

        then:
        bootstrapServers
        listContainers().size() == 1
    }
}

class KafkaInvalidTopicProvisioningConfigTest extends Specification {

    def "rejects blank Kafka topic names"() {
        when:
        KafkaTestResourceProvider.configuredTopics([(KafkaTestResourceProvider.KAFKA_TOPICS): ["valid-topic", "   "]])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("must not be blank")
    }

    def "rejects Kafka topic partitions lower than one"() {
        when:
        KafkaTestResourceProvider.configuredPartitions([(KafkaTestResourceProvider.KAFKA_PARTITIONS): 0])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("greater than or equal to 1")
    }

    def "rejects Kafka topic partitions with a fractional numeric value"() {
        when:
        KafkaTestResourceProvider.configuredPartitions([(KafkaTestResourceProvider.KAFKA_PARTITIONS): 1.5])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("must be an integer")
    }
}
