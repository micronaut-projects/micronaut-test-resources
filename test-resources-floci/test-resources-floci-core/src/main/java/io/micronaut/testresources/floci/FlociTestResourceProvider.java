/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.floci;

import io.floci.testcontainers.FlociContainer;
import io.micronaut.testresources.aws.AbstractAwsTestResourceProvider;
import io.micronaut.testresources.core.DefaultTestResourceImages;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A test resource provider which will spawn Floci test containers.
 */
public class FlociTestResourceProvider extends AbstractAwsTestResourceProvider<FlociContainer, FlociService> {

    public static final String DISPLAY_NAME = "Floci";

    private static final String NAME = "floci";

    private static final String SERVICE_APIGATEWAY_MANAGEMENT = "execute-api";
    private static final String SERVICE_CLOUDWATCH_LOGS = "logs";
    private static final String SERVICE_S3 = "s3";
    private static final String SERVICE_DYNAMODB = "dynamodb";
    private static final String SERVICE_LAMBDA = "lambda";
    private static final String SERVICE_SECRETS_MANAGER = "secretsmanager";
    private static final String SERVICE_SES = "ses";
    private static final String SERVICE_SNS = "sns";
    private static final String SERVICE_SQS = "sqs";
    private static final String SERVICE_SSM = "ssm";

    private static final List<FlociService> DEFAULT_SERVICES = Arrays.asList(
        new FlociEndpointService(SERVICE_APIGATEWAY_MANAGEMENT),
        new FlociEndpointService(SERVICE_CLOUDWATCH_LOGS),
        new FlociEndpointService(SERVICE_DYNAMODB),
        new FlociEndpointService(SERVICE_LAMBDA),
        new FlociEndpointService(SERVICE_S3),
        new FlociEndpointService(SERVICE_SECRETS_MANAGER),
        new FlociEndpointService(SERVICE_SES),
        new FlociEndpointService(SERVICE_SNS),
        new FlociEndpointService(SERVICE_SQS),
        new FlociEndpointService(SERVICE_SSM)
    );

    public FlociTestResourceProvider() {
        super(FlociService.class, DEFAULT_SERVICES);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DefaultTestResourceImages.DEFAULT_FLOCI_IMAGE;
    }

    @Override
    protected FlociContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        FlociContainer flociContainer = new FlociContainer(imageName);
        configureServices(flociContainer, getServiceKinds());
        return flociContainer;
    }

    static void configureServices(FlociContainer flociContainer, Set<String> serviceKinds) {
        flociContainer.withAcmConfig(config -> config.enabled(false));
        flociContainer.withApiGatewayConfig(config -> config.enabled(serviceKinds.contains(SERVICE_APIGATEWAY_MANAGEMENT)));
        flociContainer.withApiGatewayV2Config(config -> config.enabled(serviceKinds.contains(SERVICE_APIGATEWAY_MANAGEMENT)));
        flociContainer.withAppConfigConfig(config -> config.enabled(false));
        flociContainer.withAppConfigDataConfig(config -> config.enabled(false));
        flociContainer.withAthenaConfig(config -> config.enabled(false));
        flociContainer.withBcmDataExportsConfig(config -> config.enabled(false));
        flociContainer.withBackupConfig(config -> config.enabled(false));
        flociContainer.withBedrockRuntimeConfig(config -> config.enabled(false));
        flociContainer.withCloudFormationConfig(config -> config.enabled(false));
        flociContainer.withCloudFrontConfig(config -> config.enabled(false));
        flociContainer.withCloudWatchLogsConfig(config -> config.enabled(serviceKinds.contains(SERVICE_CLOUDWATCH_LOGS)));
        flociContainer.withCloudWatchMetricsConfig(config -> config.enabled(false));
        flociContainer.withCodeBuildConfig(config -> config.enabled(false));
        flociContainer.withCodeDeployConfig(config -> config.enabled(false));
        flociContainer.withCognitoConfig(config -> config.enabled(false));
        flociContainer.withConfigServiceConfig(config -> config.enabled(false));
        flociContainer.withCostExplorerConfig(config -> config.enabled(false));
        flociContainer.withCurConfig(config -> config.enabled(false));
        flociContainer.withDynamoDbConfig(config -> config.enabled(serviceKinds.contains(SERVICE_DYNAMODB)));
        flociContainer.withEc2Config(config -> config.enabled(false));
        flociContainer.withEcrConfig(config -> config.enabled(false));
        flociContainer.withEcsConfig(config -> config.enabled(false));
        flociContainer.withEksConfig(config -> config.enabled(false));
        flociContainer.withElastiCacheConfig(config -> config.enabled(false));
        flociContainer.withElbV2Config(config -> config.enabled(false));
        flociContainer.withEventBridgeConfig(config -> config.enabled(false));
        flociContainer.withFirehoseConfig(config -> config.enabled(false));
        flociContainer.withGlueConfig(config -> config.enabled(false));
        flociContainer.withIamConfig(config -> config.enabled(false));
        flociContainer.withKinesisConfig(config -> config.enabled(false));
        flociContainer.withKmsConfig(config -> config.enabled(false));
        flociContainer.withLambdaConfig(config -> config.enabled(serviceKinds.contains(SERVICE_LAMBDA)));
        flociContainer.withMskConfig(config -> config.enabled(false));
        flociContainer.withNeptuneConfig(config -> config.enabled(false));
        flociContainer.withOpenSearchConfig(config -> config.enabled(false));
        flociContainer.withPipesConfig(config -> config.enabled(false));
        flociContainer.withPricingConfig(config -> config.enabled(false));
        flociContainer.withRdsConfig(config -> config.enabled(false));
        flociContainer.withResourceGroupsTaggingConfig(config -> config.enabled(false));
        flociContainer.withRoute53Config(config -> config.enabled(false));
        flociContainer.withS3Config(config -> config.enabled(serviceKinds.contains(SERVICE_S3)));
        flociContainer.withSchedulerConfig(config -> config.enabled(false));
        flociContainer.withSecretsManagerConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SECRETS_MANAGER)));
        flociContainer.withSesConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SES)));
        flociContainer.withSnsConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SNS)));
        flociContainer.withSqsConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SQS)));
        flociContainer.withSsmConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SSM)));
        flociContainer.withStepFunctionsConfig(config -> config.enabled(false));
        flociContainer.withTextractConfig(config -> config.enabled(false));
        flociContainer.withTransferFamilyConfig(config -> config.enabled(false));
    }

    @Override
    protected String resolveAccessKey(FlociContainer container) {
        return container.getAccessKey();
    }

    @Override
    protected String resolveSecretKey(FlociContainer container) {
        return container.getSecretKey();
    }

    @Override
    protected String resolveRegion(FlociContainer container) {
        return container.getRegion();
    }
}
