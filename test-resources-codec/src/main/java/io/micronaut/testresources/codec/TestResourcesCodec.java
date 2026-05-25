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

import io.micronaut.http.codec.CodecException;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binary codec used between the test resources client and server.
 */
public final class TestResourcesCodec {
    static final int MAGIC = 0x54524231; // TRB1
    static final byte VERSION = 1;
    static final int MAX_COLLECTION_ELEMENTS = 10_000;
    static final int MAX_NESTING_DEPTH = 256;

    private TestResourcesCodec() {
    }

    public static @Nullable Object readValue(InputStream inputStream) throws IOException {
        var input = new DataInputStream(inputStream);
        validateEnvelope(input);
        return readObject(input, 0);
    }

    public static void writeValue(@Nullable Object object, OutputStream outputStream) throws IOException {
        var output = new DataOutputStream(outputStream);
        output.writeInt(MAGIC);
        output.writeByte(VERSION);
        writeObject(object, output);
        output.flush();
    }

    private static void validateEnvelope(DataInputStream input) throws IOException {
        var magic = input.readInt();
        if (magic != MAGIC) {
            throw new CodecException("Unsupported test resources payload header");
        }
        var version = input.readByte();
        if (version != VERSION) {
            throw new CodecException("Unsupported test resources protocol version: " + Byte.toUnsignedInt(version));
        }
    }

    private static @Nullable Object readObject(DataInputStream input, int depth) throws IOException {
        var kind = SupportedType.of(input.readByte());
        return switch (kind) {
            case NULL -> null;
            case BOOLEAN -> Boolean.valueOf(input.readBoolean());
            case INTEGER -> Integer.valueOf(input.readInt());
            case LONG -> Long.valueOf(input.readLong());
            case STRING -> input.readUTF();
            case LIST -> {
                int nestedDepth = validateNestingDepth(depth + 1);
                int count = validateCollectionSize("list", input.readInt());
                List<Object> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    list.add(readObject(input, nestedDepth));
                }
                yield Collections.unmodifiableList(list);
            }
            case MAP -> {
                int nestedDepth = validateNestingDepth(depth + 1);
                int count = validateCollectionSize("map", input.readInt());
                Map<String, Object> map = new LinkedHashMap<>(count);
                for (int i = 0; i < count; i++) {
                    map.put(input.readUTF(), readObject(input, nestedDepth));
                }
                yield Collections.unmodifiableMap(map);
            }
        };
    }

    private static void writeObject(@Nullable Object object, DataOutputStream output) throws IOException {
        writeObject(object, output, 0);
    }

    @SuppressWarnings("unchecked")
    private static void writeObject(@Nullable Object object, DataOutputStream output, int depth) throws IOException {
        if (object instanceof Result<?> result) {
            writeObject(result.value(), output, depth);
            return;
        }
        if (object == null) {
            output.writeByte(SupportedType.NULL.asByte());
            return;
        }
        var kind = SupportedType.kindOf(object);
        output.writeByte(kind.asByte());
        switch (kind) {
            case BOOLEAN -> output.writeBoolean((Boolean) object);
            case INTEGER -> output.writeInt((Integer) object);
            case LONG -> output.writeLong((Long) object);
            case STRING -> output.writeUTF((String) object);
            case LIST -> {
                int nestedDepth = validateNestingDepth(depth + 1);
                Collection<?> collection = (Collection<?>) object;
                validateCollectionSize("list", collection.size());
                output.writeInt(collection.size());
                for (Object value : collection) {
                    writeObject(value, output, nestedDepth);
                }
            }
            case MAP -> {
                int nestedDepth = validateNestingDepth(depth + 1);
                Map<?, ?> map = (Map<?, ?>) object;
                validateCollectionSize("map", map.size());
                output.writeInt(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new CodecException("Unsupported map key type: " + typeName(entry.getKey()));
                    }
                    output.writeUTF(key);
                    writeObject(entry.getValue(), output, nestedDepth);
                }
            }
            default -> throw new CodecException("Unsupported value kind: " + kind);
        }
    }

    enum SupportedType {
        NULL,
        BOOLEAN,
        INTEGER,
        LONG,
        STRING,
        LIST,
        MAP;

        byte asByte() {
            return (byte) ordinal();
        }

        static SupportedType of(byte value) {
            if (value < 0 || value >= values().length) {
                throw new CodecException("Unsupported value kind: " + value);
            }
            return values()[value];
        }

        static SupportedType kindOf(Object value) {
            if (value instanceof Boolean) {
                return BOOLEAN;
            }
            if (value instanceof Integer) {
                return INTEGER;
            }
            if (value instanceof Long) {
                return LONG;
            }
            if (value instanceof String) {
                return STRING;
            }
            if (value instanceof Collection<?>) {
                return LIST;
            }
            if (value instanceof Map<?, ?>) {
                return MAP;
            }
            throw new CodecException("Unsupported type: " + value.getClass().getName());
        }
    }

    private static String typeName(@Nullable Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static int validateCollectionSize(String collectionType, int count) {
        if (count < 0) {
            throw new CodecException("Unsupported " + collectionType + " size: " + count);
        }
        if (count > MAX_COLLECTION_ELEMENTS) {
            throw new CodecException("Unsupported " + collectionType + " size: " + count + " (max " + MAX_COLLECTION_ELEMENTS + ")");
        }
        return count;
    }

    private static int validateNestingDepth(int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new CodecException("Unsupported nesting depth: " + depth + " (max " + MAX_NESTING_DEPTH + ")");
        }
        return depth;
    }
}
