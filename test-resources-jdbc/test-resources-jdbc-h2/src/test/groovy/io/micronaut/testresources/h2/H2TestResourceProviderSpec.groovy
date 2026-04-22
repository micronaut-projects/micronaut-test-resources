package io.micronaut.testresources.h2

import io.micronaut.testresources.core.Scope
import spock.lang.Specification

import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException

class H2TestResourceProviderSpec extends Specification {
    private final H2TestResourceProvider provider = new H2TestResourceProvider()

    void cleanup() {
        provider.close()
    }

    void "resolves JDBC properties when db type is h2"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'test',
            'datasources.default.db-type'  : 'h2'
        ]

        when:
        String url = provider.resolve('datasources.default.url', properties, [:]).orElse(null)
        String username = provider.resolve('datasources.default.username', properties, [:]).orElse(null)
        String password = provider.resolve('datasources.default.password', properties, [:]).orElse(null)
        String driver = provider.resolve('datasources.default.driver-class-name', properties, [:]).orElse(null)

        then:
        url.startsWith('jdbc:h2:tcp://localhost:')
        url.contains('/mem:default_')
        username == 'sa'
        password == ''
        driver == 'org.h2.Driver'
        provider.serverCount() == 1
    }

    void "reuses the same server inside a scope"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'shared',
            'datasources.default.db-type'  : 'h2'
        ]

        when:
        String first = provider.resolve('datasources.default.url', properties, [:]).orElseThrow()
        String second = provider.resolve('datasources.default.url', properties, [:]).orElseThrow()
        String username = provider.resolve('datasources.default.username', properties, [:]).orElseThrow()

        then:
        first == second
        username == 'sa'
        provider.serverCount() == 1
    }

    void "creates a different server for another scope"() {
        given:
        Map<String, Object> left = [
            (Scope.PROPERTY_KEY)           : 'left',
            'datasources.default.db-type'  : 'h2'
        ]
        Map<String, Object> right = [
            (Scope.PROPERTY_KEY)           : 'right',
            'datasources.default.db-type'  : 'h2'
        ]

        when:
        String leftUrl = provider.resolve('datasources.default.url', left, [:]).orElseThrow()
        String rightUrl = provider.resolve('datasources.default.url', right, [:]).orElseThrow()

        then:
        leftUrl != rightUrl
        provider.serverCount() == 2
    }

    void "reuses the same server for multiple datasource names in the same scope"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)         : 'shared',
            'datasources.alpha.db-type'  : 'h2',
            'datasources.beta.db-type'   : 'h2'
        ]

        when:
        String alphaUrl = provider.resolve('datasources.alpha.url', properties, [:]).orElseThrow()
        String betaUrl = provider.resolve('datasources.beta.url', properties, [:]).orElseThrow()

        then:
        alphaUrl.startsWith('jdbc:h2:tcp://localhost:')
        betaUrl.startsWith('jdbc:h2:tcp://localhost:')
        alphaUrl.contains('/mem:alpha_')
        betaUrl.contains('/mem:beta_')
        alphaUrl != betaUrl
        alphaUrl.substring(0, alphaUrl.indexOf('/mem:')) == betaUrl.substring(0, betaUrl.indexOf('/mem:'))
        provider.serverCount() == 1
    }

    void "binds the H2 TCP listener to loopback only"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'loopback',
            'datasources.default.db-type'  : 'h2'
        ]

        when:
        String url = provider.resolve('datasources.default.url', properties, [:]).orElseThrow()
        InetAddress nonLoopbackAddress = findNonLoopbackAddress()
        if (nonLoopbackAddress == null) {
            return
        }
        int port = tcpPortOf(url)
        Socket socket = new Socket()
        try {
            socket.connect(new InetSocketAddress(nonLoopbackAddress, port), 500)
        } finally {
            socket.close()
        }

        then:
        IOException e = thrown()
        e instanceof ConnectException || e instanceof NoRouteToHostException || e instanceof SocketTimeoutException
        provider.serverCount() == 1
    }

    void "matches the H2 dialect fallback"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)            : 'dialect',
            'datasources.default.dialect'   : 'H2'
        ]

        expect:
        provider.resolve('datasources.default.url', properties, [:]).present
    }

    void "does not answer other database types"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'other',
            'datasources.default.db-type'  : 'postgres'
        ]

        expect:
        provider.resolve('datasources.default.url', properties, [:]).empty
        provider.serverCount() == 0
    }

    void "requires H2 datasource hints and the external server flag"() {
        expect:
        provider.getRequiredProperties('datasources.default.url') == [
            'datasources.default.db-type',
            'datasources.default.dialect',
            'micronaut.test.resources.server.uri'
        ]
    }

    void "does not answer when using an external test resources server"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'remote-host',
            'datasources.default.db-type'  : 'h2',
            'micronaut.test.resources.server.uri': 'http://localhost:8080'
        ]

        expect:
        provider.resolve('datasources.default.url', properties, [:]).empty
        provider.resolve('datasources.default.username', properties, [:]).empty
        provider.resolve('datasources.default.password', properties, [:]).empty
        provider.resolve('datasources.default.driver-class-name', properties, [:]).empty
        provider.serverCount() == 0
    }

    void "closes active servers"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'closing',
            'datasources.default.db-type'  : 'h2'
        ]

        when:
        provider.resolve('datasources.default.url', properties, [:]).orElseThrow()
        provider.close()

        then:
        provider.serverCount() == 0
    }

    void "does not create new servers after close"() {
        given:
        Map<String, Object> properties = [
            (Scope.PROPERTY_KEY)           : 'closed',
            'datasources.default.db-type'  : 'h2'
        ]

        when:
        provider.close()
        provider.resolve('datasources.default.url', properties, [:])

        then:
        IllegalStateException e = thrown()
        e.message == 'H2 test resource provider is closed'
        provider.serverCount() == 0
    }

    private static int tcpPortOf(String url) {
        def matcher = url =~ /jdbc:h2:tcp:\/\/localhost:(\d+)\/mem:.+/
        assert matcher.matches()
        matcher.group(1) as int
    }

    private static InetAddress findNonLoopbackAddress() {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement()
            if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                continue
            }
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses()
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement()
                if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress() && !address.isAnyLocalAddress()) {
                    return address
                }
            }
        }
        null
    }
}
