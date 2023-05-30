/*
 * Copyright 2017-2023 original authors
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
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.body.MessageBodyHandler;
import io.micronaut.http.codec.CodecException;
import jakarta.inject.Singleton;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static io.micronaut.testresources.codec.TestResourcesMediaType.TEST_RESOURCES_BINARY;
import static io.micronaut.testresources.codec.TestResourcesMediaType.TEST_RESOURCES_BINARY_MEDIA_TYPE;

/**
 * The test resources binary protocol body handler.
 *
 * @param <T> the type of the arguments
 * @since 2.0.0
 */
@Singleton
@Consumes(TEST_RESOURCES_BINARY)
@Produces(TEST_RESOURCES_BINARY)
@BootstrapContextCompatible
public class TestResourcesBodyHandler<T> implements MessageBodyHandler<T> {
    @Override
    public boolean isReadable(Argument<T> type, MediaType mediaType) {
        return mediaType.matches(TEST_RESOURCES_BINARY_MEDIA_TYPE);
    }

    @Override
    public boolean isWriteable(Argument<T> type, MediaType mediaType) {
        return mediaType.matches(TEST_RESOURCES_BINARY_MEDIA_TYPE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(Argument<T> type, MediaType mediaType, Headers httpHeaders, InputStream inputStream) throws CodecException {
        try {
            var dis = new DataInputStream(inputStream);
            var result = TestResourcesCodec.readObject(dis);
            if (result instanceof Map map && type.getType().equals(ConvertibleValues.class)) {
                return (T) ConvertibleValues.of(map);
            }
            if (Result.class.equals(type.getType())) {
                return (T) Result.of(result);
            }
            return (T) result;
        } catch (IOException e) {
            throw new CodecException("Invalid binary stream", e);
        }
    }

    @Override
    public void writeTo(Argument<T> type, MediaType mediaType, T object, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
        try {
            var dos = new DataOutputStream(outputStream);
            outgoingHeaders.set("Content-Type", TEST_RESOURCES_BINARY_MEDIA_TYPE);
            TestResourcesCodec.writeObject(object, dos);
        } catch (IOException e) {
            throw new CodecException("Invalid binary stream", e);
        }
    }

}
