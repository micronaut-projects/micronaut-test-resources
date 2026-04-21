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
import org.h2.tools.Server;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private static final List<String> SUPPORTED_PROPERTIES = List.of(URL, USERNAME, PASSWORD, DRIVER);

    private final Map<Key, H2Server> servers = new ConcurrentHashMap<>();

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
            case URL -> server(properties, datasource).getJdbcUrl();
            case USERNAME -> DEFAULT_USERNAME;
            case PASSWORD -> DEFAULT_PASSWORD;
            case DRIVER -> DEFAULT_DRIVER;
            default -> null;
        });
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        for (H2Server server : servers.values()) {
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
        servers.clear();
        if (failure != null) {
            throw failure;
        }
    }

    int serverCount() {
        return servers.size();
    }

    private H2Server server(Map<String, Object> properties, String datasource) {
        return servers.computeIfAbsent(
            new Key(Scope.from(properties), datasource),
            key -> H2Server.start(datasource)
        );
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

    private record Key(Scope scope, String datasource) {
    }

    private static final class H2Server implements Closeable {
        private final String databaseName;
        private final Server server;

        private H2Server(String databaseName, Server server) {
            this.databaseName = databaseName;
            this.server = server;
        }

        private static H2Server start(String datasource) {
            String databaseName = datasource + "_" + UUID.randomUUID().toString().replace("-", "");
            int port = findAvailablePort();
            try {
                Server server = Server.createTcpServer(
                    "-tcp",
                    "-tcpPort", Integer.toString(port),
                    "-ifNotExists"
                ).start();
                return new H2Server(databaseName, server);
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to start H2 TCP server", e);
            }
        }

        private String getJdbcUrl() {
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
