package io.micronaut.testresources.buildtools

import io.micronaut.context.ApplicationContext
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.runtime.server.EmbeddedServer
import jakarta.inject.Inject
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class ServerUtilsTest extends Specification {
    @TempDir
    Path tmpDir

    def "writes and reads server settings"() {
        def settings = new ServerSettings(
                1234,
                token,
                timeout
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

    def "requires new server"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()

        when:
        def settings = ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, token, classpath, timeout, factory)

        then:
        1 * factory.startServer(_) >> { ServerUtils.ProcessParameters params ->
            assert params.mainClass == 'io.micronaut.testresources.server.Application'
            assert params.classpath == classpath
            def sysProps = [:]
            if (token != null) {
                sysProps["server.access.token"] = token
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

    def "reuses existing server"() {
        def portFile = tmpDir.resolve("port-file")
        def settingsDir = tmpDir.resolve("settings")
        def factory = Mock(ServerFactory)
        def applicationContext = ApplicationContext.builder().start()
        def embeddedServer = applicationContext.getBean(EmbeddedServer)
        embeddedServer.start()
        ServerUtils.writeServerSettings(settingsDir, new ServerSettings(embeddedServer.port, null, null))

        when: "no explicit port"
        ServerUtils.startOrConnectToExistingServer(null, portFile, settingsDir, null, [], null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)

        when: "explicit port"
        ServerUtils.startOrConnectToExistingServer(embeddedServer.port, portFile, settingsDir, null, [], null, factory)

        then:
        0 * factory.startServer(_)
        0 * factory.waitFor(_)
    }

    @Controller
    static class ServerMock {

        @Inject
        ApplicationContext ctx

        @Post("/stop")
        void close() {
            ctx.close()
        }
    }
}
