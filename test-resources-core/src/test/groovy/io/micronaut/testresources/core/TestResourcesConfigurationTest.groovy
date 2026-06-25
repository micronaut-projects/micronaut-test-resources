package io.micronaut.testresources.core

import spock.lang.Specification

class TestResourcesConfigurationTest extends Specification {

    void "enabled property is declared for configuration metadata"() {
        given:
        def metadata = getClass().classLoader.getResource('META-INF/spring-configuration-metadata.json').text

        expect:
        metadata.contains('"name":"test-resources.enabled"')
        metadata.contains('"type":"boolean"')
        metadata.contains('"sourceType":"' + TestResourcesConfiguration.name + '"')
    }
}
