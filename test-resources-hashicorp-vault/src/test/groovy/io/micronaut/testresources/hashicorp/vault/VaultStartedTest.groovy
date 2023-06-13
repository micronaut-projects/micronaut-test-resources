package io.micronaut.testresources.hashicorp.vault

import io.micronaut.context.annotation.Value
import io.micronaut.security.oauth2.configuration.OauthClientConfiguration
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import jakarta.inject.Inject
import jakarta.inject.Named

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
        expect:
        listContainers().collectMany { it.ports as List }.any { uri == "http://localhost:$it.publicPort" }
    }

    def "secretsAreUsedForConfiguration"() {
        expect:
        oauthClientConfiguration.clientId == 'hello'
        oauthClientConfiguration.clientSecret == 'world'
    }
}
