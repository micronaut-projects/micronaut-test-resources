package io.micronaut.testresources.azure.cosmos

import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.core.Scope
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.testcontainers.TestContainers
import org.testcontainers.containers.CosmosDBEmulatorContainer
import spock.lang.Shared

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate

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
        try {
            restoreSystemProperties()
            if (keyStoreFile != null) {
                Files.deleteIfExists(keyStoreFile)
            }
        } finally {
            TestContainers.closeScope(scopeName)
        }
    }

    private void configureSsl(CosmosDBEmulatorContainer container, String key) {
        keyStoreFile = Files.createTempFile('azure-cosmos-emulator', '.keystore')
        Files.newOutputStream(keyStoreFile).withCloseable { output ->
            KeyStore keyStore = KeyStore.getInstance('PKCS12')
            keyStore.load(null, key.toCharArray())
            keyStore.setCertificateEntry('azure-cosmos-emulator', certificateFrom(container.emulatorEndpoint))
            keyStore.store(output, key.toCharArray())
        }
        setSystemProperty('javax.net.ssl.trustStore', keyStoreFile.toString())
        setSystemProperty('javax.net.ssl.trustStorePassword', key)
        setSystemProperty('javax.net.ssl.trustStoreType', 'PKCS12')
    }

    private static X509Certificate certificateFrom(String endpoint) {
        URI uri = URI.create(endpoint)
        SSLContext sslContext = SSLContext.getInstance('TLS')
        sslContext.init(null, [new X509TrustManager() {
            @Override
            void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            X509Certificate[] getAcceptedIssuers() {
                [] as X509Certificate[]
            }
        }] as TrustManager[], new SecureRandom())
        SSLSocket socket = (SSLSocket) sslContext.socketFactory.createSocket(uri.host, uri.port)
        socket.withCloseable {
            it.startHandshake()
            it.session.peerCertificates[0] as X509Certificate
        }
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
