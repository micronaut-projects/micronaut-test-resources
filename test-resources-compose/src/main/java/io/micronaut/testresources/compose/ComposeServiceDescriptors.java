/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.testresources.compose;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

final class ComposeServiceDescriptors {
    static final String DATASOURCES = "datasources";
    static final String URL = "url";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String DRIVER = "driver-class-name";
    static final String DB_NAME = "db-name";
    static final String TYPE = "db-type";
    static final String DIALECT = "dialect";
    static final String RESOURCE_NAME = "test-resources.resource-name";
    static final String R2DBC_DATASOURCES = "r2dbc.datasources";
    static final String R2DBC_DRIVER = "driverClassName";
    static final String JPA = "jpa";
    static final String HIBERNATE_CONNECTION = "properties.hibernate.connection.";
    static final String MONGODB_SERVERS = "mongodb.servers";

    private static final String POSTGRES_SERVICE = "postgres";
    private static final String MYSQL_SERVICE = "mysql";
    private static final String MARIADB_SERVICE = "mariadb";
    private static final String MSSQL_SERVICE = "mssql";
    private static final String ORACLE_SERVICE = "oracle";
    private static final String MONGODB_SERVICE = "mongodb";
    private static final String MONGODB_SERVERS_PREFIX = MONGODB_SERVERS + ".";

    private static final DatabaseDescriptor POSTGRES = new DatabaseDescriptor(
        POSTGRES_SERVICE,
        Set.of(POSTGRES_SERVICE, "postgresql", "pg"),
        5432,
        "jdbc:postgresql",
        "postgresql",
        "org.postgresql.Driver",
        "POSTGRES_USER",
        "POSTGRES_PASSWORD",
        "POSTGRES_DB",
        "test",
        "test",
        "test"
    );
    private static final DatabaseDescriptor MYSQL = new DatabaseDescriptor(
        MYSQL_SERVICE,
        Set.of(MYSQL_SERVICE),
        3306,
        "jdbc:mysql",
        MYSQL_SERVICE,
        "com.mysql.cj.jdbc.Driver",
        "MYSQL_USER",
        "MYSQL_PASSWORD",
        "MYSQL_DATABASE",
        "test",
        "test",
        "test"
    );
    private static final DatabaseDescriptor MARIADB = new DatabaseDescriptor(
        MARIADB_SERVICE,
        Set.of(MARIADB_SERVICE, "maria"),
        3306,
        "jdbc:mariadb",
        MARIADB_SERVICE,
        "org.mariadb.jdbc.Driver",
        "MARIADB_USER",
        "MARIADB_PASSWORD",
        "MARIADB_DATABASE",
        "test",
        "test",
        "test"
    );
    private static final DatabaseDescriptor MSSQL = new DatabaseDescriptor(
        MSSQL_SERVICE,
        Set.of(MSSQL_SERVICE, "sqlserver", "sql-server", "microsoftsqlserver"),
        1433,
        "jdbc:sqlserver",
        MSSQL_SERVICE,
        "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        "MSSQL_USER",
        "MSSQL_PASSWORD",
        "MSSQL_DATABASE",
        "SA",
        "A_Str0ng_Required_Password",
        "test"
    );
    private static final DatabaseDescriptor ORACLE = new DatabaseDescriptor(
        ORACLE_SERVICE,
        Set.of(ORACLE_SERVICE, "oracle-free", "oracle-xe"),
        1521,
        "jdbc:oracle:thin",
        ORACLE_SERVICE,
        "oracle.jdbc.OracleDriver",
        "ORACLE_USER",
        "ORACLE_PASSWORD",
        "ORACLE_DATABASE",
        "test",
        "test",
        "freepdb1"
    );

    static final List<DatabaseDescriptor> DATABASES = List.of(POSTGRES, MYSQL, MARIADB, MSSQL, ORACLE);

    static final List<ServiceDescriptor> SERVICES = List.of(
        service("redis", Set.of("redis"), 6379, List.of("redis.uri"), context -> context.service().labels().containsKey(ComposeLabels.PASSWORD) || context.service().environment().containsKey("REDIS_PASSWORD") ? null : "redis://" + context.hostPort()),
        service("rabbitmq", Set.of("rabbitmq", "rabbit"), 5672, List.of("rabbitmq.uri", "rabbitmq.username", "rabbitmq.password"), context -> switch (context.propertyName()) {
            case "rabbitmq.uri" -> "amqp://" + context.hostPort();
            case "rabbitmq.username" -> context.labelOrEnvironment(ComposeLabels.USERNAME, "RABBITMQ_DEFAULT_USER", "guest");
            case "rabbitmq.password" -> context.labelOrEnvironment(ComposeLabels.PASSWORD, "RABBITMQ_DEFAULT_PASS", "guest");
            default -> null;
        }),
        service("kafka", Set.of("kafka", "redpanda"), 9092, List.of("kafka.bootstrap.servers"), ResolutionContext::hostPort),
        service(MONGODB_SERVICE, Set.of(MONGODB_SERVICE, "mongo"), 27017, List.of("mongodb.uri"), context -> "mongodb://" + context.hostPort() + "/" + context.database(mongoDatabase(context.propertyName()))),
        service("localstack", Set.of("localstack"), 4566, List.of(
            "aws.access-key-id",
            "aws.secret-key",
            "aws.region",
            "aws.services.s3.endpoint-override",
            "aws.services.dynamodb.endpoint-override",
            "aws.services.sqs.endpoint-override",
            "aws.services.sns.endpoint-override"
        ), context -> switch (context.propertyName()) {
            case "aws.access-key-id" -> context.labelOrEnvironment(ComposeLabels.ACCESS_KEY, "AWS_ACCESS_KEY_ID", "test");
            case "aws.secret-key" -> context.labelOrEnvironment(ComposeLabels.SECRET_KEY, "AWS_SECRET_ACCESS_KEY", "test");
            case "aws.region" -> context.labelOrEnvironment("io.micronaut.test-resources.region", "AWS_DEFAULT_REGION", "us-east-1");
            default -> context.http();
        }),
        service("azurite", Set.of("azurite", "azure-storage"), 10000, List.of(
            "azure.credential.storage-shared-key.account-name",
            "azure.credential.storage-shared-key.account-key",
            "azure.credential.storage-shared-key.connection-string"
        ), context -> switch (context.propertyName()) {
            case "azure.credential.storage-shared-key.account-name" -> "devstoreaccount1";
            case "azure.credential.storage-shared-key.account-key" -> "Eby8vdM02xNOcqFeqCnf2A==";
            case "azure.credential.storage-shared-key.connection-string" -> "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFeqCnf2A==;BlobEndpoint=" + context.http() + "/devstoreaccount1;";
            default -> null;
        }),
        service("couchbase", Set.of("couchbase"), 11210, List.of("couchbase.uri", "couchbase.username", "couchbase.password"), context -> switch (context.propertyName()) {
            case "couchbase.uri" -> "couchbase://" + context.hostPort();
            case "couchbase.username" -> context.labelOrEnvironment(ComposeLabels.USERNAME, "COUCHBASE_ADMINISTRATOR_USERNAME", "Administrator");
            case "couchbase.password" -> context.labelOrEnvironment(ComposeLabels.PASSWORD, "COUCHBASE_ADMINISTRATOR_PASSWORD", PASSWORD);
            default -> null;
        }),
        service("hashicorp-consul", Set.of("consul", "hashicorp-consul"), 8500, List.of("consul.client.host", "consul.client.port"), context -> switch (context.propertyName()) {
            case "consul.client.host" -> context.endpoint().host();
            case "consul.client.port" -> String.valueOf(context.endpoint().port());
            default -> null;
        }),
        service("hashicorp-vault", Set.of("vault", "hashicorp-vault"), 8200, List.of("vault.client.uri", "vault.client.token"), context -> switch (context.propertyName()) {
            case "vault.client.uri" -> context.http();
            case "vault.client.token" -> context.labelOrEnvironment(ComposeLabels.TOKEN, "VAULT_DEV_ROOT_TOKEN_ID", "vault-token");
            default -> null;
        }),
        service("hazelcast", Set.of("hazelcast"), 5701, List.of("hazelcast.client.network.addresses"), ResolutionContext::hostPort),
        service("hivemq", Set.of("hivemq", "mqtt"), 1883, List.of("mqtt.client.client-id", "mqtt.client.server-uri"), context -> switch (context.propertyName()) {
            case "mqtt.client.client-id" -> context.labelOrEnvironment(ComposeLabels.CLIENT_ID, "MQTT_CLIENT_ID", "test-resources");
            case "mqtt.client.server-uri" -> "tcp://" + context.hostPort();
            default -> null;
        }),
        service("infinispan", Set.of("infinispan"), 11222, List.of(
            "infinispan.client.hotrod.server.host",
            "infinispan.client.hotrod.server.port",
            "infinispan.client.hotrod.security.authentication.username",
            "infinispan.client.hotrod.security.authentication.password"
        ), context -> switch (context.propertyName()) {
            case "infinispan.client.hotrod.server.host" -> context.endpoint().host();
            case "infinispan.client.hotrod.server.port" -> String.valueOf(context.endpoint().port());
            case "infinispan.client.hotrod.security.authentication.username" -> context.labelOrEnvironment(ComposeLabels.USERNAME, "USER", "admin");
            case "infinispan.client.hotrod.security.authentication.password" -> context.labelOrEnvironment(ComposeLabels.PASSWORD, "PASS", PASSWORD);
            default -> null;
        }),
        service("mailpit", Set.of("mailpit"), 1025, List.of(
            "javamail.properties.mail.smtp.host",
            "javamail.properties.mail.smtp.port",
            "javamail.properties.mail.smtp.auth",
            "javamail.properties.mail.smtp.starttls.enable"
        ), context -> switch (context.propertyName()) {
            case "javamail.properties.mail.smtp.host" -> context.endpoint().host();
            case "javamail.properties.mail.smtp.port" -> String.valueOf(context.endpoint().port());
            case "javamail.properties.mail.smtp.auth", "javamail.properties.mail.smtp.starttls.enable" -> "false";
            default -> null;
        }),
        service("minio", Set.of("minio"), 9000, List.of("minio.url", "minio.access-key", "minio.secret-key"), context -> switch (context.propertyName()) {
            case "minio.url" -> context.http();
            case "minio.access-key" -> context.labelOrEnvironment(ComposeLabels.ACCESS_KEY, "MINIO_ROOT_USER", "minioadmin");
            case "minio.secret-key" -> context.labelOrEnvironment(ComposeLabels.SECRET_KEY, "MINIO_ROOT_PASSWORD", "minioadmin");
            default -> null;
        }),
        service("neo4j", Set.of("neo4j"), 7687, List.of("neo4j.uri"), context -> "bolt://" + context.hostPort()),
        service("keycloak", Set.of("keycloak"), 8080, List.of(
            "micronaut.security.oauth2.clients.keycloak.client-id",
            "micronaut.security.oauth2.clients.keycloak.client-secret",
            "micronaut.security.oauth2.clients.keycloak.openid.issuer",
            "micronaut.security.token.jwt.signatures.jwks.keycloak.url"
        ), context -> {
            String realm = context.labelOrEnvironment(ComposeLabels.REALM, "KEYCLOAK_REALM", "micronaut");
            String issuer = context.http() + "/realms/" + realm;
            return switch (context.propertyName()) {
                case "micronaut.security.oauth2.clients.keycloak.client-id" -> context.labelOrEnvironment(ComposeLabels.CLIENT_ID, "KEYCLOAK_CLIENT_ID", "micronaut-test-resources");
                case "micronaut.security.oauth2.clients.keycloak.client-secret" -> context.labelOrEnvironment(ComposeLabels.CLIENT_SECRET, "KEYCLOAK_CLIENT_SECRET", "secret");
                case "micronaut.security.oauth2.clients.keycloak.openid.issuer" -> issuer;
                case "micronaut.security.token.jwt.signatures.jwks.keycloak.url" -> issuer + "/protocol/openid-connect/certs";
                default -> null;
            };
        }),
        service("opensearch", Set.of("opensearch", "elasticsearch"), 9200, List.of("micronaut.opensearch.rest-client.http-hosts", "micronaut.opensearch.httpclient5.http-hosts"), ResolutionContext::hostPort),
        service("opentelemetry", Set.of("opentelemetry", "otel", "lgtm"), 4317, List.of("otel.exporter.otlp.endpoint"), ResolutionContext::http),
        service("pulsar", Set.of("pulsar"), 6650, List.of("pulsar.service-url"), context -> "pulsar://" + context.hostPort()),
        service("seaweedfs", Set.of("seaweedfs"), 8333, List.of("seaweedfs.url", "seaweedfs.access-key", "seaweedfs.secret-key"), context -> switch (context.propertyName()) {
            case "seaweedfs.url" -> context.http();
            case "seaweedfs.access-key" -> context.labelOrEnvironment(ComposeLabels.ACCESS_KEY, "AWS_ACCESS_KEY_ID", "some_access_key1");
            case "seaweedfs.secret-key" -> context.labelOrEnvironment(ComposeLabels.SECRET_KEY, "AWS_SECRET_ACCESS_KEY", "some_secret_key1");
            default -> null;
        }),
        service("solr", Set.of("solr"), 8983, List.of("micronaut.solr.hosts"), context -> context.http() + "/solr"),
        service("wiremock", Set.of("wiremock"), 8080, List.of("wiremock.host", "wiremock.port", "wiremock.url"), context -> switch (context.propertyName()) {
            case "wiremock.host" -> context.endpoint().host();
            case "wiremock.port" -> String.valueOf(context.endpoint().port());
            case "wiremock.url" -> context.http();
            default -> null;
        })
    );

    private ComposeServiceDescriptors() {
    }

    static Collection<String> allCoveredServiceKinds() {
        Set<String> serviceKinds = new LinkedHashSet<>();
        DATABASES.forEach(database -> serviceKinds.add(database.serviceType()));
        SERVICES.forEach(service -> serviceKinds.add(service.serviceType()));
        serviceKinds.add("h2");
        serviceKinds.add("oracle-test-pilot");
        serviceKinds.add("r2dbc-pool");
        serviceKinds.add("generic-testcontainers");
        return serviceKinds;
    }

    static List<Integer> exposedPorts(ComposeService service) {
        return java.util.stream.Stream.concat(
                DATABASES.stream().filter(database -> database.matches(service)).map(DatabaseDescriptor::port),
                SERVICES.stream().filter(descriptor -> descriptor.matches(service)).map(ServiceDescriptor::port)
            )
            .distinct()
            .toList();
    }

    static Optional<ServiceDescriptor> findServiceDescriptor(String propertyName) {
        return SERVICES.stream()
            .filter(descriptor -> descriptor.properties().contains(propertyName) || descriptor.serviceType().equals(MONGODB_SERVICE) && propertyName.startsWith(MONGODB_SERVERS_PREFIX) && propertyName.endsWith(".uri"))
            .findFirst();
    }

    static Optional<DatabaseDescriptor> findDatabaseDescriptor(String propertyName, Map<String, Object> properties) {
        @Nullable String datasource = datasourceName(propertyName);
        if (datasource == null) {
            return Optional.empty();
        }
        for (DatabaseDescriptor descriptor : DATABASES) {
            if (descriptor.requested(datasource, propertyName, properties)) {
                return Optional.of(descriptor);
            }
        }
        return Optional.empty();
    }

    static @Nullable String datasourceName(String expression) {
        if (expression.startsWith(DATASOURCES + ".")) {
            return nameAfterPrefix(expression, DATASOURCES);
        }
        if (expression.startsWith(R2DBC_DATASOURCES + ".")) {
            return nameAfterPrefix(expression, R2DBC_DATASOURCES);
        }
        if (expression.startsWith(JPA + ".")) {
            return nameAfterPrefix(expression, JPA);
        }
        return null;
    }

    static String propertyName(String expression) {
        @Nullable String datasource = datasourceName(expression);
        if (datasource == null) {
            return expression;
        }
        return expression.substring(expression.indexOf(datasource) + datasource.length() + 1);
    }

    static String property(String prefix, String datasource, String property) {
        return prefix + "." + datasource + "." + property;
    }

    static @Nullable String stringValue(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static ServiceDescriptor service(String serviceType, Set<String> aliases, int port, List<String> properties, Function<ResolutionContext, @Nullable String> resolver) {
        return new ServiceDescriptor(serviceType, aliases(serviceType, aliases), port, properties, resolver);
    }

    private static Set<String> aliases(String serviceType, Collection<String> aliases) {
        Set<String> allAliases = new LinkedHashSet<>();
        allAliases.add(normalize(serviceType));
        aliases.stream().map(ComposeServiceDescriptors::normalize).forEach(allAliases::add);
        return Set.copyOf(allAliases);
    }

    private static @Nullable String nameAfterPrefix(String expression, String prefix) {
        int start = prefix.length() + 1;
        int end = expression.indexOf('.', start);
        return end > start ? expression.substring(start, end) : null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    private static String mongoDatabase(String propertyName) {
        if (propertyName.startsWith(MONGODB_SERVERS_PREFIX) && propertyName.endsWith(".uri")) {
            String suffix = propertyName.substring(MONGODB_SERVERS_PREFIX.length());
            return suffix.substring(0, suffix.length() - ".uri".length());
        }
        return "test";
    }

    record DatabaseDescriptor(
        String serviceType,
        Set<String> aliases,
        int port,
        String jdbcScheme,
        String r2dbcScheme,
        String driverClassName,
        String userEnvironment,
        String passwordEnvironment,
        String databaseEnvironment,
        String defaultUser,
        String defaultPassword,
        String defaultDatabase
    ) {
        DatabaseDescriptor {
            aliases = ComposeServiceDescriptors.aliases(serviceType, aliases);
        }

        boolean matches(ComposeService service) {
            return service.explicitService(aliases)
                || aliases.stream().anyMatch(service::imageContains)
                || service.exposes(port);
        }

        boolean requested(String datasource, String propertyName, Map<String, Object> properties) {
            String type = stringValue(properties.get(property(DATASOURCES, datasource, TYPE)));
            if (type == null && propertyName.startsWith(R2DBC_DATASOURCES + ".")) {
                type = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, TYPE)));
            }
            if (type == null && propertyName.startsWith(JPA + ".")) {
                type = stringValue(properties.get(property(JPA, datasource, HIBERNATE_CONNECTION + TYPE)));
            }
            if (type != null && aliases.contains(normalize(type))) {
                return true;
            }
            String dialect = stringValue(properties.get(property(DATASOURCES, datasource, DIALECT)));
            if (dialect == null && propertyName.startsWith(R2DBC_DATASOURCES + ".")) {
                dialect = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, DIALECT)));
            }
            if (dialect != null && aliases.contains(normalize(dialect))) {
                return true;
            }
            String driver = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, R2DBC_DRIVER)));
            return driver != null && aliases.stream().anyMatch(normalize(driver)::contains);
        }

        @Nullable String resolve(String propertyName, ComposeEndpoint endpoint, ComposeService service, Map<String, Object> properties) {
            @Nullable String datasource = datasourceName(propertyName);
            String leaf = propertyName(propertyName);
            return switch (leaf) {
                case URL -> propertyName.startsWith(R2DBC_DATASOURCES + ".") ? r2dbcUrl(endpoint, service, datasource, properties) : jdbcUrl(endpoint, service, datasource, properties);
                case HIBERNATE_CONNECTION + URL -> jdbcUrl(endpoint, service, datasource, properties);
                case USERNAME, HIBERNATE_CONNECTION + USERNAME -> username(service);
                case PASSWORD, HIBERNATE_CONNECTION + PASSWORD -> password(service);
                case DRIVER -> driverClassName;
                default -> null;
            };
        }

        @Nullable String resolveWithoutEndpoint(String propertyName, ComposeService service, Map<String, Object> properties) {
            return switch (propertyName(propertyName)) {
                case USERNAME, HIBERNATE_CONNECTION + USERNAME -> username(service);
                case PASSWORD, HIBERNATE_CONNECTION + PASSWORD -> password(service);
                case DRIVER -> driverClassName;
                default -> null;
            };
        }

        String jdbcUrl(ComposeEndpoint endpoint, ComposeService service, @Nullable String datasource, Map<String, Object> properties) {
            String hostPort = endpoint.hostPort();
            String database = database(service, datasource, properties);
            return switch (serviceType) {
                case MSSQL_SERVICE -> jdbcScheme + "://" + hostPort + ";databaseName=" + database;
                case ORACLE_SERVICE -> jdbcScheme + ":@" + "//" + hostPort + "/" + database;
                default -> jdbcScheme + "://" + hostPort + "/" + database;
            };
        }

        String r2dbcUrl(ComposeEndpoint endpoint, ComposeService service, @Nullable String datasource, Map<String, Object> properties) {
            return "r2dbc:" + r2dbcScheme + "://" + endpoint.hostPort() + "/" + database(service, datasource, properties);
        }

        String username(ComposeService service) {
            return service.labelOrEnvironment(ComposeLabels.USERNAME, userEnvironment, defaultUser);
        }

        String password(ComposeService service) {
            return service.labelOrEnvironment(ComposeLabels.PASSWORD, passwordEnvironment, defaultPassword);
        }

        String database(ComposeService service, @Nullable String datasource, Map<String, Object> properties) {
            @Nullable String configured = stringValue(properties.get(property(DATASOURCES, datasource, DB_NAME)));
            if (configured == null) {
                configured = stringValue(properties.get(property(R2DBC_DATASOURCES, datasource, DB_NAME)));
            }
            return configured == null ? service.labelOrEnvironment(ComposeLabels.DATABASE, databaseEnvironment, defaultDatabase) : configured;
        }
    }

    record ServiceDescriptor(String serviceType, Set<String> aliases, int port, List<String> properties, Function<ResolutionContext, @Nullable String> resolver) {
        boolean matches(ComposeService service) {
            return service.explicitService(aliases)
                || aliases.stream().anyMatch(service::imageContains)
                || service.exposes(port);
        }

        @Nullable String resolve(ResolutionContext context) {
            return resolver.apply(context);
        }
    }

    record ResolutionContext(String propertyName, ComposeEndpoint endpoint, ComposeService service) {
        String hostPort() {
            return endpoint.hostPort();
        }

        String http() {
            return "http://" + hostPort();
        }

        String labelOrEnvironment(String label, String environmentName, String defaultValue) {
            return service.labelOrEnvironment(label, environmentName, defaultValue);
        }

        String database(String defaultValue) {
            return service.labelOrEnvironment(ComposeLabels.DATABASE, "MONGO_INITDB_DATABASE", defaultValue);
        }
    }
}
