package io.micronaut.testresources.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.DescribeTopicsResult
import org.apache.kafka.clients.admin.ListTopicsResult
import org.apache.kafka.clients.admin.TopicDescription
import org.apache.kafka.common.KafkaFuture
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import spock.lang.Specification

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kafka admin retry specs that need no broker. The aggregate build runs this spec
 * even without -Pkafka, so keep it free of containers.
 */
class KafkaAdminRetryTest extends Specification {

    def "retries Kafka metadata requests once after a timeout"() {
        given:
        def attempts = new AtomicInteger()

        when:
        def result = KafkaTestResourceProvider.retryMetadataRequest({
            if (attempts.incrementAndGet() == 1) {
                throw new TimeoutException("first")
            }
            "metadata"
        } as KafkaTestResourceProvider.AdminMetadataRequest<String>)

        then:
        result == "metadata"
        attempts.get() == 2
    }

    def "rethrows the last timeout when Kafka metadata retries are exhausted"() {
        given:
        def attempts = new AtomicInteger()

        when:
        KafkaTestResourceProvider.retryMetadataRequest({
            throw new TimeoutException("timeout-${attempts.incrementAndGet()}")
        } as KafkaTestResourceProvider.AdminMetadataRequest<String>)

        then:
        def e = thrown(TimeoutException)
        e.message == "timeout-3"
        attempts.get() == 3
    }

    def "retries Kafka metadata requests while the broker does not know the topic yet"() {
        given:
        def attempts = new AtomicInteger()

        when:
        def result = KafkaTestResourceProvider.retryMetadataRequest({
            if (attempts.incrementAndGet() == 1) {
                throw new ExecutionException(new UnknownTopicOrPartitionException("not yet"))
            }
            "metadata"
        } as KafkaTestResourceProvider.AdminMetadataRequest<String>)

        then:
        result == "metadata"
        attempts.get() == 2
    }

    def "rethrows an unknown topic failure when Kafka metadata retries are exhausted"() {
        given:
        def attempts = new AtomicInteger()

        when:
        KafkaTestResourceProvider.retryMetadataRequest({
            attempts.incrementAndGet()
            throw new ExecutionException(new UnknownTopicOrPartitionException("missing"))
        } as KafkaTestResourceProvider.AdminMetadataRequest<String>)

        then:
        def e = thrown(ExecutionException)
        e.cause instanceof UnknownTopicOrPartitionException
        attempts.get() == 3
    }

    def "does not retry other Kafka metadata failures"() {
        given:
        def attempts = new AtomicInteger()

        when:
        KafkaTestResourceProvider.retryMetadataRequest({
            attempts.incrementAndGet()
            throw new ExecutionException(new IllegalStateException("broken"))
        } as KafkaTestResourceProvider.AdminMetadataRequest<String>)

        then:
        def e = thrown(ExecutionException)
        e.cause.message == "broken"
        attempts.get() == 1
    }

    def "retries timed out topic listing before failing"() {
        given:
        def adminClient = Mock(AdminClient)
        def listTopicsResult = Mock(ListTopicsResult)
        listTopicsResult.names() >> new TimeoutOnlyKafkaFuture<Set<String>>()

        when:
        KafkaTestResourceProvider.listTopicNames(adminClient)

        then:
        thrown(TimeoutException)
        3 * adminClient.listTopics() >> listTopicsResult
    }

    def "retries timed out topic listing before returning names"() {
        given:
        def adminClient = Mock(AdminClient)
        def listTopicsResult = Mock(ListTopicsResult)
        def topicNames = ["orders"] as Set
        adminClient.listTopics() >> listTopicsResult
        listTopicsResult.names() >>> [
            new TimeoutOnlyKafkaFuture<Set<String>>(),
            KafkaFuture.completedFuture(topicNames)
        ]

        expect:
        KafkaTestResourceProvider.listTopicNames(adminClient) == topicNames
    }

    def "retries timed out topic description before failing"() {
        given:
        def adminClient = Mock(AdminClient)
        def describeTopicsResult = Mock(DescribeTopicsResult)
        def topicNames = ["orders"]
        describeTopicsResult.allTopicNames() >> new TimeoutOnlyKafkaFuture<Map<String, TopicDescription>>()

        when:
        KafkaTestResourceProvider.describeTopics(adminClient, topicNames)

        then:
        thrown(TimeoutException)
        3 * adminClient.describeTopics(topicNames) >> describeTopicsResult
    }

    def "retries timed out topic description before returning descriptions"() {
        given:
        def adminClient = Mock(AdminClient)
        def describeTopicsResult = Mock(DescribeTopicsResult)
        def topicNames = ["orders"]
        def descriptions = [(topicNames.first()): Mock(TopicDescription)]
        adminClient.describeTopics(topicNames) >> describeTopicsResult
        describeTopicsResult.allTopicNames() >>> [
            new TimeoutOnlyKafkaFuture<Map<String, TopicDescription>>(),
            KafkaFuture.completedFuture(descriptions)
        ]

        expect:
        KafkaTestResourceProvider.describeTopics(adminClient, topicNames) == descriptions
    }
}
