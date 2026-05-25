package io.micronaut.testresources.floci.s3

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.floci.AbstractFlociSpec
import jakarta.inject.Inject

@MicronautTest
class FlociS3Test extends AbstractFlociSpec {

    @Inject
    S3Config s3Config

    def "automatically starts an S3 container"() {
        expect:
        s3Config.s3.endpointOverride.startsWith("http://")

        and:
        listContainers().size() == 1
    }

    @ConfigurationProperties("aws")
    static class S3Config {
        String accessKeyId
        String secretKey
        String region

        @ConfigurationBuilder(configurationPrefix = "services.s3")
        final S3 s3 = new S3()

        static class S3 {
            String endpointOverride
        }
    }
}
