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
    private static final String ENDPOINT_VERIFICATION_ALGORITHM_PROPERTY = 'io.netty.handler.ssl.defaultEndpointVerificationAlgorithm'
    private static final AzureCosmosTestResourceProvider PROVIDER = new AzureCosmosTestResourceProvider()

    @Shared
    private Path keyStoreFile

    @Shared
    private Map<String, String> previousSystemProperties = [:]

    @Shared
    private Set<String> absentSystemProperties = [] as Set

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
        setSystemProperty(ENDPOINT_VERIFICATION_ALGORITHM_PROPERTY, 'NONE')
        return [(Scope.PROPERTY_KEY): scopeName]
    }

    @Override
    void cleanupSpec() {
        restoreSystemProperties()
        if (keyStoreFile != null) {
            Files.deleteIfExists(keyStoreFile)
        }
    }

    private void configureSsl(CosmosDBEmulatorContainer container, String key) {
        keyStoreFile = Files.createTempFile('azure-cosmos-emulator', '.keystore')
        Files.newOutputStream(keyStoreFile).withCloseable { output ->
            container.buildNewKeyStore().store(output, key.toCharArray())
        }
        setSystemProperty('javax.net.ssl.trustStore', keyStoreFile.toString())
        setSystemProperty('javax.net.ssl.trustStorePassword', key)
        setSystemProperty('javax.net.ssl.trustStoreType', 'PKCS12')
    }

    private void setSystemProperty(String name, String value) {
        rememberSystemProperty(name)
        System.setProperty(name, value)
    }

    private void rememberSystemProperty(String name) {
        if (previousSystemProperties.containsKey(name) || absentSystemProperties.contains(name)) {
            return
        }
        String previousValue = System.getProperty(name)
        if (previousValue == null) {
            absentSystemProperties.add(name)
        } else {
            previousSystemProperties[name] = previousValue
        }
    }

    private void restoreSystemProperties() {
        absentSystemProperties.each { System.clearProperty(it) }
        previousSystemProperties.each { name, value -> System.setProperty(name, value) }
        absentSystemProperties.clear()
        previousSystemProperties.clear()
    }
}
