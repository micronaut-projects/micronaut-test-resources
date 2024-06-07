package io.micronaut.testresources.localstack.sns

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sns.SnsClient

import java.util.concurrent.atomic.AtomicReference

@Controller("/sns-test")
class TestSnsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSnsController.class)

    @Inject
    ObjectMapper objectMapper

    @Inject
    SnsClient snsClient

    // for passing information between controller and test case
    AtomicReference testBody = new AtomicReference<String>()
    AtomicReference topicArn = new AtomicReference<String>()

    @Post(consumes = "text/plain")
    def test(@Body String body) {
        LOGGER.info("Got body {}", body)
        Map<String, String> map = objectMapper.readValue(body, new TypeReference<Map<String, String>>() {})
        if (map.get("Type") == "SubscriptionConfirmation") {
            snsClient.confirmSubscription {
                it.topicArn(topicArn.get())
                it.token(map.get("Token"))
            }
            return HttpResponse.ok()
        } else if (map.get("Type") == "Notification") {
            testBody.set(map.get("Message"))
            return HttpResponse.ok()
        } else {
            return HttpResponse.serverError()
        }
    }
}
