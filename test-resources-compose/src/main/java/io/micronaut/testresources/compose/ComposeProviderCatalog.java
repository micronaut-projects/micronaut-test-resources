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

import io.micronaut.testresources.compose.AbstractComposeDatabaseTestResourcesProvider.Metadata;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Compose mappers for provider modules whose property contracts are stable and do not need provider implementation classes.
 */
@SuppressWarnings("checkstyle:MissingJavadocType")
public final class ComposeProviderCatalog {
    private static final String USERNAME = ComposeLabels.USERNAME;
    private static final String PASSWORD = ComposeLabels.PASSWORD;
    private static final String ACCESS_KEY = ComposeLabels.ACCESS_KEY;
    private static final String SECRET_KEY = ComposeLabels.SECRET_KEY;

    private ComposeProviderCatalog() {
    }

    private static Metadata postgres() {
        return new Metadata("postgres", Set.of("postgres", "postgresql", "pg"), 5432, "jdbc:postgresql", "postgresql",
            "org.postgresql.Driver", "POSTGRES_USER", "POSTGRES_PASSWORD", "POSTGRES_DB", "test", "test", "test");
    }

    private static Metadata mysql() {
        return new Metadata("mysql", Set.of("mysql"), 3306, "jdbc:mysql", "mysql",
            "com.mysql.cj.jdbc.Driver", "MYSQL_USER", "MYSQL_PASSWORD", "MYSQL_DATABASE", "test", "test", "test");
    }

    private static Metadata mariaDb() {
        return new Metadata("mariadb", Set.of("mariadb", "maria"), 3306, "jdbc:mariadb", "mariadb",
            "org.mariadb.jdbc.Driver", "MARIADB_USER", "MARIADB_PASSWORD", "MARIADB_DATABASE", "test", "test", "test");
    }

    private static Metadata mssql() {
        return new Metadata("mssql", Set.of("mssql", "sqlserver", "sql-server", "microsoftsqlserver"), 1433, "jdbc:sqlserver", "mssql",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver", "MSSQL_USER", "MSSQL_PASSWORD", "MSSQL_DATABASE", "SA", "A_Str0ng_Required_Password", "test");
    }

    private static Metadata oracle() {
        return new Metadata("oracle", Set.of("oracle", "oracle-free", "oracle-xe"), 1521, "jdbc:oracle:thin", "oracle",
            "oracle.jdbc.OracleDriver", "ORACLE_USER", "ORACLE_PASSWORD", "ORACLE_DATABASE", "test", "test", "freepdb1");
    }

    abstract static class Simple extends AbstractComposeTestResourcesProvider {
        Simple(String serviceType, Collection<String> aliases, int port, List<String> properties) {
            super(serviceType, aliases, port, properties);
        }
    }

    abstract static class Database extends AbstractComposeDatabaseTestResourcesProvider {
        Database(Kind kind, Metadata metadata) {
            super(kind, metadata);
        }
    }

    public static final class MySqlJdbc extends Database {
        public MySqlJdbc() {
            super(Kind.JDBC, mysql());
        }
    }

    public static final class MariaDbJdbc extends Database {
        public MariaDbJdbc() {
            super(Kind.JDBC, mariaDb());
        }
    }

    public static final class MssqlJdbc extends Database {
        public MssqlJdbc() {
            super(Kind.JDBC, mssql());
        }
    }

    public static final class OracleFreeJdbc extends Database {
        public OracleFreeJdbc() {
            super(Kind.JDBC, oracle());
        }
    }

    public static final class OracleXeJdbc extends Database {
        public OracleXeJdbc() {
            super(Kind.JDBC, oracle());
        }
    }

    public static final class PostgresR2dbc extends Database {
        public PostgresR2dbc() {
            super(Kind.R2DBC, postgres());
        }
    }

    public static final class MySqlR2dbc extends Database {
        public MySqlR2dbc() {
            super(Kind.R2DBC, mysql());
        }
    }

    public static final class MariaDbR2dbc extends Database {
        public MariaDbR2dbc() {
            super(Kind.R2DBC, mariaDb());
        }
    }

    public static final class MssqlR2dbc extends Database {
        public MssqlR2dbc() {
            super(Kind.R2DBC, mssql());
        }
    }

    public static final class OracleFreeR2dbc extends Database {
        public OracleFreeR2dbc() {
            super(Kind.R2DBC, oracle());
        }
    }

    public static final class OracleXeR2dbc extends Database {
        public OracleXeR2dbc() {
            super(Kind.R2DBC, oracle());
        }
    }

    public static final class PostgresHibernateReactive extends Database {
        public PostgresHibernateReactive() {
            super(Kind.HIBERNATE_REACTIVE, postgres());
        }
    }

    public static final class MySqlHibernateReactive extends Database {
        public MySqlHibernateReactive() {
            super(Kind.HIBERNATE_REACTIVE, mysql());
        }
    }

    public static final class MariaDbHibernateReactive extends Database {
        public MariaDbHibernateReactive() {
            super(Kind.HIBERNATE_REACTIVE, mariaDb());
        }
    }

    public static final class MssqlHibernateReactive extends Database {
        public MssqlHibernateReactive() {
            super(Kind.HIBERNATE_REACTIVE, mssql());
        }
    }

    public static final class OracleFreeHibernateReactive extends Database {
        public OracleFreeHibernateReactive() {
            super(Kind.HIBERNATE_REACTIVE, oracle());
        }
    }

    public static final class OracleXeHibernateReactive extends Database {
        public OracleXeHibernateReactive() {
            super(Kind.HIBERNATE_REACTIVE, oracle());
        }
    }

    public static final class Azurite extends Simple {
        private static final String ACCOUNT_NAME = "azure.credential.storage-shared-key.account-name";
        private static final String ACCOUNT_KEY = "azure.credential.storage-shared-key.account-key";
        private static final String CONNECTION_STRING = "azure.credential.storage-shared-key.connection-string";
        private static final String DEFAULT_ACCOUNT_NAME = "devstoreaccount1";
        private static final String DEFAULT_ACCOUNT_KEY =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

        public Azurite() {
            super("azurite", Set.of("azure-storage"), 10000, List.of(ACCOUNT_NAME, ACCOUNT_KEY, CONNECTION_STRING));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case ACCOUNT_NAME -> DEFAULT_ACCOUNT_NAME;
                case ACCOUNT_KEY -> DEFAULT_ACCOUNT_KEY;
                case CONNECTION_STRING -> "DefaultEndpointsProtocol=http;"
                    + "AccountName=" + DEFAULT_ACCOUNT_NAME + ";"
                    + "AccountKey=" + DEFAULT_ACCOUNT_KEY + ";"
                    + "BlobEndpoint=" + endpoint(context, 10000) + ";"
                    + "QueueEndpoint=" + endpoint(context, 10001) + ";"
                    + "TableEndpoint=" + endpoint(context, 10002) + ";";
                default -> null;
            };
        }

        private static String endpoint(ResolutionContext context, int port) {
            return context.http(port).map(value -> value + "/" + DEFAULT_ACCOUNT_NAME).orElse("");
        }
    }

    public static final class Couchbase extends Simple {
        public Couchbase() {
            super("couchbase", Set.of(), 11210, List.of("couchbase.uri", "couchbase.username", "couchbase.password"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "couchbase.uri" -> "couchbase://" + context.hostPort();
                case "couchbase.username" -> context.labelOrEnvironment(USERNAME, "COUCHBASE_USERNAME", "Administrator");
                case "couchbase.password" -> context.labelOrEnvironment(PASSWORD, "COUCHBASE_PASSWORD", "password");
                default -> null;
            };
        }
    }

    public static final class Consul extends Simple {
        public Consul() {
            super("hashicorp-consul", Set.of("consul"), 8500, List.of("consul.client.host", "consul.client.port", "consul.client.default-zone"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "consul.client.host" -> context.host();
                case "consul.client.port" -> String.valueOf(context.port());
                case "consul.client.default-zone" -> context.hostPort();
                default -> null;
            };
        }
    }

    public static final class Vault extends Simple {
        public Vault() {
            super("hashicorp-vault", Set.of("vault"), 8200, List.of("vault.client.uri", "vault.client.token"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "vault.client.uri" -> context.http();
                case "vault.client.token" -> context.labelOrEnvironment(ComposeLabels.TOKEN, "VAULT_DEV_ROOT_TOKEN_ID", "vault-token");
                default -> null;
            };
        }
    }

    public static final class Hazelcast extends Simple {
        public Hazelcast() {
            super("hazelcast", Set.of(), 5701, List.of("hazelcast.client.network.addresses"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return context.hostPort();
        }
    }

    public static final class HiveMq extends Simple {
        public HiveMq() {
            super("hivemq", Set.of("mqtt"), 1883, List.of("mqtt.client.client-id", "mqtt.client.server-uri"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "mqtt.client.client-id" -> context.labelOrEnvironment("io.micronaut.test-resources.client-id", "MQTT_CLIENT_ID", "micronaut-test-resources-" + UUID.randomUUID());
                case "mqtt.client.server-uri" -> "tcp://" + context.hostPort();
                default -> null;
            };
        }
    }

    public static final class Infinispan extends Simple {
        public Infinispan() {
            super("infinispan", Set.of(), 11222, List.of(
                "infinispan.client.hotrod.server.host",
                "infinispan.client.hotrod.server.port",
                "infinispan.client.hotrod.security.authentication.username",
                "infinispan.client.hotrod.security.authentication.password"
            ));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "infinispan.client.hotrod.server.host" -> context.host();
                case "infinispan.client.hotrod.server.port" -> String.valueOf(context.port());
                case "infinispan.client.hotrod.security.authentication.username" -> context.labelOrEnvironment(USERNAME, "USER", "admin");
                case "infinispan.client.hotrod.security.authentication.password" -> context.labelOrEnvironment(PASSWORD, "PASS", "password");
                default -> null;
            };
        }
    }

    public static final class MailpitSmtp extends Simple {
        public MailpitSmtp() {
            super("mailpit", Set.of(), 1025, List.of(
                "javamail.properties.mail.smtp.host",
                "javamail.properties.mail.smtp.port",
                "javamail.properties.mail.smtp.auth",
                "javamail.properties.mail.smtp.starttls.enable"
            ));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "javamail.properties.mail.smtp.host" -> context.host();
                case "javamail.properties.mail.smtp.port" -> String.valueOf(context.port());
                case "javamail.properties.mail.smtp.auth", "javamail.properties.mail.smtp.starttls.enable" -> "false";
                default -> null;
            };
        }
    }

    public static final class MailpitHttp extends Simple {
        public MailpitHttp() {
            super("mailpit", Set.of(), 8025, List.of("mailpit.ui.url", "mailpit.api.url"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "mailpit.ui.url" -> context.http();
                case "mailpit.api.url" -> context.http() + "/api/v1";
                default -> null;
            };
        }
    }

    public static final class Minio extends Simple {
        public Minio() {
            super("minio", Set.of(), 9000, List.of("minio.url", "minio.access-key", "minio.secret-key"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "minio.url" -> context.http();
                case "minio.access-key" -> context.labelOrEnvironment(ACCESS_KEY, "MINIO_ROOT_USER", "minioadmin");
                case "minio.secret-key" -> context.labelOrEnvironment(SECRET_KEY, "MINIO_ROOT_PASSWORD", "minioadmin");
                default -> null;
            };
        }
    }

    public static final class Neo4j extends Simple {
        public Neo4j() {
            super("neo4j", Set.of(), 7687, List.of("neo4j.uri"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return "bolt://" + context.hostPort();
        }
    }

    public static final class Keycloak extends Simple {
        public Keycloak() {
            super("keycloak", Set.of(), 8080, List.of(
                "micronaut.security.oauth2.clients.keycloak.client-id",
                "micronaut.security.oauth2.clients.keycloak.client-secret",
                "micronaut.security.oauth2.clients.keycloak.openid.issuer",
                "micronaut.security.token.jwt.signatures.jwks.keycloak.url"
            ));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "micronaut.security.oauth2.clients.keycloak.client-id" -> context.labelOrEnvironment(ComposeLabels.CLIENT_ID, "KEYCLOAK_CLIENT_ID", "micronaut-test-resources");
                case "micronaut.security.oauth2.clients.keycloak.client-secret" -> context.labelOrEnvironment(ComposeLabels.CLIENT_SECRET, "KEYCLOAK_CLIENT_SECRET", "secret");
                case "micronaut.security.oauth2.clients.keycloak.openid.issuer" -> issuer(context);
                case "micronaut.security.token.jwt.signatures.jwks.keycloak.url" -> issuer(context) + "/protocol/openid-connect/certs";
                default -> null;
            };
        }

        private static String issuer(ResolutionContext context) {
            return context.http() + "/realms/" + context.labelOrEnvironment(ComposeLabels.REALM, "KEYCLOAK_REALM", "micronaut");
        }
    }

    public static final class OpenSearch extends Simple {
        public OpenSearch() {
            super("opensearch", Set.of("elasticsearch"), 9200, List.of(
                "micronaut.opensearch.rest-client.http-hosts",
                "micronaut.opensearch.httpclient5.http-hosts"
            ));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return context.http();
        }
    }

    public static final class OpenTelemetry extends Simple {
        public OpenTelemetry() {
            super("opentelemetry", Set.of("otel", "lgtm"), 4317, List.of("otel.exporter.otlp.endpoint"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return context.http();
        }
    }

    public static final class Pulsar extends Simple {
        public Pulsar() {
            super("pulsar", Set.of(), 6650, List.of("pulsar.service-url"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return "pulsar://" + context.hostPort();
        }
    }

    public static final class SeaweedFs extends Simple {
        public SeaweedFs() {
            super("seaweedfs", Set.of(), 8333, List.of("seaweedfs.url", "seaweedfs.access-key", "seaweedfs.secret-key"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "seaweedfs.url" -> context.http();
                case "seaweedfs.access-key" -> context.labelOrEnvironment(ACCESS_KEY, "SEAWEEDFS_ACCESS_KEY", "some_access_key1");
                case "seaweedfs.secret-key" -> context.labelOrEnvironment(SECRET_KEY, "SEAWEEDFS_SECRET_KEY", "some_secret_key1");
                default -> null;
            };
        }
    }

    public static final class Solr extends Simple {
        public Solr() {
            super("solr", Set.of(), 8983, List.of("micronaut.solr.hosts", "solr.config.url", "solr.schema.url"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "micronaut.solr.hosts" -> context.http() + "/solr";
                case "solr.config.url", "solr.schema.url" -> context.properties().get(context.propertyName()) == null ? null : String.valueOf(context.properties().get(context.propertyName()));
                default -> null;
            };
        }
    }

    public static final class SolrZookeeper extends Simple {
        public SolrZookeeper() {
            super("solr-zookeeper", Set.of("zookeeper", "zk"), 9983, List.of("micronaut.solr.zk-hosts"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return context.hostPort();
        }
    }

    public static final class WireMock extends Simple {
        public WireMock() {
            super("wiremock", Set.of(), 8080, List.of("wiremock.host", "wiremock.port", "wiremock.url"));
        }

        @Override
        public @Nullable String resolve(ResolutionContext context) {
            return switch (context.propertyName()) {
                case "wiremock.host" -> context.host();
                case "wiremock.port" -> String.valueOf(context.port());
                case "wiremock.url" -> context.http();
                default -> null;
            };
        }
    }

}
