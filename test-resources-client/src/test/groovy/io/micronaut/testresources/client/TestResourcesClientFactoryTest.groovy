package io.micronaut.testresources.client

import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

import static io.micronaut.testresources.client.ConfigFinder.systemPropertyNameOf

class TestResourcesClientFactoryTest extends Specification implements ClientCleanup {

    @TempDir
    Path tempDir

    def cleanup() {
        TestResourcesClientFactory.cachedClient = null
    }

    @RestoreSystemProperties
    def "system properties win over filesystem fallback"() {
        given:
        def currentDirectory = tempDir.resolve("application")
        Files.createDirectories(currentDirectory)
        writeSettingsFile(currentDirectory.resolve(".micronaut/test-resources/test-resources.properties"), "http://file-system")
        System.setProperty(systemPropertyNameOf(TestResourcesClient.SERVER_URI), "http://system-properties")

        when:
        def client = TestResourcesClientFactory.findByConvention(currentDirectory, tempDir.resolve("home"), null)

        then:
        client.present
        client.get() instanceof DefaultTestResourcesClient
        client.get().@baseUri == "http://system-properties"
    }

    def "existing local conventional file still resolves"() {
        given:
        def currentDirectory = tempDir.resolve("application")
        writeSettingsFile(currentDirectory.resolve(".micronaut/test-resources/test-resources.properties"), "http://local")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null) ==
            Optional.of(currentDirectory.resolve(".micronaut/test-resources/test-resources.properties"))
    }

    def "standalone module directory resolves nested settings file"() {
        given:
        def currentDirectory = tempDir.resolve("test-resources")
        def settingsFile = currentDirectory.resolve(".micronaut/test-resources/test-resources-settings/test-resources.properties")
        writeSettingsFile(settingsFile, "http://standalone")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null) ==
            Optional.of(settingsFile)
    }

    def "consumer module directory resolves standalone settings file via ancestor lookup"() {
        given:
        def currentDirectory = tempDir.resolve("application")
        Files.createDirectories(currentDirectory)
        writeSettingsGradle(tempDir)
        def settingsFile = tempDir.resolve("test-resources/.micronaut/test-resources/test-resources-settings/test-resources.properties")
        writeSettingsFile(settingsFile, "http://standalone")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null) ==
            Optional.of(settingsFile)
    }

    def "repository root resolves standalone settings file"() {
        given:
        def currentDirectory = tempDir
        writeSettingsGradle(currentDirectory)
        def settingsFile = tempDir.resolve("test-resources/.micronaut/test-resources/test-resources-settings/test-resources.properties")
        writeSettingsFile(settingsFile, "http://standalone")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null) ==
            Optional.of(settingsFile)
    }

    def "ambiguous standalone settings do not resolve nondeterministically"() {
        given:
        def currentDirectory = tempDir
        writeSettingsGradle(currentDirectory)
        writeSettingsFile(tempDir.resolve("test-resources-a/.micronaut/test-resources/test-resources-settings/test-resources.properties"), "http://one")
        writeSettingsFile(tempDir.resolve("test-resources-b/.micronaut/test-resources/test-resources-settings/test-resources.properties"), "http://two")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null).empty
    }

    def "namespaced home fallback still resolves"() {
        given:
        def homeDirectory = tempDir.resolve("home")
        def settingsFile = homeDirectory.resolve(".micronaut/test-resources-example/test-resources.properties")
        writeSettingsFile(settingsFile, "http://home")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(tempDir.resolve("application"), homeDirectory, "example") ==
            Optional.of(settingsFile)
    }

    def "non project ancestors do not contribute sibling standalone settings"() {
        given:
        def currentDirectory = tempDir.resolve("application")
        Files.createDirectories(currentDirectory)
        writeSettingsFile(tempDir.resolve("test-resources/.micronaut/test-resources/test-resources-settings/test-resources.properties"), "http://standalone")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null).empty
    }

    def "non project ancestors do not contribute direct standalone settings"() {
        given:
        def currentDirectory = tempDir.resolve("outer/project/application")
        Files.createDirectories(currentDirectory)
        writeSettingsGradle(tempDir.resolve("outer/project"))
        writeSettingsFile(tempDir.resolve("outer/.micronaut/test-resources/test-resources-settings/test-resources.properties"), "http://standalone")

        expect:
        TestResourcesClientFactory.findPropertiesFileByConvention(currentDirectory, tempDir.resolve("home"), null).empty
    }

    private static void writeSettingsFile(Path settingsFile, String serverUri) {
        Files.createDirectories(settingsFile.parent)
        Files.writeString(settingsFile, "server.uri=${serverUri}\n")
    }

    private static void writeSettingsGradle(Path projectDirectory) {
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'test-project'\n")
    }
}
