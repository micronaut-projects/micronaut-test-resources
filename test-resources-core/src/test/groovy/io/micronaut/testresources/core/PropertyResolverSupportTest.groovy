package io.micronaut.testresources.core

import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.env.PropertySourcePropertyResolver
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.naming.conventions.StringConvention
import io.micronaut.core.value.PropertyResolver
import spock.lang.Specification

class PropertyResolverSupportTest extends Specification {

    void "keeps map keys of indexed entries spelled as written"() {
        given:
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.env[0].SOME_ENV_VAR"        : "some value",
                "test-resources.containers.foo.env[1].OTHER_ENV_VAR"       : "value.with.dots=and-equals",
                "test-resources.containers.foo.image-name"                 : "some/image",
                "test-resources.containers.foo.exposed-ports[0].myPort"    : "8080",
            ])
        )

        when:
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        then: "indexed env entries keep the spelling the user wrote"
        config["containers.foo.env"] == [
            ["SOME_ENV_VAR": "some value"],
            ["OTHER_ENV_VAR": "value.with.dots=and-equals"]
        ]

        and: "non-indexed structural keys resolve exactly as before"
        config["containers.foo.image-name"] == "some/image"

        and: "camelCase map keys of other indexed lists keep their spelling too"
        config["containers.foo.exposed-ports"] == [
            ["myPort": "8080"]
        ]
    }

    void "overlays the raw aggregated list onto the generated catalog without mutating the resolver's own maps"() {
        given: "a hyphenated generated aggregate and a raw aggregate spelled as written, for the same key"
        def generatedList = [["some-env-var": "some value"]]
        def rawList = [["SOME_ENV_VAR": "some value"]]
        def generated = ["containers.foo.env": generatedList]
        def raw = ["containers.foo.env": rawList]
        def resolver = new FakePropertyResolver(generated: generated, raw: raw)

        when:
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        then: "the raw aggregate, which spells the key as written, overlays the generated one position by position"
        config["containers.foo.env"] == rawList

        and: "the resolver's own maps and lists are left untouched"
        generated["containers.foo.env"].is(generatedList)
        raw["containers.foo.env"].is(rawList)
        !config.is(generated)
    }

    void "does not drop entries when two spellings of the same base name target different indices"() {
        given: "camelCase and hyphenated spellings of the same base name writing different indices, with keys that normalization would mangle"
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.env[0].X_ONE"  : "1",
                "test-resources.containers.foo.ENV[1].Y_TWO"  : "2",
            ])
        )

        when:
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        then: "each index keeps the key its own spelling contributed, spelled as written rather than as x-one and y-two"
        config["containers.foo.env"] == [
            ["X_ONE": "1"],
            ["Y_TWO": "2"]
        ]
    }

    void "does not drop keys when two spellings of the same base name target the same index"() {
        given: "camelCase and hyphenated spellings of the same base name, both writing a key at index 0"
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.env[0].X_ONE" : "1",
                "test-resources.containers.foo.ENV[0].Y_TWO" : "2",
            ])
        )

        when:
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        then: "both keys survive at that position, spelled as written, regardless of which spelling the merge visits last"
        def list = config["containers.foo.env"]
        list.size() == 1
        list[0].keySet() == ["X_ONE", "Y_TWO"] as Set
        list[0]["X_ONE"] == "1"
        list[0]["Y_TWO"] == "2"
    }

    void "does not drop keys when three spellings of the same base name target the same index"() {
        given: "three spellings of the same base name, all writing keys at index 0"
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.env[0].X_ONE"   : "1",
                "test-resources.containers.foo.ENV[0].Y_TWO"   : "2",
                "test-resources.containers.foo.Env[0].Z_THREE" : "3",
            ])
        )

        when:
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        then: "all three keys survive at that position, spelled as written"
        def list = config["containers.foo.env"]
        list.size() == 1
        list[0].keySet() == ["X_ONE", "Y_TWO", "Z_THREE"] as Set
        list[0]["X_ONE"] == "1"
        list[0]["Y_TWO"] == "2"
        list[0]["Z_THREE"] == "3"
    }

    void "respells dotted label names, which normalization lower-cases"() {
        given: "a mixed-case dotted label and the conventional all-lowercase OCI label"
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.labels[0].My.Label"                      : "v",
                "test-resources.containers.foo.labels[1].org.opencontainers.image.title": "t",
            ])
        )

        when:
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        then: "the mixed-case one is restored, and the already-lowercase one round-trips unchanged"
        config["containers.foo.labels"] == [
            ["My.Label": "v"],
            ["org.opencontainers.image.title": "t"]
        ]
    }

    private static final class FakePropertyResolver implements PropertyResolver {
        Map<String, Object> generated = [:]
        Map<String, Object> raw = [:]

        @Override
        Map<String, Object> getProperties(String name, StringConvention keyFormat) {
            return keyFormat == StringConvention.RAW ? raw : generated
        }

        @Override
        boolean containsProperty(String name) {
            return false
        }

        @Override
        boolean containsProperties(String name) {
            return false
        }

        @Override
        <T> Optional<T> getProperty(String name, ArgumentConversionContext<T> conversionContext) {
            return Optional.empty()
        }

        @Override
        Collection<List<String>> getPropertyPathMatches(String pathPattern) {
            return Collections.emptyList()
        }
    }
}
