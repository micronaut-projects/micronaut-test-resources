package io.micronaut.testresources.azure

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobServiceClientBuilder
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

import java.nio.charset.StandardCharsets

@MicronautTest
class AzuriteStartedTest extends AbstractAzuriteSpec {

    @Inject
    SharedKeyConfiguration sharedKeyConfiguration

    def "automatically starts an Azurite container and resolves shared-key properties"() {
        given:
        def serviceClient = new BlobServiceClientBuilder()
                .connectionString(sharedKeyConfiguration.connectionString)
                .buildClient()
        def containerClient = serviceClient.createBlobContainer('test-container')
        def blobClient = containerClient.getBlobClient('test-blob')
        blobClient.upload(BinaryData.fromString('test data'), true)
        def mappedBlobPort = listContainers()
                .first()
                .ports
                .find { it.privatePort == AzuriteTestResourceProvider.BLOB_PORT }
                ?.publicPort

        expect:
        sharedKeyConfiguration.accountName == AzuriteTestResourceProvider.DEFAULT_ACCOUNT_NAME
        sharedKeyConfiguration.accountKey == AzuriteTestResourceProvider.DEFAULT_ACCOUNT_KEY
        sharedKeyConfiguration.connectionString.contains('BlobEndpoint=http://')
        sharedKeyConfiguration.connectionString.contains('QueueEndpoint=http://')
        sharedKeyConfiguration.connectionString.contains('TableEndpoint=http://')
        sharedKeyConfiguration.connectionString.contains(":${mappedBlobPort}/${AzuriteTestResourceProvider.DEFAULT_ACCOUNT_NAME}")
        new String(blobClient.downloadContent().toBytes(), StandardCharsets.UTF_8) == 'test data'
        listContainers().size() == 1
    }

    @ConfigurationProperties('azure.credential.storage-shared-key')
    static class SharedKeyConfiguration {
        String accountName
        String accountKey
        String connectionString
    }
}
