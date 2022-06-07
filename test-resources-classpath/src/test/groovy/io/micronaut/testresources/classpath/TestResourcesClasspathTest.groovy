package io.micronaut.testresources.classpath

import spock.lang.Specification

class TestResourcesClasspathTest extends Specification {
    private Set<String> inferred = []

    def "always adds default modules"() {
        when:
        infer()

        then:
        inferredClasspathEquals(
                'io.micronaut.test:micronaut-test-resources-server:1.0.34',
                'io.micronaut.test:micronaut-test-resources-testcontainers:1.0.34'
        )
    }

    def "deduces #mn"() {
        when:
        infer("io.micronaut.kafka:micronaut-$mn:1.0")

        then:
        inferredClasspathEquals(
                'io.micronaut.test:micronaut-test-resources-server:1.0.34',
                'io.micronaut.test:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.test:micronaut-test-resources-$module:1.0.34"
        )

        where:
        mn      | module
        'kafka' | 'kafka'
        'mqtt'  | 'hivemq'
    }

    def "passes through JDBC driver #driver"() {
        when:
        infer("org:foo:1.0", "$driver:1.0")

        then:
        inferredClasspathEquals(
                'io.micronaut.test:micronaut-test-resources-server:1.0.34',
                'io.micronaut.test:micronaut-test-resources-testcontainers:1.0.34',
                "$driver:1.0"
        )

        where:
        driver << [
                'mysql:mysql-connector-java',
                'org.postgresql:postgresql',
                'org.mariadb.jdbc:mariadb-java-client'
        ]
    }

    def "infers Micronaut Data module"() {
        when:
        infer 'io.micronaut.data:micronaut-data-runtime:1.0', "$driver:1.0"

        then:
        inferredClasspathEquals(
                'io.micronaut.test:micronaut-test-resources-server:1.0.34',
                'io.micronaut.test:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.test:micronaut-test-resources-jdbc-$module:1.0.34",
                "$driver:1.0"
        )

        where:
        driver                                 | module
        'mysql:mysql-connector-java'           | 'mysql'
        'org.postgresql:postgresql'            | 'postgresql'
        'org.mariadb.jdbc:mariadb-java-client' | 'mariadb'

    }

    def "infers Micronaut Data Mongo"() {
        when:
        infer 'io.micronaut.data:micronaut-data-mongodb:1.0', "$driver:1.0"

        then:
        inferredClasspathEquals(
                'io.micronaut.test:micronaut-test-resources-server:1.0.34',
                'io.micronaut.test:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.test:micronaut-test-resources-mongodb:1.0.34",
                "$driver:1.0"
        )

        where:
        driver << [
                "org.mongodb:mongodb-driver-async",
                "org.mongodb:mongodb-driver-sync",
                "org.mongodb:mongodb-driver-reactivestreams"
        ]

    }

    private void inferredClasspathEquals(String... dependencies) {
        Set<String> expected = dependencies as SortedSet<String>
        Set<String> actual = inferred as SortedSet<String>
        assert actual == expected
    }

    private void infer(String... dependencies) {
        inferred = TestResourcesClasspath.inferTestResourcesClasspath(
                dependencies.collect {
                    def (g, a, v) = it.split(':') as List
                    new MavenDependency(g, a, v)
                },
                '1.0.34'
        ).collect(new LinkedHashSet<>()) {
            it.toString()
        }
    }
}
