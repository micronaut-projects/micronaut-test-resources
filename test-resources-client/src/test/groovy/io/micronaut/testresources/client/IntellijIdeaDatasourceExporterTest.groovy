package io.micronaut.testresources.client

import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.Locale
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
    def "project path system property overrides the default export base"() {
        given:
        def otherProjectDir = tempDir.resolve("other-project")
        Files.createDirectories(otherProjectDir)
        System.setProperty("micronaut.test.resources.${IntellijIdeaDatasourceExporter.PROJECT_PATH_URI}".toString(), otherProjectDir.toUri().toString())
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", config)
        exporter.export("datasources.default.username", "demo_user", config)
        exporter.export("datasources.default.password", "demo_secret", config)
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", config)

        then:
        Files.exists(otherProjectDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH))
        !Files.exists(tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH))
    }

    @RestoreSystemProperties
    def "driver detection uses a locale-stable lowercasing strategy"() {
        given:
        Locale defaultLocale = Locale.default
        Locale.setDefault(Locale.forLanguageTag("tr"))
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]

        when:
        exporter.export("datasources.default.url", "JDBC:MARIADB://localhost:3306/demo", config)
        exporter.export("datasources.default.username", "demo_user", config)
        exporter.export("datasources.default.password", "demo_secret", config)
        exporter.export("datasources.default.driver-class-name", "ORG.MARIADB.JDBC.DRIVER", config)

        then:
        def output = Files.readString(tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH))
        output.contains('<driver-ref>mariadb</driver-ref>')
        output.contains('dbms="MARIADB"')

        cleanup:
        Locale.setDefault(defaultLocale)
    }

    def "overlapping sessions sharing an output path merge and rewrite live datasource state"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]
        def outputFile = tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH)

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/demo", config, "first")
        exporter.export("datasources.default.username", "demo_user", config, "first")
        exporter.export("datasources.default.password", "demo_secret", config, "first")
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", config, "first")
        exporter.export("datasources.analytics.url", "jdbc:mysql://localhost:3306/analytics", config, "second")
        exporter.export("datasources.analytics.username", "analytics_user", config, "second")
        exporter.export("datasources.analytics.password", "analytics_secret", config, "second")
        exporter.export("datasources.analytics.driver-class-name", "com.mysql.cj.jdbc.Driver", config, "second")

        then:
        def combinedOutput = Files.readString(outputFile)
        combinedOutput.contains("#LocalDataSource: default")
        combinedOutput.contains("#LocalDataSource: analytics")

        when:
        exporter.clearSession("second")

        then:
        def firstOnlyOutput = Files.readString(outputFile)
        firstOnlyOutput.contains("#LocalDataSource: default")
        !firstOnlyOutput.contains("#LocalDataSource: analytics")

        when:
        exporter.clearSession("first")

        then:
        !Files.exists(outputFile)
    }

    def "overlapping sessions sharing an output path preserve same-name datasources"() {
        given:
        def exporter = new IntellijIdeaDatasourceExporter(tempDir)
        def config = [(IntellijIdeaDatasourceExporter.ENABLED): true]
        def outputFile = tempDir.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH)

        when:
        exporter.export("datasources.default.url", "jdbc:postgresql://localhost:5432/first", config, "first")
        exporter.export("datasources.default.username", "first_user", config, "first")
        exporter.export("datasources.default.password", "first_secret", config, "first")
        exporter.export("datasources.default.driver-class-name", "org.postgresql.Driver", config, "first")
        exporter.export("datasources.default.url", "jdbc:mysql://localhost:3306/second", config, "second")
        exporter.export("datasources.default.username", "second_user", config, "second")
        exporter.export("datasources.default.password", "second_secret", config, "second")
        exporter.export("datasources.default.driver-class-name", "com.mysql.cj.jdbc.Driver", config, "second")

        then:
        def combinedOutput = Files.readString(outputFile)
        combinedOutput.contains("#LocalDataSource: default\n")
        combinedOutput.contains("#LocalDataSource: default (2)\n")
        combinedOutput.contains("<jdbc-url>jdbc:postgresql://localhost:5432/first</jdbc-url>")
        combinedOutput.contains("<jdbc-url>jdbc:mysql://localhost:3306/second</jdbc-url>")
        combinedOutput.readLines().count { it == "#BEGIN#" } == 2

        when:
        exporter.clearSession("first")

        then:
        def secondOnlyOutput = Files.readString(outputFile)
        secondOnlyOutput.contains("#LocalDataSource: default\n")
        !secondOnlyOutput.contains("#LocalDataSource: default (2)\n")
        secondOnlyOutput.contains("<jdbc-url>jdbc:mysql://localhost:3306/second</jdbc-url>")
        !secondOnlyOutput.contains("<jdbc-url>jdbc:postgresql://localhost:5432/first</jdbc-url>")

        when:
        exporter.clearSession("second")

        then:
        !Files.exists(outputFile)
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
