package io.micronaut.testresources.client

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.server.exceptions.ExceptionHandler
import io.micronaut.testresources.codec.TestResourcesMediaType
import jakarta.inject.Singleton

/**
 * Sends the exception message in the error body, like the server's
 * {@code TestResourcesExceptionHandler}, instead of relying on the
 * HTTP server's default error response, which does not include
 * exception messages since Micronaut Core 5.2.
 */
@Singleton
@Requires(property = "server", notEquals = "false")
class TestServerExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<?>> {
    @Override
    HttpResponse<?> handle(HttpRequest request, Throwable exception) {
        HttpResponse.serverError([message: exception.message])
            .contentType(TestResourcesMediaType.TEST_RESOURCES_BINARY_MEDIA_TYPE)
    }
}
