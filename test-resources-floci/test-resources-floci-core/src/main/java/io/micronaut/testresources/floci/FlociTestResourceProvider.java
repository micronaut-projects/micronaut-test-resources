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

import java.util.Map;
import java.util.Set;

/**
 * A test resource provider which will spawn Floci test containers.
 */
public class FlociTestResourceProvider extends AbstractAwsTestResourceProvider<FlociContainer, FlociService> {

    public static final String DISPLAY_NAME = "Floci";

    private static final String NAME = "floci";

    private static final String SERVICE_DYNAMODB = "dynamodb";
    private static final String SERVICE_S3 = "s3";
    private static final String SERVICE_SNS = "sns";
    private static final String SERVICE_SQS = "sqs";

    public FlociTestResourceProvider() {
        super(FlociService.class);
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
        flociContainer.withApiGatewayConfig(config -> config.enabled(false));
        flociContainer.withApiGatewayV2Config(config -> config.enabled(false));
        flociContainer.withAppConfigConfig(config -> config.enabled(false));
        flociContainer.withAppConfigDataConfig(config -> config.enabled(false));
        flociContainer.withAthenaConfig(config -> config.enabled(false));
        flociContainer.withBackupConfig(config -> config.enabled(false));
        flociContainer.withBedrockRuntimeConfig(config -> config.enabled(false));
        flociContainer.withCloudFormationConfig(config -> config.enabled(false));
        flociContainer.withCloudWatchLogsConfig(config -> config.enabled(false));
        flociContainer.withCloudWatchMetricsConfig(config -> config.enabled(false));
        flociContainer.withCodeBuildConfig(config -> config.enabled(false));
        flociContainer.withCodeDeployConfig(config -> config.enabled(false));
        flociContainer.withCognitoConfig(config -> config.enabled(false));
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
        flociContainer.withLambdaConfig(config -> config.enabled(false));
        flociContainer.withMskConfig(config -> config.enabled(false));
        flociContainer.withOpenSearchConfig(config -> config.enabled(false));
        flociContainer.withPipesConfig(config -> config.enabled(false));
        flociContainer.withPricingConfig(config -> config.enabled(false));
        flociContainer.withRdsConfig(config -> config.enabled(false));
        flociContainer.withResourceGroupsTaggingConfig(config -> config.enabled(false));
        flociContainer.withRoute53Config(config -> config.enabled(false));
        flociContainer.withS3Config(config -> config.enabled(serviceKinds.contains(SERVICE_S3)));
        flociContainer.withSchedulerConfig(config -> config.enabled(false));
        flociContainer.withSecretsManagerConfig(config -> config.enabled(false));
        flociContainer.withSesConfig(config -> config.enabled(false));
        flociContainer.withSnsConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SNS)));
        flociContainer.withSqsConfig(config -> config.enabled(serviceKinds.contains(SERVICE_SQS)));
        flociContainer.withSsmConfig(config -> config.enabled(false));
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
