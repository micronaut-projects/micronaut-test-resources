package io.micronaut.testresources.kafka

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.core.TestResourcesResolutionException
import jakarta.inject.Inject
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.admin.TopicDescription

import java.util.Properties
import java.util.concurrent.ExecutionException
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

class KafkaReusedContainerTopicProvisioningTest extends AbstractKafkaTopicProvisioningSpec {

    def "provisions configured Kafka topics when reusing a cached broker in the same scope"() {
        given:
        def provider = new KafkaTestResourceProvider()
        def requestedProperties = properties

        when:
        def bootstrapServers = provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, requestedProperties, [:]).orElseThrow()
        def reusedBootstrapServers = provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, requestedProperties, [
            (KafkaTestResourceProvider.KAFKA_TOPICS)    : "orders",
            (KafkaTestResourceProvider.KAFKA_PARTITIONS): "2"
        ]).orElseThrow()
        def topics = describeTopics(reusedBootstrapServers, "orders")

        then:
        bootstrapServers == reusedBootstrapServers
        topics.orders.partitions().size() == 2
        listContainers().size() == 1
    }

    def "rejects conflicting Kafka partition requests when reusing a cached broker in the same scope"() {
        given:
        def provider = new KafkaTestResourceProvider()
        def requestedProperties = properties

        when:
        def bootstrapServers = provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, requestedProperties, [
            (KafkaTestResourceProvider.KAFKA_TOPICS)    : "payments",
            (KafkaTestResourceProvider.KAFKA_PARTITIONS): "1"
        ]).orElseThrow()
        provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, requestedProperties, [
            (KafkaTestResourceProvider.KAFKA_TOPICS)    : "payments",
            (KafkaTestResourceProvider.KAFKA_PARTITIONS): "3"
        ]).orElseThrow()

        then:
        def e = thrown(TestResourcesResolutionException)
        e.message.contains("already exists with 1 partitions")
        e.message.contains("requested 3")
        describeTopics(bootstrapServers, "payments").payments.partitions().size() == 1
        listContainers().size() == 1
    }

    def "rejects partition mismatch when the topic appears between listing and create"() {
        given:
        def provider = new RacingKafkaTestResourceProvider()

        when:
        provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, properties, [
            (KafkaTestResourceProvider.KAFKA_TOPICS)    : "race-topic",
            (KafkaTestResourceProvider.KAFKA_PARTITIONS): "3"
        ]).orElseThrow()

        then:
        def e = thrown(TestResourcesResolutionException)
        e.message.contains("already exists with 1 partitions")
        e.message.contains("requested 3")
    }
}

class RacingKafkaTestResourceProvider extends KafkaTestResourceProvider {
    @Override
    protected void beforeCreateTopics(org.testcontainers.kafka.KafkaContainer container,
                                      TopicProvisioningConfiguration configuration,
                                      List<NewTopic> topicsToCreate) {
        Properties properties = new Properties()
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, container.bootstrapServers)
        try (AdminClient adminClient = AdminClient.create(properties)) {
            NewTopic topic = new NewTopic(topicsToCreate.first().name(), 1, (short) 1)
            adminClient.createTopics([topic]).all().get(30, TimeUnit.SECONDS)
        } catch (ExecutionException e) {
            throw new AssertionError("Failed to create the racing Kafka topic", e)
        }
    }
}

class KafkaInvalidTopicProvisioningConfigTest extends AbstractKafkaSpec {

    def "ignores Kafka topic partitions when no topics are configured"() {
        given:
        def provider = new KafkaTestResourceProvider()

        when:
        def bootstrapServers = provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, properties, [
            (KafkaTestResourceProvider.KAFKA_PARTITIONS): "0"
        ]).orElseThrow()

        then:
        noExceptionThrown()
        bootstrapServers
    }

    def "rejects Kafka topic partitions lower than one when topics are configured"() {
        given:
        def provider = new KafkaTestResourceProvider()

        when:
        provider.resolve(KafkaTestResourceProvider.KAFKA_BOOTSTRAP_SERVERS, properties, [
            (KafkaTestResourceProvider.KAFKA_TOPICS)    : "orders",
            (KafkaTestResourceProvider.KAFKA_PARTITIONS): "0"
        ])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("greater than or equal to 1")
    }

    def "rejects blank Kafka topic names"() {
        when:
        KafkaTestResourceProvider.configuredTopics([(KafkaTestResourceProvider.KAFKA_TOPICS): ["valid-topic", "   "]])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("must not be blank")
    }

    def "rejects Kafka topic partitions lower than one when parsed directly"() {
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
