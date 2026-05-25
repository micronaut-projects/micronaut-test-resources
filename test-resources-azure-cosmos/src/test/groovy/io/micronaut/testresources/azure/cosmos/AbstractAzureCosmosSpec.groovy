package io.micronaut.testresources.azure.cosmos

import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.containers.CosmosDBEmulatorContainer
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path

abstract class AbstractAzureCosmosSpec extends AbstractTestContainersSpec implements TestPropertyProvider {
    private static final AzureCosmosTestResourceProvider PROVIDER = new AzureCosmosTestResourceProvider()

    @Shared
    private Path keyStoreFile

    @Override
    String getScopeName() {
        'azure-cosmos'
    }

    @Override
    String getImageName() {
        'azure-cosmos-emulator'
    }

    @Override
    Map<String, String> getProperties() {
        Map<String, Object> request = [(Scope.PROPERTY_KEY): scopeName]
        PROVIDER.resolve(AzureCosmosTestResourceProvider.ENDPOINT, request, [:]).orElseThrow()
        String key = PROVIDER.resolve(AzureCosmosTestResourceProvider.KEY, request, [:]).orElseThrow()
        CosmosDBEmulatorContainer container = (CosmosDBEmulatorContainer) TestContainers
                .findByRequestedProperty(Scope.of(scopeName), AzureCosmosTestResourceProvider.ENDPOINT)
                .first()
        configureSsl(container, key)
        setDefaultEndpointVerificationAlgorithmToNone()
        return [(Scope.PROPERTY_KEY): scopeName]
    }

    @Override
    void cleanupSpec() {
        if (keyStoreFile != null) {
            Files.deleteIfExists(keyStoreFile)
        }
    }

    private void configureSsl(CosmosDBEmulatorContainer container, String key) {
        keyStoreFile = Files.createTempFile('azure-cosmos-emulator', '.keystore')
        Files.newOutputStream(keyStoreFile).withCloseable { output ->
            container.buildNewKeyStore().store(output, key.toCharArray())
        }
        System.setProperty('javax.net.ssl.trustStore', keyStoreFile.toString())
        System.setProperty('javax.net.ssl.trustStorePassword', key)
        System.setProperty('javax.net.ssl.trustStoreType', 'PKCS12')
    }
}
