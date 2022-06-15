package io.micronaut.testresources.buildtools

import spock.lang.Specification

class TestResourcesClasspathTest extends Specification {
    private Set<String> inferred = []

    def "always adds default modules"() {
        when:
        infer()

        then:
        inferredClasspathEquals(
                'io.micronaut.testresources:micronaut-test-resources-server:1.0.34',
                'io.micronaut.testresources:micronaut-test-resources-testcontainers:1.0.34'
        )
    }

    def "deduces #mn"() {
        when:
        infer("io.micronaut.kafka:micronaut-$mn:1.0")

        then:
        inferredClasspathEquals(
                'io.micronaut.testresources:micronaut-test-resources-server:1.0.34',
                'io.micronaut.testresources:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.testresources:micronaut-test-resources-$module:1.0.34"
        )

        where:
        mn              | module
        'kafka'         | 'kafka'
        'mqtt'          | 'hivemq'
        'neo4j-bolt'    | 'neo4j'
        'rabbitmq'      | 'rabbitmq'
        'redis-lettuce' | 'redis'
    }

    def "passes through driver #driver"() {
        when:
        infer("org:foo:1.0", "$driver:1.0")

        then:
        inferredClasspathEquals(
                'io.micronaut.testresources:micronaut-test-resources-server:1.0.34',
                'io.micronaut.testresources:micronaut-test-resources-testcontainers:1.0.34',
                "$driver:1.0"
        )

        where:
        driver << [
                'mysql:mysql-connector-java',
                'org.postgresql:postgresql',
                'org.mariadb.jdbc:mariadb-java-client',
                'com.oracle.database.jdbc:ojdbc5',
                'com.oracle.database.jdbc:ojdbc6',
                'com.oracle.database.jdbc:ojdbc8',
                'com.oracle.database.jdbc:ojdbc10',
                'com.oracle.database.jdbc:ojdbc11',
                'dev.miku:r2dbc-mysql',
                'org.mariadb:r2dbc-mariadb',
                'org.postgresql:r2dbc-postgresql',
                'com.oracle.database.r2dbc:oracle-r2dbc',
                'com.microsoft.sqlserver:mssql-jdbc',
                'io.r2dbc:r2dbc-mssql'
        ]
    }

    def "infers Micronaut Data module"() {
        when:
        infer 'io.micronaut.data:micronaut-data-runtime:1.0', "$driver:1.0"

        then:
        inferredClasspathEquals(
                'io.micronaut.testresources:micronaut-test-resources-server:1.0.34',
                'io.micronaut.testresources:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.testresources:micronaut-test-resources-jdbc-$module:1.0.34",
                "$driver:1.0"
        )

        where:
        driver                                 | module
        'mysql:mysql-connector-java'           | 'mysql'
        'org.postgresql:postgresql'            | 'postgresql'
        'org.mariadb.jdbc:mariadb-java-client' | 'mariadb'
        'com.oracle.database.jdbc:ojdbc8'      | 'oracle-xe'
        'com.microsoft.sqlserver:mssql-jdbc'   | 'mssql'
    }

    def "infers Micronaut Data Mongo"() {
        when:
        infer 'io.micronaut.data:micronaut-data-mongodb:1.0', "$driver:1.0"

        then:
        inferredClasspathEquals(
                'io.micronaut.testresources:micronaut-test-resources-server:1.0.34',
                'io.micronaut.testresources:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.testresources:micronaut-test-resources-mongodb:1.0.34",
                "$driver:1.0"
        )

        where:
        driver << [
                "org.mongodb:mongodb-driver-async",
                "org.mongodb:mongodb-driver-sync",
                "org.mongodb:mongodb-driver-reactivestreams"
        ]

    }

    def "infers Micronaut Data R2DBC #driver"() {
        when:
        infer 'io.micronaut.data:micronaut-data-r2dbc:1.0', "$driver:1.0"

        then:
        inferredClasspathEquals(
                'io.micronaut.testresources:micronaut-test-resources-server:1.0.34',
                'io.micronaut.testresources:micronaut-test-resources-testcontainers:1.0.34',
                "io.micronaut.testresources:micronaut-test-resources-r2dbc-$module:1.0.34",
                "$driver:1.0"
        )

        where:
        driver                                   | module
        'dev.miku:r2dbc-mysql'                   | 'mysql'
        'org.mariadb:r2dbc-mariadb'              | 'mariadb'
        'org.postgresql:r2dbc-postgresql'        | 'postgresql'
        'com.oracle.database.r2dbc:oracle-r2dbc' | 'oracle-xe'
        'io.r2dbc:r2dbc-mssql'                   | 'mssql'
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
