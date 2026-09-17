package io.micronaut.testresources.client

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.micronaut.testresources.codec.TestResourcesCodec
import spock.lang.Specification

import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class DefaultTestResourcesClientRetryTest extends Specification {
    private HttpServer server

    def cleanup() {
        server?.stop(0)
    }

    def "retries a transient connection failure while resolving a property"() {
        given:
        def attempts = new AtomicInteger()
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        server.createContext("/resolve") { HttpExchange exchange ->
            if (attempts.getAndIncrement() == 0) {
                exchange.close()
                return
            }
            def output = new ByteArrayOutputStream()
            TestResourcesCodec.writeValue('resolved', output)
            def body = output.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.length)
            exchange.responseBody.withCloseable { it.write(body) }
        }
        server.start()
        def client = new DefaultTestResourcesClient("http://localhost:${server.address.port}", null, 10)

        expect:
        client.resolve("name", [:], [:]).get() == "resolved"
        attempts.get() == 2
    }
}
