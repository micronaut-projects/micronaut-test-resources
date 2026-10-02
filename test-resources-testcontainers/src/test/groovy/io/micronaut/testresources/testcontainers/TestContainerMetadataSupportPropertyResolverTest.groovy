package io.micronaut.testresources.testcontainers

import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.env.PropertySource
import io.micronaut.context.env.PropertySourcePropertyResolver
import io.micronaut.context.env.yaml.YamlPropertySourceLoader
import io.micronaut.testresources.core.PropertyResolverSupport
import spock.lang.Specification

import java.nio.charset.StandardCharsets

/**
 * Exercises {@link TestContainerMetadataSupport#convertToMetadata} through a real
 * Micronaut {@link io.micronaut.core.value.PropertyResolver}, unlike
 * {@link TestContainerMetadataSupportTest}, whose {@code flatten} helper bypasses
 * Micronaut's property resolution and so cannot observe how it normalizes keys.
 */
class TestContainerMetadataSupportPropertyResolverTest extends Specification {

    void "keeps environment variable names spelled as written when supplied as indexed property keys"() {
        given:
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.env[0].SOME_ENV_VAR"      : "some value",
                "test-resources.containers.foo.env[1].SOME_OTHER_ENV_VAR": "some other value",
            ])
        )
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        when:
        def md = TestContainerMetadataSupport.convertToMetadata(config, "foo")

        then:
        md.present
        with(md.get()) {
            env == [
                'SOME_ENV_VAR'      : 'some value',
                'SOME_OTHER_ENV_VAR': 'some other value'
            ]
        }
    }

    void "keeps label names spelled as written when supplied as indexed property keys"() {
        given:
        def resolver = new PropertySourcePropertyResolver(
            MapPropertySource.of("test", [
                "test-resources.containers.foo.labels[0].MyLabel": "value",
            ])
        )
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        when:
        def md = TestContainerMetadataSupport.convertToMetadata(config, "foo")

        then:
        md.present
        with(md.get()) {
            labels == ['MyLabel': 'value']
        }
    }

    void "keeps environment variable names spelled as written when loaded from a YAML sequence"() {
        given:
        def yaml = """
                test-resources:
                  containers:
                    foo:
                      env:
                        - SOME_ENV_VAR: some value
        """
        def loader = new YamlPropertySourceLoader()
        def parsed = loader.read("test.yml", new ByteArrayInputStream(yaml.stripIndent().getBytes(StandardCharsets.UTF_8)))
        def resolver = new PropertySourcePropertyResolver(PropertySource.of("test.yml", parsed))
        def config = PropertyResolverSupport.resolveTestResourcesConfiguration(resolver)

        when:
        def md = TestContainerMetadataSupport.convertToMetadata(config, "foo")

        then:
        md.present
        with(md.get()) {
            env == ['SOME_ENV_VAR': 'some value']
        }
    }
}
