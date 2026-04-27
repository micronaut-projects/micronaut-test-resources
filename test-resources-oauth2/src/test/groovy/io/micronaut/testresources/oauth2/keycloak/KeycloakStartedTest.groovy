package io.micronaut.testresources.oauth2.keycloak

import io.micronaut.context.annotation.Value
import io.micronaut.json.JsonMapper
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec
import io.micronaut.testresources.core.Scope

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@MicronautTest
class KeycloakStartedTest extends AbstractTestContainersSpec {
    private static final JsonMapper JSON_MAPPER = JsonMapper.createDefault()

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
        URI issuerUri = URI.create(issuer)
        List<Integer> mappedPorts = listContainers().collectMany { it.ports as List }
            .findAll { it.publicPort != null }
            .collect { it.publicPort as Integer }

        expect:
        listContainers().size() == 1
        issuerUri.scheme == 'http'
        issuerUri.host
        issuerUri.path == '/realms/micronaut'
        issuerUri.port in mappedPorts
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
        Map<String, Object> tokenJson = JSON_MAPPER.readValue(tokenResponse.body(), Map)
        Map<String, Object> jwksJson = JSON_MAPPER.readValue(jwksResponse.body(), Map)
        then:
        tokenResponse.statusCode() == 200
        tokenJson.access_token
        tokenJson.token_type == 'Bearer'
        jwksResponse.statusCode() == 200
        jwksJson.keys
    }

    private static String formData(Map<String, String> parameters) {
        parameters.collect { key, value ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }.join('&')
    }
}

@MicronautTest
class KeycloakConfiguredValuesTest extends AbstractTestContainersSpec {
    private static final String CUSTOM_REALM = "custom-realm"
    private static final String CUSTOM_CLIENT_ID = "custom-client-id"
    private static final String CUSTOM_CLIENT_SECRET = "custom-secret-value"

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
        'keycloak-custom'
    }

    @Override
    String getImageName() {
        'keycloak'
    }

    @Override
    Map<String, String> getProperties() {
        super.properties + [
            (Scope.PROPERTY_KEY): scopeName,
            'test-resources.containers.keycloak.realm'        : CUSTOM_REALM,
            'test-resources.containers.keycloak.client-id'    : CUSTOM_CLIENT_ID,
            'test-resources.containers.keycloak.client-secret': CUSTOM_CLIENT_SECRET
        ]
    }

    def "respects explicit keycloak configuration overrides"() {
        expect:
        clientId == CUSTOM_CLIENT_ID
        clientSecret == CUSTOM_CLIENT_SECRET
        URI.create(issuer).path == "/realms/${CUSTOM_REALM}"
        jwksUrl == issuer + '/protocol/openid-connect/certs'
        listContainers().size() == 1
    }
}
