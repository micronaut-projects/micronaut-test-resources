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
package io.micronaut.testresources.codec

import io.micronaut.http.codec.CodecException
import spock.lang.Specification

class TestResourcesCodecTest extends Specification {

    def "round-trips supported payload shapes"() {
        given:
        def payload = [
            flag   : true,
            count  : 42,
            big    : 9007199254740991L,
            text   : "value",
            values : ["a", null, 1, 2L, [nested: false]],
            nested : [name: "codec", enabled: true]
        ]

        when:
        def bytes = encode(payload)
        def decoded = TestResourcesCodec.readValue(new ByteArrayInputStream(bytes))

        then:
        decoded == payload
    }

    def "unwraps result values on write"() {
        when:
        def bytes = encode(Result.of("ok"))
        def decoded = TestResourcesCodec.readValue(new ByteArrayInputStream(bytes))

        then:
        decoded == "ok"
    }

    def "encodes generic collections as list payloads"() {
        given:
        def payload = [entries: new LinkedHashSet<>(["alpha", "beta"])]

        when:
        def bytes = encode(payload)
        def decoded = TestResourcesCodec.readValue(new ByteArrayInputStream(bytes))

        then:
        decoded == [entries: ["alpha", "beta"]]
    }

    def "rejects unsupported types"() {
        when:
        encode([new Date()])

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported type: java.util.Date"
    }

    def "rejects non string map keys"() {
        when:
        encode([(1): "value"])

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported map key type: java.lang.Integer"
    }

    def "rejects invalid header"() {
        when:
        TestResourcesCodec.readValue(new ByteArrayInputStream([0, 0, 0, 0, 1, 2, 3] as byte[]))

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported test resources payload header"
    }

    def "rejects unsupported protocol version"() {
        given:
        def bytes = encode("ok")
        bytes[4] = (byte) 99

        when:
        TestResourcesCodec.readValue(new ByteArrayInputStream(bytes))

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported test resources protocol version: 99"
    }

    def "rejects negative collection sizes before allocation"() {
        when:
        TestResourcesCodec.readValue(new ByteArrayInputStream(payloadWithCollectionSize(TestResourcesCodec.SupportedType.LIST, -1)))

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported list size: -1"
    }

    def "rejects oversized collection sizes before allocation"() {
        when:
        TestResourcesCodec.readValue(new ByteArrayInputStream(payloadWithCollectionSize(
            TestResourcesCodec.SupportedType.MAP,
            TestResourcesCodec.MAX_COLLECTION_ELEMENTS + 1
        )))

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported map size: ${TestResourcesCodec.MAX_COLLECTION_ELEMENTS + 1} (max ${TestResourcesCodec.MAX_COLLECTION_ELEMENTS})"
    }

    def "rejects deeply nested lists before stack exhaustion"() {
        when:
        TestResourcesCodec.readValue(new ByteArrayInputStream(payloadWithNestedLists(TestResourcesCodec.MAX_NESTING_DEPTH + 1)))

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported nesting depth: ${TestResourcesCodec.MAX_NESTING_DEPTH + 1} (max ${TestResourcesCodec.MAX_NESTING_DEPTH})"
    }

    def "rejects deeply nested maps before stack exhaustion"() {
        when:
        TestResourcesCodec.readValue(new ByteArrayInputStream(payloadWithNestedMaps(TestResourcesCodec.MAX_NESTING_DEPTH + 1)))

        then:
        def ex = thrown(CodecException)
        ex.message == "Unsupported nesting depth: ${TestResourcesCodec.MAX_NESTING_DEPTH + 1} (max ${TestResourcesCodec.MAX_NESTING_DEPTH})"
    }

    private static byte[] encode(Object value) {
        def output = new ByteArrayOutputStream()
        TestResourcesCodec.writeValue(value, output)
        output.toByteArray()
    }

    private static byte[] payloadWithCollectionSize(TestResourcesCodec.SupportedType type, int size) {
        def output = new ByteArrayOutputStream()
        def data = new DataOutputStream(output)
        data.writeInt(TestResourcesCodec.MAGIC)
        data.writeByte(TestResourcesCodec.VERSION)
        data.writeByte(type.asByte())
        data.writeInt(size)
        data.flush()
        output.toByteArray()
    }

    private static byte[] payloadWithNestedLists(int depth) {
        def output = new ByteArrayOutputStream()
        def data = new DataOutputStream(output)
        data.writeInt(TestResourcesCodec.MAGIC)
        data.writeByte(TestResourcesCodec.VERSION)
        depth.times {
            data.writeByte(TestResourcesCodec.SupportedType.LIST.asByte())
            data.writeInt(1)
        }
        data.writeByte(TestResourcesCodec.SupportedType.NULL.asByte())
        data.flush()
        output.toByteArray()
    }

    private static byte[] payloadWithNestedMaps(int depth) {
        def output = new ByteArrayOutputStream()
        def data = new DataOutputStream(output)
        data.writeInt(TestResourcesCodec.MAGIC)
        data.writeByte(TestResourcesCodec.VERSION)
        depth.times {
            data.writeByte(TestResourcesCodec.SupportedType.MAP.asByte())
            data.writeInt(1)
            data.writeUTF("nested")
        }
        data.writeByte(TestResourcesCodec.SupportedType.NULL.asByte())
        data.flush()
        output.toByteArray()
    }
}
