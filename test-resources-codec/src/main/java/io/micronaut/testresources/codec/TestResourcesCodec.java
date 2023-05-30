/*
 * Copyright 2017-2021 original authors
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This codec is responsible for (de)serializing the binary stream
 * between the test resources client and server. It is used by the
 * message body handler in the controller, but also directly by
 * the test resources client to decode messages.
 *
 * @since 2.0.0
 */
public final class TestResourcesCodec {
    private TestResourcesCodec() {

    }

    public static <V> V readObject(DataInputStream dis) throws IOException {
        var kind = SupportedType.of(dis.readByte());
        return switch (kind) {
            case NULL -> null;
            case RESULT -> readObject(dis);
            case BOOLEAN -> cast(dis.readBoolean());
            case INTEGER -> cast(dis.readInt());
            case STRING -> cast(dis.readUTF());
            case LIST -> {
                int count = dis.readInt();
                List<Object> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    list.add(readObject(dis));
                }
                yield cast(Collections.unmodifiableList(list));
            }
            case MAP -> {
                int count = dis.readInt();
                Map<Object, Object> map = new HashMap<>();
                for (int i = 0; i < count; i++) {
                    Object key = readObject(dis);
                    Object value = readObject(dis);
                    map.put(key, value);
                }
                yield cast(Collections.unmodifiableMap(map));
            }
        };
    }

    public static void writeObject(Object object, DataOutputStream dos) throws IOException {
        var kind = SupportedType.kindOf(object);
        if (SupportedType.RESULT == kind) {
            writeObject(((Result<?>) object).value(), dos);
            return;
        }
        dos.write(kind.asByte());
        switch (kind) {
            case BOOLEAN -> dos.writeBoolean((Boolean) object);
            case INTEGER -> dos.writeInt((Integer) object);
            case STRING -> dos.writeUTF((String) object);
            case LIST -> {
                List<Object> list = cast(object);
                dos.writeInt(list.size());
                for (Object o : list) {
                    writeObject(o, dos);
                }
            }
            case MAP -> {
                Map<Object, Object> map = cast(object);
                dos.writeInt(map.size());
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    writeObject(entry.getKey(), dos);
                    writeObject(entry.getValue(), dos);
                }
            }
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <V> V cast(Object o) {
        return (V) o;
    }

    enum SupportedType {
        NULL,
        RESULT,
        BOOLEAN,
        INTEGER,
        STRING,
        LIST,
        MAP;

        byte asByte() {
            return (byte) ordinal();
        }

        static SupportedType of(byte b) {
            return SupportedType.values()[b];
        }

        public static SupportedType kindOf(Object o) {
            if (o == null) {
                return NULL;
            }
            var clazz = o.getClass();
            if (Result.class.equals(clazz)) {
                return RESULT;
            }
            if (Boolean.TYPE.equals(clazz) || Boolean.class.equals(clazz)) {
                return BOOLEAN;
            }
            if (String.class.equals(clazz)) {
                return STRING;
            }
            if (List.class.isAssignableFrom(clazz)) {
                return LIST;
            }
            if (Map.class.isAssignableFrom(clazz)) {
                return MAP;
            }
            throw new CodecException("Unsupported type " + clazz);
        }

    }
}
