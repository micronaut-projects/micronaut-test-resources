package io.micronaut.testresources.server

import spock.lang.Specification
import spock.lang.TempDir
import spock.util.concurrent.PollingConditions

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class TestResourcesServiceConfigFilesSpec extends Specification {
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30)
    private static final String ACCESS_TOKEN = "dev8-token"
    private static final String MICRONAUT_CONFIG_FILES_ENV = "MICRONAUT_CONFIG_FILES"
    private static final String RUNTIME_CLASSPATH_PROPERTY = "test.resources.server.runtime.classpath"

    @TempDir
    Path tempDir

    def cleanup() {
        runningProcesses.each { Process process ->
            stopProcess(process)
        }
        runningProcesses.clear()
    }

    def "starts and writes the port file when MICRONAUT_CONFIG_FILES points to YAML"() {
        given:
        def yamlConfig = tempDir.resolve("local-secrets.yml")
        Files.writeString(yamlConfig, "foo:\n  bar: baz\n")
        def portFile = tempDir.resolve("port-file")

        when:
        def process = startServer(portFile, [(MICRONAUT_CONFIG_FILES_ENV): yamlConfig.toString()])
        def port = waitForPortFile(portFile)

        then:
        port > 0
        process.isAlive()
    }

    def "keeps honoring explicit system properties while ignoring inherited config files"() {
        given:
        def yamlConfig = tempDir.resolve("local-secrets.yml")
        Files.writeString(yamlConfig, "foo:\n  bar: baz\n")
        def portFile = tempDir.resolve("port-file")

        when:
        def process = startServer(
            portFile,
            [(MICRONAUT_CONFIG_FILES_ENV): yamlConfig.toString()],
            ["-Dserver.access-token=${ACCESS_TOKEN}".toString()]
        )
        def port = waitForPortFile(portFile)

        then:
        request(port).statusCode() == 401
        request(port, [(AccessConfiguration.ACCESS_TOKEN): ACCESS_TOKEN]).statusCode() == 200
        process.isAlive()
    }

    private final List<Process> runningProcesses = []

    private Process startServer(Path portFile, Map<String, String> environment, List<String> jvmArgs = []) {
        def command = [
            javaCommand(),
            *jvmArgs,
            "-cp",
            System.getProperty(RUNTIME_CLASSPATH_PROPERTY),
            TestResourcesService.name,
            "--port-file=${portFile.toAbsolutePath()}".toString()
        ]
        def builder = new ProcessBuilder(command)
        builder.redirectErrorStream(true)
        builder.environment().putAll(environment)
        def process = builder.start()
        runningProcesses.add(process)
        return process
    }

    private int waitForPortFile(Path portFile) {
        def conditions = new PollingConditions(timeout: STARTUP_TIMEOUT.seconds, initialDelay: 0.1, delay: 0.2)
        conditions.eventually {
            assert Files.exists(portFile)
            assert Files.size(portFile) > 0
        }
        return Integer.parseInt(Files.readString(portFile).trim())
    }

    private HttpResponse<String> request(int port, Map<String, String> headers = [:]) {
        def requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:${port}/list"))
            .timeout(Duration.ofSeconds(10))
            .GET()
        headers.each { name, value -> requestBuilder.header(name, value) }
        return HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private static String javaCommand() {
        Path.of(System.getProperty("java.home"), "bin", "java").toString()
    }

    private static void stopProcess(Process process) {
        if (!process.isAlive()) {
            return
        }
        process.destroy()
        if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly()
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}
