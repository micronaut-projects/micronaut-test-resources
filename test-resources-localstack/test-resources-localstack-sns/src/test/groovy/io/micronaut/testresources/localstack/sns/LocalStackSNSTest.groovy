package io.micronaut.testresources.localstack.sns

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject
import org.junit.Assert
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.Topic

@MicronautTest
class LocalStackSNSTest extends AbstractLocalStackSpec {

    @Inject
    SnsClient client

    def "automatically starts a SNS container"() {
        when:
        client.createTopic {
            it.name("test-topic")
        }

        then:
        List<Topic> topics = client.listTopics().topics()
        Assert.assertEquals(1, topics.size())
        Assert.assertTrue(topics[0].topicArn().endsWith("test-topic"))
    }
}
