/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.server;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.testresources.codec.TestResourcesMediaType;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * Encodes transport errors with the binary media type so the client does not
 * need a JSON fallback.
 */
@Singleton
public class TestResourcesExceptionHandler implements ExceptionHandler<Throwable, HttpResponse<?>> {
    @Override
    public HttpResponse<?> handle(HttpRequest request, Throwable exception) {
        return HttpResponse.serverError(Map.of("message", extractMessage(exception)))
            .contentType(TestResourcesMediaType.TEST_RESOURCES_BINARY_MEDIA_TYPE);
    }

    /**
     * Returns the deepest non-blank message in the cause chain, unchanged.
     * Wrappers such as {@code TestResourcesResolutionException(cause)} carry
     * {@code cause.toString()} as their message, so walking to the cause
     * already skips their {@code java.lang.SomeException: } prefix. Messages
     * themselves may contain colons, for example an image name with a tag.
     */
    private static String extractMessage(Throwable exception) {
        Throwable cursor = exception;
        String message = exception.getClass().getSimpleName();
        while (cursor != null) {
            if (cursor.getMessage() != null && !cursor.getMessage().isBlank()) {
                message = cursor.getMessage();
            }
            cursor = cursor.getCause();
        }
        return message;
    }
}
