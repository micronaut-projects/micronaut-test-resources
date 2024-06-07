package io.micronaut.testresources.localstack.sns;

import io.micronaut.testresources.localstack.LocalStackService;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adds support for Localstack SNS.
 */
public class LocalStackSNSService implements LocalStackService {

    private static final String AWS_SNS_ENDPOINT_OVERRIDE = "aws.services.sns.endpoint-override";

    @Override
    public LocalStackContainer.Service getServiceKind() {
        return LocalStackContainer.Service.SNS;
    }

    @Override
    public List<String> getResolvableProperties() {
        return Collections.singletonList(AWS_SNS_ENDPOINT_OVERRIDE);
    }

    @Override
    public Optional<String> resolveProperty(String propertyName, LocalStackContainer container) {
        if (AWS_SNS_ENDPOINT_OVERRIDE.equals(propertyName)) {
            return Optional.of(container.getEndpointOverride(LocalStackContainer.Service.SNS).toString());
        }
        return Optional.empty();
    }
}
