package io.micronaut.testresources.oauth2.keycloak

import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import org.testcontainers.DockerClientFactory

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@MicronautTest
class KeycloakStartedTest extends AbstractTestContainersSpec {

    @Value('${micronaut.security.oauth2.clients.keycloak.client-id}')
    String clientId

    @Value('${micronaut.security.oauth2.clients.keycloak.client-secret}')
    String clientSecret

    @Value('${micronaut.security.oauth2.clients.keycloak.openid.issuer}')
    String issuer

    @Value('${micronaut.security.token.jwt.signatures.jwks.keycloak.url}')
    String jwksUrl

    @Override
    String getScopeName() {
        'keycloak'
    }

    @Override
    String getImageName() {
        'keycloak'
    }

    def "automatically starts a Keycloak container and resolves OAuth2 properties"() {
        given:
        def host = DockerClientFactory.instance().dockerHostIpAddress()

        expect:
        host in ["localhost", "127.0.0.1"]
        listContainers().size() == 1
        listContainers().collectMany { it.ports as List }.any {
            issuer == "http://$host:$it.publicPort/realms/micronaut"
        }
        jwksUrl == issuer + "/protocol/openid-connect/certs"
        clientId
        clientSecret
    }

    def "generated credentials are usable against Keycloak"() {
        given:
        HttpClient client = HttpClient.newHttpClient()

        when:
        HttpResponse<String> tokenResponse = client.send(
            HttpRequest.newBuilder(URI.create(issuer + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData(
                    grant_type: 'client_credentials',
                    client_id: clientId,
                    client_secret: clientSecret
                )))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        HttpResponse<String> jwksResponse = client.send(
            HttpRequest.newBuilder(URI.create(jwksUrl)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        then:
        tokenResponse.statusCode() == 200
        tokenResponse.body().contains('"access_token"')
        tokenResponse.body().contains('"token_type":"Bearer"')
        jwksResponse.statusCode() == 200
        jwksResponse.body().contains('"keys"')
    }

    private static String formData(Map<String, String> parameters) {
        parameters.collect { key, value ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }.join('&')
    }
}
