package io.micronaut.testresources.oauth2.keycloak

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject

@MicronautTest
class KeycloakTest extends AbstractKeycloakSpec {

    @Inject
    MyClient client

    def "starts a keycloak server"() {
        when:
        String message = client.index()

        then:
        message == "Hello, Keycloak!"

        listContainers().size() == 1
    }

    static interface MyApi {
        @Get("/")
        String index()
    }

    @Controller("/")
    static class MyController implements MyApi {
        @Secured(SecurityRule.IS_ANONYMOUS)
        @Get("/")
        String index() {
            "Hello, Keycloak!"
        }
    }

    @Client("/")
    static interface MyClient extends MyApi {
    }
}
