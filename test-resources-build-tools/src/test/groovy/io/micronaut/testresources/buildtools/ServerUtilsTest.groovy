package io.micronaut.testresources.buildtools

import com.sun.net.httpserver.HttpServer
import io.micronaut.context.ApplicationContext
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.environment.RestoreSystemProperties

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.Properties

class ServerUtilsTest extends Specification {
    private static final String JMX_PROPERTY = 'com.sun.management.jmxremote'

    @TempDir
    Path tmpDir

    def "writes and reads server settings"() {
        def settings = new ServerSettings(
                1234,
                token,
                timeout,
                null
        )
        def settingsDir = tmpDir.resolve("settings")

        when:
        ServerUtils.writeServerSettings(settingsDir, settings)
        def read = ServerUtils.readServerSettings(settingsDir)

        then:
        read.present
        def actual = read.get()
        actual == settings

        where:
        token | timeout
        null  | null
        'abc' | null
        null  | 60
        'abc' | 98
    }

    def "writes the current project path uri into server settings when available"() {
        given:
        def projectDir = tmpDir.resolve("sample-project")
        Files.createDirectories(projectDir.resolve("build/test-resources-server-config"))
        Files.createFile(projectDir.resolve("settings.gradle"))
        def settingsDir = projectDir.resolve("build/test-resources-server-config")
        def settings = new ServerSettings(1234, null, null, null)

        when:
        ServerUtils.writeServerSettings(settingsDir, settings)
        def props = new Properties()
        Files.newInputStream(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME)).withCloseable(props.&load)

        then:
        props.getProperty("micronaut.test.resources.project-path-uri") == projectDir.toUri().toString()
    }

    def "writes disabled server settings"() {
        given:
        def projectDir = tmpDir.resolve("sample-project")
        Files.createDirectories(projectDir.resolve("build/test-resources-server-config"))
        Files.createFile(projectDir.resolve("settings.gradle"))
        def settingsDir = projectDir.resolve("build/test-resources-server-config")

        when:
        ServerUtils.writeDisabledServerSettings(settingsDir)
        def props = new Properties()
        Files.newInputStream(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME)).withCloseable(props.&load)

        then:
        props.getProperty("enabled") == "false"
        props.getProperty("micronaut.test.resources.project-path-uri") == projectDir.toUri().toString()
        ServerUtils.readServerSettings(settingsDir).empty
    }

    def "requires new server"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, token, classpath, timeout, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.TestResourcesService'
            assert params.classpath == classpath
            def sysProps = [(JMX_PROPERTY): null]
            if (token != null) {
                sysProps["server.access-token"] = token
            }
            assert params.systemProperties == sysProps
            assert params.arguments == [
                    "--port-file=${portFile.toAbsolutePath()}".toString()
            ]
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        and:
        Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))
        settings.port == embeddedServer.port
        settings.accessToken == Optional.ofNullable(token)
        settings.clientTimeout == Optional.ofNullable(timeout)

        when:
        ServerUtils.stopServer(settingsDir)

        then:
        !Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))

        cleanup:
        applicationContext.stop()

        where:
        token | classpath         | timeout
        null  | []                | null
        'abc' | []                | null
        null  | [new File('abc')] | null
        'abc' | [new File('def')] | 98
    }

    @RestoreSystemProperties
    def "can set the docker check timeout"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        System.setProperty('docker.check.timeout.seconds', "100")

        when:
        ServerUtils.startOrConnectToExistingServer(9999, portFile, settingsDir, null, null, null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.TestResourcesService'
            assert params.systemProperties['docker.check.timeout.seconds'] == '100'
        }
    }

    @RestoreSystemProperties
    def "waits for the server to be available when using an explicit port"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def iterations = new AtomicInteger(0);
        System.setProperty(ServerUtils.SERVER_TEST_PROPERTY, "false")

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(9999, portFile, settingsDir, null, null, null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.TestResourcesService'
        }
        _ * factory.waitFor(_) >> {
            if (iterations.incrementAndGet() == 4) {
                // start the service
                System.setProperty(ServerUtils.SERVER_TEST_PROPERTY, "true")
            } else {
                Thread.sleep(10)
            }
        }

        and:
        Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))
        iterations.get() == 4


    }

    def "reuses existing server"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()
        ServerUtils.writeServerSettings(settingsDir, new ServerSettings(embeddedServer.port, null, null, null))

        when: "no explicit port"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        when: "explicit port"
        ServerUtils.startOrConnectToExistingServer(embeddedServer.port, portFile, settingsDir, null, [], null, null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        cleanup:
        applicationContext.stop()
    }

    def "starts a fresh server when persisted settings point to another service"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def staleServer = startRequirementsServer(null, 'application/json', '{"service":"other"}')
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()
        ServerUtils.writeServerSettings(settingsDir, new ServerSettings(staleServer.address.port, null, null, null))

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        1 * factory.startServer(_)
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }
        settings.port == embeddedServer.port

        cleanup:
        staleServer?.stop(0)
        applicationContext.stop()
    }

    def "reuses existing server using the saved access token"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def server = startRequirementsServer('secret-token')
        ServerUtils.writeServerSettings(settingsDir, new ServerSettings(server.address.port, 'secret-token', null, null))

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)
        settings == new ServerSettings(server.address.port, 'secret-token', null, null)

        cleanup:
        server?.stop(0)
    }

    def "fails clearly when an explicit port belongs to another service"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def server = startRequirementsServer(null, 'text/plain', 'not test resources')

        when:
        ServerUtils.startOrConnectToExistingServer(server.address.port, portFile, settingsDir, null, [], null, null, factory)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Explicit test resources port ${server.address.port} is already in use by a service that could not be validated as Micronaut Test Resources: it did not return JSON"
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        cleanup:
        server?.stop(0)
    }

    def "fails clearly when an explicit port requires an access token"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def server = startRequirementsServer('secret-token')

        when:
        ServerUtils.startOrConnectToExistingServer(server.address.port, portFile, settingsDir, null, [], null, null, factory)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Explicit test resources port ${server.address.port} is already in use by a service that could not be validated as Micronaut Test Resources: an access token is required"
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        cleanup:
        server?.stop(0)
    }

    def "supports class data sharing"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def cdsDir = tmpDir.resolve("cds-dir")
        def cdsClasspathDir = tmpDir.resolve("some-classes")
        Files.createDirectory(cdsClasspathDir)
        Files.write(cdsClasspathDir.resolve("some.class"), [1, 2, 3] as byte[])
        def cdsClassList = cdsDir.resolve("cds.classlist")
        def cdsArchiveFile = cdsDir.resolve("cds.jsa")
        def cdsFlatJar = cdsDir.resolve("flat.jar")
        def cdsLoggingOffOption = "-Xlog:cds*=off"
        String cdsClassListOption = "-XX:DumpLoadedClassList=${cdsClassList.toAbsolutePath()}"
        String cdsSharedClassListOption = "-XX:SharedClassListFile=${cdsClassList.toAbsolutePath()}"
        String cdsSharedArchiveFileOption = "-XX:SharedArchiveFile=${cdsArchiveFile.toAbsolutePath()}"
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when: "first call with CDS support enabled"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [cdsClasspathDir.toFile()], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains(cdsLoggingOffOption)
            assert jvmArgs.contains("-Xshare:off")
            assert jvmArgs.contains(cdsClassListOption)
            assert params.systemProperties.containsKey(JMX_PROPERTY)
            assert params.systemProperties.get(JMX_PROPERTY) == null
            assert params.classpath.contains(cdsFlatJar.toFile())
            assert Files.exists(cdsFlatJar)
            Files.write(cdsClassList, "test".getBytes())
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }
        settingsDir.toFile().deleteDir()

        when: "second call dumps CDS then starts server"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [cdsClasspathDir.toFile()], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains(cdsLoggingOffOption)
            assert jvmArgs.contains("-Xshare:dump")
            assert jvmArgs.contains(cdsSharedClassListOption)
            assert jvmArgs.contains(cdsSharedArchiveFileOption)
            assert !params.systemProperties.containsKey(JMX_PROPERTY)
            Files.write(cdsArchiveFile, "test".getBytes())
        }
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains(cdsLoggingOffOption)
            assert !jvmArgs.contains("-Xshare:dump")
            assert !jvmArgs.contains(cdsSharedClassListOption)
            assert jvmArgs.contains(cdsSharedArchiveFileOption)
            assert params.systemProperties.containsKey(JMX_PROPERTY)
            assert params.systemProperties.get(JMX_PROPERTY) == null
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        settingsDir.toFile().deleteDir()

        when: "third call starts server with CDS"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [cdsClasspathDir.toFile()], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains(cdsLoggingOffOption)
            assert !jvmArgs.contains("-Xshare:dump")
            assert !jvmArgs.contains(cdsSharedClassListOption)
            assert jvmArgs.contains(cdsSharedArchiveFileOption)
            assert params.systemProperties.containsKey(JMX_PROPERTY)
            assert params.systemProperties.get(JMX_PROPERTY) == null
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        settingsDir.toFile().deleteDir()

        when: "removes CDS files if classpath changes"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, cdsDir, [], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            def jvmArgs = params.jvmArguments
            assert jvmArgs.contains(cdsLoggingOffOption)
            assert jvmArgs.contains("-Xshare:off")
            assert jvmArgs.contains(cdsClassListOption)
            assert params.systemProperties.containsKey(JMX_PROPERTY)
            assert params.systemProperties.get(JMX_PROPERTY) == null
            assert params.classpath == []
            Files.write(cdsClassList, "test".getBytes())
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        cleanup:
        applicationContext.stop()
    }

    def "waits for port file to have contents"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->

        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = ""
            println "Waiting to write the contents of the port file"
        }
        1 * factory.waitFor(_) >> {
            portFile.toFile().text = "${embeddedServer.port}"
        }

        cleanup:
        applicationContext.stop()
    }

    def "reasonable error message if port file can never be read"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, null, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->

        }
        10 * factory.waitFor(_) >> {
            portFile.toFile().text = ""
            println "Waiting to write the contents of the port file"
        }
        IllegalStateException ex = thrown()
        ex.message == "Unable to read port file ${portFile}: file is empty"

        cleanup:
        applicationContext.stop()
    }

    def "can configure a namespace for the default shared settings"() {
        def withoutNamespace = ServerUtils.getDefaultSharedSettingsPath()
        def withNamespace = ServerUtils.getDefaultSharedSettingsPath("custom")

        expect:
        withoutNamespace == ServerUtils.getDefaultSharedSettingsPath(null)
        withNamespace.parent == withoutNamespace.parent
        withNamespace.parent.resolve("test-resources-custom") == withNamespace
    }

    def "stops the real server controller with the existing stopServer caller"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = new ForkingServerFactory(tmpDir.resolve("server.log"))

        when:
        ServerUtils.startOrConnectToExistingServer(
            null,
            portFile,
            settingsDir,
            null,
            currentJvmClasspath(),
            null,
            1,
            factory
        )
        ServerUtils.stopServer(settingsDir)

        then:
        !Files.exists(settingsDir.resolve(ServerUtils.PROPERTIES_FILE_NAME))
        factory.waitForExit(Duration.ofSeconds(15))
        factory.exitValue() == 0

        cleanup:
        factory.destroy()
    }

    private static List<File> currentJvmClasspath() {
        System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .collect { new File(it) }
    }

    @Controller
    static class ServerMock {

        @Inject
        ApplicationContext ctx

        @Get("/requirements/entries")
        List<String> entries() {
            ['stub.entry']
        }

        @Post("/stop")
        void close() {
            ctx.close()
        }
    }

    private static HttpServer startRequirementsServer(String expectedAccessToken = null,
                                                      String contentType = 'application/json',
                                                      String body = '["stub.entry"]') {
        def server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        server.createContext('/requirements/entries') { exchange ->
            if (expectedAccessToken != null && exchange.requestHeaders.getFirst('Access-Token') != expectedAccessToken) {
                exchange.sendResponseHeaders(401, -1)
                exchange.close()
                return
            }
            byte[] response = body.getBytes(StandardCharsets.UTF_8)
            exchange.responseHeaders.set('Content-Type', contentType)
            exchange.sendResponseHeaders(200, response.length)
            exchange.responseBody.write(response)
            exchange.close()
        }
        server.start()
        server
    }

    private static final class ForkingServerFactory implements ServerFactory {
        private final Path logFile
        private Process process

        private ForkingServerFactory(Path logFile) {
            this.logFile = logFile
        }

        @Override
        void startServer(ServerUtils.ProcessParameters processParameters) throws IOException {
            def command = new ArrayList<String>()
            command.add(new File(System.getProperty("java.home"), "bin/java").absolutePath)
            command.addAll(processParameters.jvmArguments.collect { it.toString() })
            processParameters.systemProperties.each { key, value ->
                command.add(value == null ? "-D${key}".toString() : "-D${key}=${value}".toString())
            }
            command.add("-cp")
            command.add(processParameters.classpath.collect { it.absolutePath }.join(File.pathSeparator))
            command.add(processParameters.mainClass.toString())
            command.addAll(processParameters.arguments.collect { it.toString() })
            process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start()
        }

        @Override
        void waitFor(Duration duration) throws InterruptedException {
            Thread.sleep(duration.toMillis())
        }

        boolean waitForExit(Duration duration) throws InterruptedException {
            process.waitFor(duration.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
        }

        int exitValue() {
            process.exitValue()
        }

        void destroy() throws InterruptedException {
            if (process?.isAlive()) {
                process.destroyForcibly()
                process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }
}
