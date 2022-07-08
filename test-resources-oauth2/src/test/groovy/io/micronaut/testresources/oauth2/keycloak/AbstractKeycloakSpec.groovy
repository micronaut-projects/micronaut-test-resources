package io.micronaut.testresources.oauth2.keycloak

import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractKeycloakSpec extends AbstractTestContainersSpec {

    @Override
    String getScopeName() {
        'keycloak'
    }

}
