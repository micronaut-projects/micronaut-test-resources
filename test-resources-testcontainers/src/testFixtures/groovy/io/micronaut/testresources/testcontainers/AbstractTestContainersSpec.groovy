package io.micronaut.testresources.testcontainers

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Container
import io.micronaut.test.support.TestPropertyProvider
import io.micronaut.testresources.core.Scope
import org.testcontainers.DockerClientFactory
import spock.lang.Specification

abstract class AbstractTestContainersSpec extends Specification implements TestPropertyProvider {

    String getScopeName() {
        this.class.simpleName
    }

    Map<String, String> getProperties() {
        [(Scope.PROPERTY_KEY): scopeName]
    }

    void cleanupSpec() {
        TestContainers.closeScope(scopeName)
    }


    protected DockerClient dockerClient() {
        DockerClientFactory.instance().client()
    }

    protected List<Container> runningTestContainers() {
        dockerClient().listContainersCmd()
                .exec()
                .findAll {
                    it.labels['org.testcontainers'] == 'true'
                }
                .findAll {
                    println it
                    it.state == 'running'
                }
    }

    String getImageName() {
        scopeName
    }

    protected List<Container> listContainers() {
        runningTestContainers()
                .findAll { it.image.contains(imageName) }
    }

    static void setDefaultEndpointVerificationAlgorithmToNone() {
        // In netty 4.2.2 SslContextBuilder defaults endpointIdentificationAlgorithm to "HTTPS" if system property
        // "io.netty.handler.ssl.defaultEndpointVerificationAlgorithm" is not set. This makes sun.security.util.HostnameChecker.match(String expectedName, X509Certificate cert, boolean chainsToPublicCA)
        // method to throw error "javax.net.ssl.SSLHandshakeException: No name matching localhost found" and this is workaround.
        // Which could be documented or suggested if users experience this issue
        System.setProperty("io.netty.handler.ssl.defaultEndpointVerificationAlgorithm", "NONE");
    }
}
