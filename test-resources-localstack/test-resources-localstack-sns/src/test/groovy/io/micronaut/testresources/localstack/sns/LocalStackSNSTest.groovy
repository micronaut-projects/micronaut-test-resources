package io.micronaut.testresources.localstack.sns


import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject
import org.junit.After
import org.junit.Assert
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@MicronautTest
class LocalStackSNSTest extends AbstractLocalStackSpec {

    @Inject
    SnsClient client

    @Inject
    EmbeddedServer embeddedServer

    @Inject
    TestSnsController snsTestController

    @After
    void afterEach() {
        snsTestController.testBody.set(null)
    }

    def "automatically starts a SNS container"() {
        when:
        def topic = client.createTopic {
            it.name("test-topic")
        }

        snsTestController.topicArn.set(topic.topicArn())

        client.subscribe {
            it.topicArn(topic.topicArn())
            it.protocol("http")
            it.endpoint("http://host.docker.internal:${embeddedServer.port}/sns-test")
        }

        then:
        long elapsedMillis
        long startTime = System.currentTimeMillis()
        do {
            // publish won't work until controller confirms subscription
            client.publish {
                it.topicArn(topic.topicArn())
                it.message("test-message-123")
            }

            String notificationBody = snsTestController.testBody.get()
            if (notificationBody != null) {
                Assert.assertTrue(notificationBody.contains("test-message-123"))
                return
            }

            Thread.sleep(100)
            elapsedMillis = System.currentTimeMillis() - startTime
        } while (elapsedMillis < 5000)

        Assert.fail("timed out waiting for endpoint call from SNS")
    }
}
