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
package io.micronaut.testresources.codec;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Headers;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.body.MessageBodyHandler;
import io.micronaut.http.codec.CodecException;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static io.micronaut.testresources.codec.TestResourcesMediaType.TEST_RESOURCES_BINARY;
import static io.micronaut.testresources.codec.TestResourcesMediaType.TEST_RESOURCES_BINARY_MEDIA_TYPE;

/**
 * Message body handler for the test resources binary protocol.
 *
 * @param <T> The argument type
 */
@Singleton
@BootstrapContextCompatible
@Consumes(TEST_RESOURCES_BINARY)
@Produces(TEST_RESOURCES_BINARY)
public class TestResourcesBodyHandler<T> implements MessageBodyHandler<T> {
    @Override
    public boolean isReadable(Argument<T> type, MediaType mediaType) {
        return mediaType != null && mediaType.matches(TEST_RESOURCES_BINARY_MEDIA_TYPE);
    }

    @Override
    public boolean isWriteable(Argument<T> type, MediaType mediaType) {
        return mediaType != null && mediaType.matches(TEST_RESOURCES_BINARY_MEDIA_TYPE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(Argument<T> type, MediaType mediaType, Headers httpHeaders, InputStream inputStream) throws CodecException {
        try {
            Object value = TestResourcesCodec.readValue(inputStream);
            if (value instanceof Map<?, ?> map && type.getType().equals(ConvertibleValues.class)) {
                return (T) ConvertibleValues.of((Map<String, Object>) map);
            }
            if (Result.class.equals(type.getType())) {
                return (T) Result.of(value);
            }
            return (T) value;
        } catch (IOException e) {
            throw new CodecException("Unable to decode the test resources response", e);
        }
    }

    @Override
    public void writeTo(Argument<T> type, MediaType mediaType, T object, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
        try {
            outgoingHeaders.set(HttpHeaders.CONTENT_TYPE, TEST_RESOURCES_BINARY);
            TestResourcesCodec.writeValue(object, outputStream);
        } catch (IOException e) {
            throw new CodecException("Unable to encode the test resources response", e);
        }
    }
}
