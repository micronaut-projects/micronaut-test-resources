/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.h2;

import io.micronaut.testresources.core.Scope;
import io.micronaut.testresources.core.ToggableTestResourcesResolver;
import org.h2.engine.SysProperties;
import org.h2.tools.Server;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A test resource provider which starts a containerless H2 TCP server.
 */
public final class H2TestResourceProvider implements ToggableTestResourcesResolver, Closeable {
    public static final String DISPLAY_NAME = "H2";
    public static final String PREFIX = "datasources";

    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DIALECT = "dialect";
    private static final String DRIVER = "driver-class-name";
    private static final String TYPE = "db-type";
    private static final String SERVER_URI = "micronaut.test.resources.server.uri";
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_USERNAME = "sa";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DRIVER = "org.h2.Driver";
    private static final String JDBC_OPTIONS = "DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=FALSE";
    private static final String BIND_ADDRESS_PROPERTY = "h2.bindAddress";
    private static final String LOOPBACK_HOST = InetAddress.getLoopbackAddress().getHostAddress();
    private static final int MAX_START_ATTEMPTS = 10;
    private static final List<String> SUPPORTED_PROPERTIES = List.of(URL, USERNAME, PASSWORD, DRIVER);
    private static final Object H2_SYSTEM_PROPERTIES_MONITOR = new Object();

    private final Object lifecycleMonitor = new Object();
    private final Map<Scope, H2Server> servers = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getName() {
        return "h2";
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Collections.singletonList(PREFIX);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (!isDatasourceExpression(expression)) {
            return Collections.emptyList();
        }
        String datasource = datasourceNameFrom(expression);
        return Stream.of(
            datasourceExpressionOf(datasource, TYPE),
            datasourceExpressionOf(datasource, DIALECT),
            SERVER_URI
        ).toList();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        Collection<String> datasources = propertyEntries.getOrDefault(PREFIX, Collections.emptyList());
        return datasources.stream()
            .flatMap(datasource -> SUPPORTED_PROPERTIES.stream().map(property -> datasourceExpressionOf(datasource, property)))
            .toList();
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if (!shouldAnswer(propertyName, properties)) {
            return Optional.empty();
        }
        String datasource = datasourceNameFrom(propertyName);
        return Optional.ofNullable(switch (datasourcePropertyFrom(propertyName)) {
            case URL -> server(properties).getJdbcUrl(datasource);
            case USERNAME -> DEFAULT_USERNAME;
            case PASSWORD -> DEFAULT_PASSWORD;
            case DRIVER -> DEFAULT_DRIVER;
            default -> null;
        });
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        List<H2Server> activeServers;
        synchronized (lifecycleMonitor) {
            activeServers = List.copyOf(servers.values());
            servers.clear();
        }
        IOException failure = null;
        for (H2Server server : activeServers) {
            try {
                server.close();
            } catch (IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    int serverCount() {
        return servers.size();
    }

    private static boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties) {
        if (!isDatasourceExpression(propertyName)) {
            return false;
        }
        String datasource = datasourceNameFrom(propertyName);
        if (isExternalServerConfigured(requestedProperties)) {
            return false;
        }
        String type = stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, TYPE)));
        if (type != null) {
            return "h2".equalsIgnoreCase(type);
        }
        String dialect = stringOrNull(requestedProperties.get(datasourceExpressionOf(datasource, DIALECT)));
        return dialect != null && "h2".equalsIgnoreCase(dialect);
    }

    private static boolean isExternalServerConfigured(Map<String, Object> requestedProperties) {
        String serverUri = stringOrNull(requestedProperties.get(SERVER_URI));
        return serverUri != null && !serverUri.isBlank();
    }

    private static String stringOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static boolean isDatasourceExpression(String expression) {
        return expression.startsWith(PREFIX + ".");
    }

    private static String datasourceNameFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(0, remainder.indexOf('.'));
    }

    private static String datasourcePropertyFrom(String expression) {
        String remainder = expression.substring(1 + expression.indexOf('.'));
        return remainder.substring(1 + remainder.indexOf('.'));
    }

    private static String datasourceExpressionOf(String datasource, String property) {
        return PREFIX + "." + datasource + "." + property;
    }

    private H2Server server(Map<String, Object> properties) {
        synchronized (lifecycleMonitor) {
            if (closed.get()) {
                throw new IllegalStateException("H2 test resource provider is closed");
            }
            return servers.computeIfAbsent(
                Scope.from(properties),
                ignored -> H2Server.start()
            );
        }
    }

    private static final class H2Server implements Closeable {
        private final Server server;
        private final Map<String, String> databaseNames = new ConcurrentHashMap<>();

        private H2Server(Server server) {
            this.server = server;
        }

        private static H2Server start() {
            SQLException failure = null;
            for (int attempt = 0; attempt < MAX_START_ATTEMPTS; attempt++) {
                try {
                    return new H2Server(startServer());
                } catch (SQLException e) {
                    if (!isBindFailure(e) || attempt == MAX_START_ATTEMPTS - 1) {
                        throw new IllegalStateException("Unable to start H2 TCP server", e);
                    }
                    failure = e;
                }
            }
            throw new IllegalStateException("Unable to start H2 TCP server", failure);
        }

        private String getJdbcUrl(String datasource) {
            String databaseName = databaseNames.computeIfAbsent(datasource, H2Server::newDatabaseName);
            return "jdbc:h2:tcp://%s:%d/mem:%s;%s".formatted(DEFAULT_HOST, server.getPort(), databaseName, JDBC_OPTIONS);
        }

        @Override
        public void close() throws IOException {
            try {
                server.stop();
            } catch (RuntimeException e) {
                throw new IOException("Unable to stop H2 TCP server", e);
            }
        }

        private static Server startServer() throws SQLException {
            int port = findAvailablePort();
            synchronized (H2_SYSTEM_PROPERTIES_MONITOR) {
                String previousBindAddress = System.getProperty(BIND_ADDRESS_PROPERTY);
                System.setProperty(BIND_ADDRESS_PROPERTY, LOOPBACK_HOST);
                try {
                    verifyLoopbackBinding();
                    return Server.createTcpServer(
                        "-tcp",
                        "-tcpPort", Integer.toString(port),
                        "-ifNotExists"
                    ).start();
                } finally {
                    restoreBindAddress(previousBindAddress);
                }
            }
        }

        private static boolean isBindFailure(SQLException e) {
            Throwable current = e;
            while (current != null) {
                if (current instanceof BindException) {
                    return true;
                }
                current = current.getCause();
            }
            String message = e.getMessage();
            return message != null && message.toLowerCase(Locale.ROOT).contains("already in use");
        }

        private static String newDatabaseName(String datasource) {
            return datasource + "_" + UUID.randomUUID().toString().replace("-", "");
        }

        private static void verifyLoopbackBinding() throws SQLException {
            String bindAddress = SysProperties.BIND_ADDRESS;
            if (bindAddress == null || bindAddress.isBlank()) {
                throw new SQLException("Unable to enforce a loopback-only H2 bind address");
            }
            try {
                if (!InetAddress.getByName(bindAddress).isLoopbackAddress()) {
                    throw new SQLException("Unable to enforce a loopback-only H2 bind address");
                }
            } catch (IOException e) {
                throw new SQLException("Unable to enforce a loopback-only H2 bind address", e);
            }
        }

        private static void restoreBindAddress(String previousBindAddress) {
            if (previousBindAddress == null) {
                System.clearProperty(BIND_ADDRESS_PROPERTY);
            } else {
                System.setProperty(BIND_ADDRESS_PROPERTY, previousBindAddress);
            }
        }

        private static int findAvailablePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to allocate a TCP port for H2", e);
            }
        }
    }
}
