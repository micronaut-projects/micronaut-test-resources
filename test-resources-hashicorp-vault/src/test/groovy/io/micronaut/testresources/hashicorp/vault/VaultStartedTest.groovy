package io.micronaut.testresources.hashicorp.vault

import io.micronaut.context.annotation.Value
import io.micronaut.security.oauth2.configuration.OauthClientConfiguration
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject
import jakarta.inject.Named
import org.testcontainers.DockerClientFactory

@MicronautTest
class VaultStartedTest extends AbstractTestContainersSpec {

    @Value('${vault.client.uri}')
    String uri

    @Inject
    @Named("test")
    OauthClientConfiguration oauthClientConfiguration

    @Override
    String getScopeName() {
        'vault'
    }

    def "automatically starts a Vault container"() {
        given:
        // host is different on different platforms (localhost on linux, but 127.0.0.1 on osx)
        def host = DockerClientFactory.instance().dockerHostIpAddress()

        expect:
        host in ["localhost", "127.0.0.1"]
        listContainers().collectMany { it.ports as List }.any { uri == "http://$host:$it.publicPort" }
    }

    def "secretsAreUsedForConfiguration"() {
        expect:
        oauthClientConfiguration.clientId == 'hello'
        oauthClientConfiguration.clientSecret == 'world'
    }
}
