package io.micronaut.testresources.floci

import io.floci.testcontainers.FlociContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class FlociTestResourceProviderTest extends Specification {

    def "only supported service modules are enabled"() {
        given:
        def container = new FlociContainer(DockerImageName.parse("floci/floci:1.5.17"))

        when:
        FlociTestResourceProvider.configureServices(container, ["s3", "sqs"] as Set)

        then:
        container.s3Config.enabled
        container.sqsConfig.enabled

        and:
        !container.dynamoDbConfig.enabled
        !container.snsConfig.enabled

        and:
        !container.lambdaConfig.enabled
        !container.ecsConfig.enabled
        !container.rdsConfig.enabled
        !container.eksConfig.enabled
        !container.elastiCacheConfig.enabled
        !container.ec2Config.enabled
    }
}
