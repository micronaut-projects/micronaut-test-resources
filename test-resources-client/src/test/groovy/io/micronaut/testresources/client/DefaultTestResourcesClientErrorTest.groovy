package io.micronaut.testresources.client

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.micronaut.testresources.codec.TestResourcesCodec
import spock.lang.Specification

import java.net.InetSocketAddress

class DefaultTestResourcesClientErrorTest extends Specification {

    private HttpServer server
    private DefaultTestResourcesClient client

    void setup() {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        server.start()
        client = new DefaultTestResourcesClient("http://localhost:${server.address.port}", null, 10)
    }

    void cleanup() {
        server?.stop(0)
    }

    def "falls back to a JSON error body when binary decoding fails"() {
        given:
        endpoint("/resolve", 500, '{"message":"JSON failure"}'.bytes)

        when:
        client.resolve("name", [:], [:])

        then:
        def ex = thrown(TestResourcesException)
        ex.message == "JSON failure"
    }

    def "falls back to plain text error body when binary decoding fails"() {
        given:
        endpoint("/requirements/expr/name", 500, "plain failure".bytes)

        when:
        client.getRequiredProperties("name")

        then:
        def ex = thrown(TestResourcesException)
        ex.message == "plain failure"
    }

    def "reports an empty error response"() {
        given:
        endpoint("/requirements/entries", 500, new byte[0])

        when:
        client.getRequiredPropertyEntries()

        then:
        def ex = thrown(TestResourcesException)
        ex.message == "Server failed without an error body"
    }

    def "reports nested binary error responses"() {
        given:
        endpoint("/close/all", 500, binary([
            message: "outer",
            errors : [
                [message: "nested"],
                "leaf"
            ]
        ]))

        when:
        client.closeAll()

        then:
        def ex = thrown(TestResourcesException)
        ex.message == """Server failed with the following errors:
 - outer
 - nested
 - leaf
"""
    }

    def "returns null for not found responses"() {
        given:
        endpoint("/resolve", 404, new byte[0])

        expect:
        client.resolve("name", [:], [:]).empty
    }

    def "reports unexpected status codes"() {
        given:
        endpoint("/close/null", 409, new byte[0])

        when:
        client.closeScope(null)

        then:
        def ex = thrown(TestResourcesException)
        ex.message == "Unexpected response code: 409"
    }

    private void endpoint(String path, int status, byte[] body) {
        server.createContext(path) { HttpExchange exchange ->
            exchange.responseHeaders.add("Content-Type", "application/vnd.micronaut.test.resources.binary")
            exchange.sendResponseHeaders(status, body.length)
            exchange.responseBody.withCloseable {
                it.write(body)
            }
        }
    }

    private static byte[] binary(Object value) {
        def output = new ByteArrayOutputStream()
        TestResourcesCodec.writeValue(value, output)
        output.toByteArray()
    }
}
