package io.micronaut.testresources.mongodb

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

@MicronautTest
class VaultStartedTest extends AbstractTestContainersSpec {

    @Value('${vault.client.uri}')
    String uri

    @Override
    String getScopeName() {
        'vault'
    }

    def "automatically starts a Vault container"() {
        expect:
        listContainers().collectMany { it.ports as List }.any { uri == "http://localhost:$it.publicPort" }
    }

}
