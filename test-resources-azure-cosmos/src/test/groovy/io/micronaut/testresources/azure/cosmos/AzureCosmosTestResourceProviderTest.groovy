package io.micronaut.testresources.azure.cosmos

import org.testcontainers.containers.CosmosDBEmulatorContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class AzureCosmosTestResourceProviderTest extends Specification {
    private final provider = new TestableAzureCosmosTestResourceProvider()

    def "configures vnext preview image for https and readiness health check"() {
        when:
        CosmosDBEmulatorContainer container = provider.create('mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview')

        then:
        container.envMap.PROTOCOL == 'https'
        !container.envMap.containsKey('ENABLE_EXPLORER')
        container.exposedPorts.containsAll([
                AzureCosmosTestResourceProvider.COSMOS_PORT,
                AzureCosmosTestResourceProvider.VNEXT_HEALTH_PORT
        ])
    }

    def "keeps legacy image overrides on the Testcontainers defaults"() {
        when:
        CosmosDBEmulatorContainer container = provider.create('mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest')

        then:
        container.envMap.isEmpty()
        container.exposedPorts == [AzureCosmosTestResourceProvider.COSMOS_PORT]
    }

    private static final class TestableAzureCosmosTestResourceProvider extends AzureCosmosTestResourceProvider {
        CosmosDBEmulatorContainer create(String imageName) {
            createContainer(DockerImageName.parse(imageName), [:], [:])
        }
    }
}
