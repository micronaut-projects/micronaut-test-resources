package io.micronaut.testresources.client

import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.UUID

class IntellijIdeaDatasourceExporterTest extends Specification {
    @TempDir
    Path tempDir

    def "exporter stays disabled by default"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", [:])
        exporter.export("datasources.default.username", "user", [:])
        exporter.export("datasources.default.password", "secret", [:])
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", [:])

        then:
        !Files.exists(tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH))
    }

    def "exporter uses the micronaut test-resources convention by default"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", config)
        exporter.export("datasources.default.username", "user", config)
        exporter.export("datasources.default.password", "secret", config)
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", config)

        then:
        Files.exists(tempDir.resolve(".micronaut/test-resources/intellij-idea-datasources.xml"))
    }

    def "exporter writes deterministic output for multiple datasources"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]

        when:
        exporter.export("datasources.analytics.url", "jdbc:mysql://127.0.0.1:3306/analytics", config)
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", config)
        exporter.export("datasources.analytics.username", "analytics_user", config)
        exporter.export("datasources.default.username", "demo_user", config)
        exporter.export("datasources.analytics.password", "analytics_secret", config)
        exporter.export("datasources.default.password", "demo_secret", config)
        exporter.export("datasources.analytics.driver-class-name", "com.mysql.cj.jdbc.Driver", config)
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", config)

        then:
        def outputFile = tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH)
        Files.exists(outputFile)

        and:
        def output = Files.readString(outputFile)
        def analyticsUuid = UUID.nameUUIDFromBytes("intellij-idea-datasource:analytics".getBytes(StandardCharsets.UTF_8))
        def defaultUuid = UUID.nameUUIDFromBytes("intellij-idea-datasource:default".getBytes(StandardCharsets.UTF_8))
        output == """#DataSourceSettings#
#LocalDataSource: analytics
#BEGIN#
<data-source source="LOCAL" name="analytics" uuid="${analyticsUuid}"><database-info product="" version="" jdbc-version="" driver-name="" driver-version="" dbms="MYSQL"/><driver-ref>mysql</driver-ref><synchronize>true</synchronize><jdbc-driver>com.mysql.cj.jdbc.Driver</jdbc-driver><jdbc-url>jdbc:mysql://127.0.0.1:3306/analytics</jdbc-url><user-name>analytics_user</user-name><password>analytics_secret</password><working-dir>\$ProjectFileDir\$</working-dir></data-source>
#END#
#LocalDataSource: default
#BEGIN#
<data-source source="LOCAL" name="default" uuid="${defaultUuid}"><database-info product="" version="" jdbc-version="" driver-name="" driver-version="" dbms="POSTGRES"/><driver-ref>postgresql</driver-ref><synchronize>true</synchronize><jdbc-driver>org.postgresql.Driver</jdbc-driver><jdbc-url>jdbc:postgresql://localhost:5432/demo</jdbc-url><user-name>demo_user</user-name><password>demo_secret</password><working-dir>\$ProjectFileDir\$</working-dir></data-source>
#END#
"""
    }

    def "partial property resolution does not create malformed output"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", config)
        exporter.export("datasources.default.username", "demo_user", config)

        then:
        !Files.exists(tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH))
    }

    def "configured output path overrides the default"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def outputFile = tempDir.resolve("custom/idea-export.xml")
        def config = [
            (IntellijIdeaDatasourceExporter.ENABLED): true,
            (IntellijIdeaDatasourceExporter.OUTPUT_PATH): "custom/idea-export.xml"
        ]

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", config)
        exporter.export("datasources.default.username", "demo_user", config)
        exporter.export("datasources.default.password", "demo_secret", config)
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", config)

        then:
        Files.exists(outputFile)
        !Files.exists(tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH))
    }

    @RestoreSystemProperties
    def "system properties can enable the exporter"() {
        given:
        System.setProperty("micronaut.test.resources.${IntellijIdeaDatasourceExporter.ENABLED}".toString(), "true")
        System.setProperty("micronaut.test.resources.${IntellijIdeaDatasourceExporter.OUTPUT_PATH}".toString(), "system/idea-export.xml")
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", [:])
        exporter.export("datasources.default.username", "demo_user", [:])
        exporter.export("datasources.default.password", "demo_secret", [:])
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", [:])

        then:
        Files.exists(tempDir.resolve("system/idea-export.xml"))
    }
}
