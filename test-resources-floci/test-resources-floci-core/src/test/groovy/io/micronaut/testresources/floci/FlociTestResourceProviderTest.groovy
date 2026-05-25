package io.micronaut.testresources.floci

import io.floci.testcontainers.FlociContainer
import io.micronaut.testresources.core.DefaultTestResourceImages
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class FlociTestResourceProviderTest extends Specification {

    def "exposes Micronaut AWS service endpoint override properties backed by Floci"() {
        expect:
        new FlociTestResourceProvider().getResolvableProperties([:], [:]).containsAll([
                "aws.services.execute-api.endpoint-override",
                "aws.services.logs.endpoint-override",
                "aws.services.dynamodb.endpoint-override",
                "aws.services.lambda.endpoint-override",
                "aws.services.s3.endpoint-override",
                "aws.services.secretsmanager.endpoint-override",
                "aws.services.ses.endpoint-override",
                "aws.services.sns.endpoint-override",
                "aws.services.sqs.endpoint-override",
                "aws.services.ssm.endpoint-override"
        ])
    }

    def "only supported service modules are enabled"() {
        given:
        def container = new FlociContainer(DockerImageName.parse(DefaultTestResourceImages.DEFAULT_FLOCI_IMAGE))

        when:
        FlociTestResourceProvider.configureServices(container, ["execute-api", "logs", "lambda", "s3", "secretsmanager", "ses", "sqs", "ssm"] as Set)

        then:
        container.apiGatewayConfig.enabled
        container.apiGatewayV2Config.enabled
        container.cloudWatchLogsConfig.enabled
        container.lambdaConfig.enabled
        container.s3Config.enabled
        container.secretsManagerConfig.enabled
        container.sesConfig.enabled
        container.sqsConfig.enabled
        container.ssmConfig.enabled

        and:
        !container.dynamoDbConfig.enabled
        !container.snsConfig.enabled

        and:
        !container.configServiceConfig.enabled
        !container.ecsConfig.enabled
        !container.rdsConfig.enabled
        !container.eksConfig.enabled
        !container.elastiCacheConfig.enabled
        !container.ec2Config.enabled
        !container.bcmDataExportsConfig.enabled
        !container.neptuneConfig.enabled
    }
}
