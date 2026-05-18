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
import io.micronaut.testresources.core.DefaultTestResourceImages;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A test resource provider which will spawn Floci test containers.
 */
public class FlociTestResourceProvider extends AbstractTestContainersProvider<FlociContainer> {

    public static final String DISPLAY_NAME = "Floci";

    private static final String NAME = "floci";

    private static final String SERVICE_DYNAMODB = "dynamodb";
    private static final String SERVICE_S3 = "s3";
    private static final String SERVICE_SNS = "sns";
    private static final String SERVICE_SQS = "sqs";

    private static final String AWS_ACCESS_KEY_ID = "aws.access-key-id";
    private static final String AWS_SECRET_KEY = "aws.secret-key";
    private static final String AWS_REGION = "aws.region";

    private static final List<String> COMMON_PROPERTIES;

    private static final Map<String, List<String>> RESOLVABLE_PROPERTIES;
    private static final Set<String> ALL_SUPPORTED_KEYS;
    private static final List<FlociService> SERVICES;
    private static final Map<String, FlociService> PROPERTY_TO_SERVICE;

    static {
        SERVICES = StreamSupport.stream(ServiceLoader.load(FlociService.class).spliterator(), false)
            .toList();
        Map<String, List<String>> resolvableProperties = new HashMap<>();
        Map<String, FlociService> propertyToService = new HashMap<>();
        COMMON_PROPERTIES = Collections.unmodifiableList(Arrays.asList(
            AWS_ACCESS_KEY_ID,
            AWS_SECRET_KEY,
            AWS_REGION
        ));
        for (FlociService flociService : SERVICES) {
            List<String> supportedProperties = flociService.getResolvableProperties();
            resolvableProperties.put(flociService.getServiceKind(), supportedProperties);
            for (String supportedProperty : supportedProperties) {
                propertyToService.put(supportedProperty, flociService);
            }
        }
        RESOLVABLE_PROPERTIES = Collections.unmodifiableMap(resolvableProperties);
        ALL_SUPPORTED_KEYS = Stream.concat(
            COMMON_PROPERTIES.stream(),
            RESOLVABLE_PROPERTIES.values().stream().flatMap(Collection::stream)
        ).collect(Collectors.toSet());
        PROPERTY_TO_SERVICE = Collections.unmodifiableMap(propertyToService);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Stream.concat(
                SERVICES.stream().flatMap(service -> service.getResolvableProperties().stream()),
                COMMON_PROPERTIES.stream()
            ).distinct()
            .toList();
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
        configureServices(flociContainer, SERVICES.stream()
            .map(FlociService::getServiceKind)
            .collect(Collectors.toSet()));
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
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return ALL_SUPPORTED_KEYS.contains(propertyName);
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, FlociContainer container) {
        switch (propertyName) {
            case AWS_ACCESS_KEY_ID:
                return Optional.of(container.getAccessKey());
            case AWS_SECRET_KEY:
                return Optional.of(container.getSecretKey());
            case AWS_REGION:
                return Optional.of(container.getRegion());
            default:
                FlociService service = PROPERTY_TO_SERVICE.get(propertyName);
                if (service != null) {
                    return service.resolveProperty(propertyName, container);
                }
        }
        return Optional.empty();
    }
}
