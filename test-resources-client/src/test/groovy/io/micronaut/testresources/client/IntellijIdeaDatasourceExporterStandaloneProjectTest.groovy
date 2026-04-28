package io.micronaut.testresources.client

import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Timeout

import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

@Timeout(600)
class IntellijIdeaDatasourceExporterStandaloneProjectTest extends Specification {
    private static final List<String> SNAPSHOT_PUBLISH_TASKS = [
        ':micronaut-test-resources-build-tools:publishToMavenLocal',
        ':micronaut-test-resources-codec:publishToMavenLocal',
        ':micronaut-test-resources-core:publishToMavenLocal',
        ':micronaut-test-resources-client:publishToMavenLocal',
        ':micronaut-test-resources-embedded:publishToMavenLocal',
        ':micronaut-test-resources-testcontainers:publishToMavenLocal',
        ':micronaut-test-resources-server:publishToMavenLocal',
        ':micronaut-test-resources-jdbc-core:publishToMavenLocal',
        ':micronaut-test-resources-jdbc-postgresql:publishToMavenLocal'
    ]

    @TempDir
    Path tempDir

    def "standalone Gradle project writes IntelliJ IDEA datasource export file"() {
        given:
        Path repoRoot = findRepositoryRoot()
        Path mavenRepo = tempDir.resolve("maven-repo")
        Path sampleProject = tempDir.resolve("sample-project")
        Path outputFile = sampleProject.resolve("build/intellij-idea-datasources.xml")
        String projectVersion = propertyValue(repoRoot.resolve("gradle.properties"), "projectVersion")
        String micronautPlatformVersion = versionCatalogValue(repoRoot.resolve("gradle/libs.versions.toml"), "micronaut-platform")
        String micronautGradlePluginVersion = versionCatalogValue(repoRoot.resolve("gradle/libs.versions.toml"), "micronaut-gradle-plugin")
        writeStandaloneProject(sampleProject, "intellij-export-sample", outputFile, projectVersion, micronautPlatformVersion, micronautGradlePluginVersion)

        when:
        CommandResult publish = runGradle(repoRoot, ["--console=plain", "--no-daemon", "-Dmaven.repo.local=${mavenRepo}".toString()] + SNAPSHOT_PUBLISH_TASKS)
        CommandResult build = runGradle(repoRoot, [
            "--console=plain",
            "--no-daemon",
            "-p", sampleProject.toString(),
            "test",
            "--tests", "example.ExportSpec",
            "-Dsample.repo=${mavenRepo.toUri()}".toString()
        ])

        then:
        assert publish.exitCode == 0: publish.output
        assert build.exitCode == 0: build.output
        Files.exists(outputFile)

        and:
        def output = Files.readString(outputFile)
        output.contains("#LocalDataSource: default")
        output.contains("<jdbc-driver>org.postgresql.Driver</jdbc-driver>")
        output.contains("<jdbc-url>jdbc:postgresql://")
        output.contains("<user-name>test</user-name>")
        output.contains("<password>test</password>")
    }

    def "standalone Gradle daemon reuse writes the default IntelliJ IDEA export into the current project"() {
        given:
        Path repoRoot = findRepositoryRoot()
        Path mavenRepo = tempDir.resolve("maven-repo")
        Path gradleUserHome = tempDir.resolve("gradle-user-home")
        Path firstProject = tempDir.resolve("first-project")
        Path secondProject = tempDir.resolve("second-project")
        Path firstOutput = firstProject.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH)
        Path secondOutput = secondProject.resolve(IntellijIdeaDatasourceExporter.DEFAULT_OUTPUT_PATH)
        String projectVersion = propertyValue(repoRoot.resolve("gradle.properties"), "projectVersion")
        String micronautPlatformVersion = versionCatalogValue(repoRoot.resolve("gradle/libs.versions.toml"), "micronaut-platform")
        String micronautGradlePluginVersion = versionCatalogValue(repoRoot.resolve("gradle/libs.versions.toml"), "micronaut-gradle-plugin")
        writeStandaloneProject(firstProject, "intellij-export-first", null, projectVersion, micronautPlatformVersion, micronautGradlePluginVersion)
        writeStandaloneProject(secondProject, "intellij-export-second", null, projectVersion, micronautPlatformVersion, micronautGradlePluginVersion)

        when:
        CommandResult publish = runGradle(repoRoot, ["--console=plain", "--no-daemon", "-Dmaven.repo.local=${mavenRepo}".toString()] + SNAPSHOT_PUBLISH_TASKS)
        CommandResult firstBuild = runGradle(repoRoot, [
            "--console=plain",
            "--gradle-user-home", gradleUserHome.toString(),
            "-p", firstProject.toString(),
            "test",
            "--tests", "example.ExportSpec",
            "-Dsample.repo=${mavenRepo.toUri()}".toString()
        ])

        then:
        assert publish.exitCode == 0: publish.output
        assert firstBuild.exitCode == 0: firstBuild.output
        Files.exists(firstOutput)
        !Files.exists(secondOutput)

        when:
        Files.delete(firstOutput)
        CommandResult secondBuild = runGradle(repoRoot, [
            "--console=plain",
            "--gradle-user-home", gradleUserHome.toString(),
            "-p", secondProject.toString(),
            "test",
            "--tests", "example.ExportSpec",
            "-Dsample.repo=${mavenRepo.toUri()}".toString()
        ])

        then:
        assert secondBuild.exitCode == 0: secondBuild.output
        !Files.exists(firstOutput)
        Files.exists(secondOutput)

        and:
        def output = Files.readString(secondOutput)
        output.contains("#LocalDataSource: default")
        output.contains("<jdbc-driver>org.postgresql.Driver</jdbc-driver>")
        output.contains("<jdbc-url>jdbc:postgresql://")
    }

    private static void writeStandaloneProject(Path sampleProject,
                                               String projectName,
                                               Path outputFile,
                                               String projectVersion,
                                               String micronautPlatformVersion,
                                               String micronautGradlePluginVersion) {
        Files.createDirectories(sampleProject.resolve("src/test/groovy/example"))
        Files.createDirectories(sampleProject.resolve("src/test/resources"))

        Files.writeString(sampleProject.resolve("settings.gradle"), """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            rootProject.name = '${projectName}'
            """.stripIndent())

        Files.writeString(sampleProject.resolve("build.gradle"), """
            import io.micronaut.gradle.testresources.StartTestResourcesService

            plugins {
                id 'groovy'
                id 'io.micronaut.application' version '${micronautGradlePluginVersion}'
                id 'io.micronaut.test-resources' version '${micronautGradlePluginVersion}'
            }

            repositories {
                mavenCentral()
                maven {
                    url = uri(System.getProperty('sample.repo'))
                }
            }

            micronaut {
                version = '${micronautPlatformVersion}'
                runtime('netty')
                testRuntime('spock2')
                processing {
                    incremental true
                    annotations 'example.*'
                }
                testResources {
                    version = '${projectVersion}'
                }
            }

            dependencies {
                implementation 'io.micronaut:micronaut-runtime'
                implementation 'io.micronaut.data:micronaut-data-jdbc'
                implementation 'io.micronaut.sql:micronaut-jdbc-hikari'
                runtimeOnly 'org.postgresql:postgresql'
                runtimeOnly 'io.micronaut:micronaut-jackson-databind'
                runtimeOnly 'org.yaml:snakeyaml'
                testImplementation 'io.micronaut.test:micronaut-test-spock'
                testImplementation 'org.apache.groovy:groovy'
            }

            test {
                useJUnitPlatform()
            }

            tasks.withType(StartTestResourcesService).configureEach {
                useClassDataSharing = false
            }
            """.stripIndent())

        String applicationConfig = outputFile == null ? """
            datasources:
              default:
                db-type: postgres
                schema-generate: CREATE_DROP

            test-resources:
              intellij-idea:
                enabled: true
            """.stripIndent() : """
            datasources:
              default:
                db-type: postgres
                schema-generate: CREATE_DROP

            test-resources:
              intellij-idea:
                enabled: true
                output-path: ${outputFile}
            """.stripIndent()

        Files.writeString(sampleProject.resolve("src/test/resources/application-test.yml"), applicationConfig)

        Files.writeString(sampleProject.resolve("src/test/groovy/example/ExportSpec.groovy"), """
            package example

            import io.micronaut.context.ApplicationContext
            import io.micronaut.test.extensions.spock.annotation.MicronautTest
            import jakarta.inject.Inject
            import spock.lang.Specification

            @MicronautTest(transactional = false)
            class ExportSpec extends Specification {
                @Inject
                ApplicationContext applicationContext

                void 'resolves datasource properties'() {
                    expect:
                    applicationContext.getRequiredProperty('datasources.default.url', String).startsWith('jdbc:postgresql://')
                    applicationContext.getRequiredProperty('datasources.default.username', String)
                    applicationContext.getRequiredProperty('datasources.default.password', String)
                    applicationContext.getRequiredProperty('datasources.default.driver-class-name', String) == 'org.postgresql.Driver'
                }
            }
            """.stripIndent())
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize()
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties"))) {
                return current
            }
            current = current.parent
        }
        throw new IllegalStateException("Unable to locate repository root from ${Path.of('').toAbsolutePath()}")
    }

    private static String propertyValue(Path file, String key) {
        String pattern = "(?m)^" + Pattern.quote(key) + "=(.+)\$"
        def matcher = Files.readString(file) =~ pattern
        assert matcher.find(): "Unable to find ${key} in ${file}"
        matcher.group(1).trim()
    }

    private static String versionCatalogValue(Path file, String key) {
        String pattern = "(?m)^" + Pattern.quote(key) + "\\s*=\\s*\"([^\"]+)\""
        def matcher = Files.readString(file) =~ pattern
        assert matcher.find(): "Unable to find ${key} in ${file}"
        matcher.group(1)
    }

    private static CommandResult runGradle(Path repoRoot, List<String> arguments) {
        List<String> command = [repoRoot.resolve("gradlew").toString()] + arguments
        Process process = new ProcessBuilder(command)
            .directory(repoRoot.toFile())
            .redirectErrorStream(true)
            .start()
        String output = process.inputStream.text
        int exitCode = process.waitFor()
        new CommandResult(exitCode, output)
    }

    private record CommandResult(int exitCode, String output) {
    }
}
