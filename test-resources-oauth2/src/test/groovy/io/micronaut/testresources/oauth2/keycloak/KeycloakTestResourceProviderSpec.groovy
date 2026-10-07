package io.micronaut.testresources.oauth2.keycloak

import spock.lang.Specification

class KeycloakTestResourceProviderSpec extends Specification {

    def "does not advertise an explicit JWKS URL alongside the OIDC issuer"() {
        when:
        def properties = new KeycloakTestResourceProvider().getResolvableProperties([:], [:])

        then:
        properties.contains(KeycloakTestResourceProvider.ISSUER)
        !properties.contains(KeycloakTestResourceProvider.JWKS_URL)
    }
}
