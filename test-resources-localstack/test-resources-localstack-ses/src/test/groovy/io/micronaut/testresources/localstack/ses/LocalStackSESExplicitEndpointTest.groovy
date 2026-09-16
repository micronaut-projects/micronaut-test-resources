package io.micronaut.testresources.localstack.ses

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.localstack.AbstractLocalStackSpec
import jakarta.inject.Inject

@MicronautTest
class LocalStackSESExplicitEndpointTest extends AbstractLocalStackSpec {

    private static final String EXPLICIT_ENDPOINT = "http://127.0.0.1:4566"

    @Inject
    SesConfig sesConfig

    @Override
    Map<String, String> getProperties() {
        super.getProperties() + ["aws.services.ses.endpoint-override": EXPLICIT_ENDPOINT]
    }

    def "keeps explicit SES endpoint override"() {
        expect:
        sesConfig.ses.endpointOverride == EXPLICIT_ENDPOINT
    }

    @ConfigurationProperties("aws")
    static class SesConfig {
        @ConfigurationBuilder(configurationPrefix = "services.ses")
        final Ses ses = new Ses()

        static class Ses {
            String endpointOverride
        }
    }
}
