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

import io.micronaut.core.annotation.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Maps Compose services to Micronaut configuration properties.
 */
@Internal
final class ComposePropertyMapper {
    private static final Logger LOG = LoggerFactory.getLogger(ComposePropertyMapper.class);
    private static final String DATASOURCES = "datasources";
    private static final String DATASOURCES_PREFIX = DATASOURCES + ".";
    private static final String R2DBC_DATASOURCES = "r2dbc.datasources";
    private static final String R2DBC_DATASOURCES_PREFIX = R2DBC_DATASOURCES + ".";
    private static final String JPA = "jpa";
    private static final String JPA_PREFIX = JPA + ".";
    private static final String MONGODB_SERVERS = "mongodb.servers";
    private static final String MONGODB_SERVERS_PREFIX = MONGODB_SERVERS + ".";
    private static final String DEFAULT_AWS_ACCESS_KEY = "test";
    private static final String DEFAULT_AWS_SECRET_KEY = "test";
    private static final String DEFAULT_AWS_REGION = "us-east-1";
    private static final String DEFAULT_AZURE_ACCOUNT_NAME = "devstoreaccount1";
    private static final String DEFAULT_AZURE_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final String DEFAULT_SEAWEEDFS_ACCESS_KEY = "some_access_key1";
    private static final String DEFAULT_SEAWEEDFS_SECRET_KEY = "some_secret_key1";
    private static final List<String> DATASOURCE_PROPERTIES = List.of("url", "username", "password", "driver-class-name");
    private static final List<String> R2DBC_PROPERTIES = List.of("url", "username", "password");
    private static final List<String> HIBERNATE_REACTIVE_PROPERTIES = List.of(
        "properties.hibernate.connection.url",
        "properties.hibernate.connection.username",
        "properties.hibernate.connection.password"
    );
    private static final List<DatabaseDefinition> DATABASES = List.of(
        new DatabaseDefinition(
            "postgres",
            Set.of("postgres", "postgresql", "pg"),
            5432,
            "jdbc:postgresql",
            "postgresql",
            "org.postgresql.Driver",
            "POSTGRES_USER",
            "POSTGRES_PASSWORD",
            "POSTGRES_DB",
            "postgres",
            "postgres",
            null
        ),
        new DatabaseDefinition(
            "mysql",
            Set.of("mysql"),
            3306,
            "jdbc:mysql",
            "mysql",
            "com.mysql.cj.jdbc.Driver",
            "MYSQL_USER",
            "MYSQL_PASSWORD",
            "MYSQL_DATABASE",
            "test",
            "test",
            "test"
        ),
        new DatabaseDefinition(
            "mariadb",
            Set.of("mariadb", "maria"),
            3306,
            "jdbc:mariadb",
            "mariadb",
            "org.mariadb.jdbc.Driver",
            "MARIADB_USER",
            "MARIADB_PASSWORD",
            "MARIADB_DATABASE",
            "test",
            "test",
            "test"
        ),
        new DatabaseDefinition(
            "mssql",
            Set.of("mssql", "sqlserver", "sql-server", "microsoftsqlserver"),
            1433,
            "jdbc:sqlserver",
            "mssql",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "MSSQL_USER",
            "MSSQL_PASSWORD",
            "MSSQL_DATABASE",
            "SA",
            "A_Str0ng_Required_Password",
            "test"
        ),
        new DatabaseDefinition(
            "oracle",
            Set.of("oracle", "oracle-free", "oracle-xe"),
            1521,
            "jdbc:oracle:thin",
            "oracle",
            "oracle.jdbc.OracleDriver",
            "ORACLE_USER",
            "ORACLE_PASSWORD",
            "ORACLE_DATABASE",
            "test",
            "test",
            "freepdb1"
        )
    );
    private static final List<ServiceDefinition> SERVICES = List.of(
        service("azurite", Set.of("azure-storage"), "azurite", 10000, Map.of(
            "azure.credential.storage-shared-key.account-name", ctx -> DEFAULT_AZURE_ACCOUNT_NAME,
            "azure.credential.storage-shared-key.account-key", ctx -> DEFAULT_AZURE_ACCOUNT_KEY,
            "azure.credential.storage-shared-key.connection-string", ComposePropertyMapper::azuriteConnectionString
        )),
        service("couchbase", Set.of(), "couchbase", 11210, Map.of(
            "couchbase.uri", ctx -> "couchbase://" + hostPort(ctx.port()),
            "couchbase.username", ctx -> ctx.service().labelOrEnvironment(ComposeLabels.USERNAME, "COUCHBASE_USERNAME", "Administrator"),
            "couchbase.password", ctx -> ctx.service().labelOrEnvironment(ComposeLabels.PASSWORD, "COUCHBASE_PASSWORD", "password")
        )),
        service("elasticsearch", Set.of("elastic-search"), "elasticsearch", 9200, Map.of(
            "elasticsearch.http-hosts", ctx -> http(ctx.port())
        )),
        service("hashicorp-consul", Set.of("consul"), "consul", 8500, Map.of(
            "consul.client.host", ctx -> ctx.port().host(),
            "consul.client.port", ctx -> String.valueOf(ctx.port().publishedPort()),
            "consul.client.default-zone", ctx -> hostPort(ctx.port())
        )),
        service("hashicorp-vault", Set.of("vault"), "vault", 8200, Map.of(
            "vault.client.uri", ctx -> http(ctx.port()),
            "vault.client.token", ctx -> ctx.service().labelOrEnvironment(ComposeLabels.PASSWORD, "VAULT_DEV_ROOT_TOKEN_ID", "vault-token")
        )),
        service("hazelcast", Set.of(), "hazelcast", 5701, Map.of(
            "hazelcast.client.network.addresses", ctx -> hostPort(ctx.port())
        )),
        service("hivemq", Set.of("mqtt"), "hivemq", 1883, Map.of(
            "mqtt.client.client-id", ctx -> ctx.service().labelOrEnvironment(
                "io.micronaut.test-resources.client-id",
                "MQTT_CLIENT_ID",
                "micronaut-test-resources-" + UUID.randomUUID()
            ),
            "mqtt.client.server-uri", ctx -> "tcp://" + hostPort(ctx.port())
        )),
        service("infinispan", Set.of(), "infinispan", 11222, Map.of(
            "infinispan.client.hotrod.server.host", ctx -> ctx.port().host(),
            "infinispan.client.hotrod.server.port", ctx -> String.valueOf(ctx.port().publishedPort()),
            "infinispan.client.hotrod.security.authentication.username",
                ctx -> ctx.service().labelOrEnvironment(ComposeLabels.USERNAME, "USER", "admin"),
            "infinispan.client.hotrod.security.authentication.password",
                ctx -> ctx.service().labelOrEnvironment(ComposeLabels.PASSWORD, "PASS", "password")
        )),
        service("kafka", Set.of("redpanda"), "kafka", 9092, Map.of(
            "kafka.bootstrap.servers", ctx -> "PLAINTEXT://" + hostPort(ctx.port())
        )),
        service("localstack", Set.of(), "localstack", 4566, Map.of(
            "aws.access-key-id", ctx -> DEFAULT_AWS_ACCESS_KEY,
            "aws.secret-key", ctx -> DEFAULT_AWS_SECRET_KEY,
            "aws.region", ctx -> DEFAULT_AWS_REGION,
            "aws.services.dynamodb.endpoint-override", ctx -> http(ctx.port()),
            "aws.services.s3.endpoint-override", ctx -> http(ctx.port()),
            "aws.services.sns.endpoint-override", ctx -> http(ctx.port()),
            "aws.services.sqs.endpoint-override", ctx -> http(ctx.port())
        )),
        service("minio", Set.of(), "minio", 9000, Map.of(
            "minio.url", ctx -> http(ctx.port()),
            "minio.access-key", ctx -> ctx.service().labelOrEnvironment(ComposeLabels.USERNAME, "MINIO_ROOT_USER", "minioadmin"),
            "minio.secret-key", ctx -> ctx.service().labelOrEnvironment(ComposeLabels.PASSWORD, "MINIO_ROOT_PASSWORD", "minioadmin")
        )),
        service("mongodb", Set.of("mongo"), "mongo", 27017, Map.of(
            "mongodb.uri", ComposePropertyMapper::mongodbUri
        )),
        service("neo4j", Set.of(), "neo4j", 7687, Map.of(
            "neo4j.uri", ctx -> "bolt://" + hostPort(ctx.port())
        )),
        service("opensearch", Set.of(), "opensearch", 9200, Map.of(
            "micronaut.opensearch.rest-client.http-hosts", ctx -> http(ctx.port()),
            "micronaut.opensearch.httpclient5.http-hosts", ctx -> http(ctx.port())
        )),
        service("pulsar", Set.of(), "pulsar", 6650, Map.of(
            "pulsar.service-url", ctx -> "pulsar://" + hostPort(ctx.port())
        )),
        service("rabbitmq", Set.of(), "rabbitmq", 5672, Map.of(
            "rabbitmq.uri", ComposePropertyMapper::rabbitMqUri,
            "rabbitmq.username", ComposePropertyMapper::rabbitMqUsername,
            "rabbitmq.password", ComposePropertyMapper::rabbitMqPassword
        )),
        service("redis", Set.of(), "redis", 6379, Map.of(
            "redis.uri", ComposePropertyMapper::redisUri,
            "redis.uris", ComposePropertyMapper::redisUri
        )),
        service("seaweedfs", Set.of(), "seaweedfs", 8333, Map.of(
            "seaweedfs.url", ctx -> http(ctx.port()),
            "seaweedfs.access-key", ctx -> DEFAULT_SEAWEEDFS_ACCESS_KEY,
            "seaweedfs.secret-key", ctx -> DEFAULT_SEAWEEDFS_SECRET_KEY
        )),
        service("solr", Set.of(), "solr", 8983, Map.of(
            "micronaut.solr.hosts", ctx -> http(ctx.port()) + "/solr"
        )),
        service("solr-zookeeper", Set.of("zookeeper", "zk"), "zookeeper", 9983, Map.of(
            "micronaut.solr.zk-hosts", ctx -> hostPort(ctx.port())
        )),
        service("keycloak", Set.of(), "keycloak", 8080, Map.of(
            "micronaut.security.oauth2.clients.keycloak.client-id",
                ctx -> ctx.service().labelOrEnvironment("io.micronaut.test-resources.client-id", "KEYCLOAK_CLIENT_ID", "micronaut-test-resources"),
            "micronaut.security.oauth2.clients.keycloak.client-secret",
                ctx -> ctx.service().labelOrEnvironment(ComposeLabels.PASSWORD, "KEYCLOAK_CLIENT_SECRET", "secret"),
            "micronaut.security.oauth2.clients.keycloak.openid.issuer", ComposePropertyMapper::keycloakIssuer,
            "micronaut.security.token.jwt.signatures.jwks.keycloak.url",
                ctx -> keycloakIssuer(ctx) + "/protocol/openid-connect/certs"
        )),
        service("wiremock", Set.of(), "wiremock", 8080, Map.of(
            "wiremock.host", ctx -> ctx.port().host(),
            "wiremock.port", ctx -> String.valueOf(ctx.port().publishedPort()),
            "wiremock.url", ctx -> http(ctx.port())
        ))
    );
    private static final Map<String, ServiceDefinition> PROPERTY_TO_SERVICE = propertyMap();
    private static final List<String> STATIC_PROPERTIES = List.copyOf(PROPERTY_TO_SERVICE.keySet());

    Optional<String> resolve(String propertyName, Map<String, Object> requestedProperties, ComposeProject project) {
        if (propertyName.startsWith(DATASOURCES_PREFIX)) {
            return resolveJdbc(propertyName, requestedProperties, project);
        }
        if (propertyName.startsWith(R2DBC_DATASOURCES_PREFIX)) {
            return resolveR2dbc(propertyName, requestedProperties, project);
        }
        if (propertyName.startsWith(JPA_PREFIX)) {
            return resolveHibernateReactive(propertyName, requestedProperties, project);
        }
        if (propertyName.startsWith(MONGODB_SERVERS_PREFIX)) {
            return resolveMongoServer(propertyName, project);
        }
        ServiceDefinition serviceDefinition = PROPERTY_TO_SERVICE.get(propertyName);
        if (serviceDefinition == null) {
            return Optional.empty();
        }
        return resolveServiceProperty(serviceDefinition, propertyName, project);
    }

    List<String> resolvableProperties(Map<String, Collection<String>> propertyEntries) {
        List<String> properties = new ArrayList<>();
        for (String datasource : propertyEntries.getOrDefault(DATASOURCES, List.of())) {
            for (String property : DATASOURCE_PROPERTIES) {
                properties.add(DATASOURCES_PREFIX + datasource + "." + property);
            }
        }
        Collection<String> r2dbcDatasources = propertyEntries.getOrDefault(R2DBC_DATASOURCES, List.of());
        Collection<String> regularDatasources = propertyEntries.getOrDefault(DATASOURCES, List.of());
        for (String datasource : distinct(r2dbcDatasources, regularDatasources)) {
            for (String property : R2DBC_PROPERTIES) {
                properties.add(R2DBC_DATASOURCES_PREFIX + datasource + "." + property);
            }
        }
        for (String datasource : distinct(propertyEntries.getOrDefault(JPA, List.of()), regularDatasources)) {
            for (String property : HIBERNATE_REACTIVE_PROPERTIES) {
                properties.add(JPA_PREFIX + datasource + "." + property);
            }
        }
        for (String server : propertyEntries.getOrDefault(MONGODB_SERVERS, List.of())) {
            properties.add(MONGODB_SERVERS_PREFIX + server + ".uri");
        }
        properties.addAll(STATIC_PROPERTIES);
        return properties.stream().distinct().toList();
    }

    private Optional<String> resolveJdbc(String propertyName, Map<String, Object> requestedProperties, ComposeProject project) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName, DATASOURCES_PREFIX);
        if (datasourceProperty == null || !DATASOURCE_PROPERTIES.contains(datasourceProperty.property())) {
            return Optional.empty();
        }
        return databaseDefinition(datasourceProperty.datasource(), requestedProperties, false)
            .flatMap(database -> findSingle(
                project,
                database.serviceType(),
                service -> service.publishedPort(database.port()).isPresent()
                    && matchesDatasource(service, datasourceProperty.datasource())
            ).flatMap(service -> jdbcProperty(datasourceProperty, database, service)));
    }

    private Optional<String> resolveR2dbc(String propertyName, Map<String, Object> requestedProperties, ComposeProject project) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName, R2DBC_DATASOURCES_PREFIX);
        if (datasourceProperty == null || !R2DBC_PROPERTIES.contains(datasourceProperty.property())) {
            return Optional.empty();
        }
        return databaseDefinition(datasourceProperty.datasource(), requestedProperties, true)
            .flatMap(database -> findSingle(
                project,
                database.serviceType(),
                service -> service.publishedPort(database.port()).isPresent()
                    && matchesDatasource(service, datasourceProperty.datasource())
            ).flatMap(service -> r2dbcProperty(datasourceProperty, database, service, requestedProperties)));
    }

    private Optional<String> resolveHibernateReactive(String propertyName, Map<String, Object> requestedProperties, ComposeProject project) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName, JPA_PREFIX);
        if (datasourceProperty == null || !HIBERNATE_REACTIVE_PROPERTIES.contains(datasourceProperty.property())) {
            return Optional.empty();
        }
        return databaseDefinition(datasourceProperty.datasource(), requestedProperties, true)
            .flatMap(database -> findSingle(
                project,
                database.serviceType(),
                service -> service.publishedPort(database.port()).isPresent()
                    && matchesDatasource(service, datasourceProperty.datasource())
            ).flatMap(service -> hibernateReactiveProperty(datasourceProperty, database, service, requestedProperties)));
    }

    private Optional<String> resolveMongoServer(String propertyName, ComposeProject project) {
        String server = mongoServerName(propertyName);
        if (server == null) {
            return Optional.empty();
        }
        return findSingle(
            project,
            "mongodb",
            service -> service.publishedPort(27017).isPresent() && matchesDatasource(service, server)
        ).flatMap(service -> service.publishedPort(27017)
            .map(port -> mongodbUri(new ResolutionContext(service, port))));
    }

    private Optional<String> resolveServiceProperty(ServiceDefinition definition, String propertyName, ComposeProject project) {
        return findSingle(
            project,
            definition.serviceType(),
            service -> service.publishedPort(definition.port()).isPresent()
        ).flatMap(service -> service.publishedPort(definition.port())
            .map(port -> definition.properties().get(propertyName).apply(new ResolutionContext(service, port))));
    }

    private Optional<ComposeService> findSingle(ComposeProject project, String serviceType, Predicate<ComposeService> additionalFilter) {
        List<ComposeService> candidates = project.services()
            .stream()
            .filter(service -> !service.ignored())
            .filter(service -> matchesServiceType(service, serviceType))
            .filter(additionalFilter)
            .toList();
        if (candidates.isEmpty()) {
            LOG.debug("No Docker Compose {} service matched the requested property", serviceType);
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            LOG.warn("Multiple Docker Compose {} services match the requested property. Add '{}' labels to make the mapping explicit. Matched services: {}",
                serviceType,
                ComposeLabels.SERVICE,
                candidates.stream().map(ComposeService::name).toList());
            return Optional.empty();
        }
        ComposeService service = candidates.get(0);
        LOG.info("Matched Docker Compose service '{}' as {}", service.name(), serviceType);
        return Optional.of(service);
    }

    private boolean matchesServiceType(ComposeService service, String serviceType) {
        Optional<String> label = service.serviceLabel();
        if (label.isPresent()) {
            return serviceAliases(serviceType).contains(label.get());
        }
        String normalizedImage = normalize(service.image());
        return serviceAliases(serviceType).stream().anyMatch(alias -> normalizedImage.contains(normalize(alias)));
    }

    private Set<String> serviceAliases(String serviceType) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(serviceType);
        DATABASES.stream()
            .filter(database -> database.serviceType().equals(serviceType))
            .findFirst()
            .ifPresent(database -> aliases.addAll(database.aliases()));
        SERVICES.stream()
            .filter(service -> service.serviceType().equals(serviceType))
            .findFirst()
            .ifPresent(service -> aliases.addAll(service.aliases()));
        return aliases;
    }

    private boolean matchesDatasource(ComposeService service, String datasource) {
        String explicitDatasource = service.labels().get(ComposeLabels.DATASOURCE);
        if (explicitDatasource != null) {
            return explicitDatasource.equalsIgnoreCase(datasource);
        }
        return "default".equals(datasource);
    }

    private Optional<DatabaseDefinition> databaseDefinition(String datasource, Map<String, Object> requestedProperties, boolean requireType) {
        String type = stringOrNull(requestedProperties.get(datasourceExpressionOf(DATASOURCES_PREFIX, datasource, "db-type")));
        if (type == null) {
            type = stringOrNull(requestedProperties.get(datasourceExpressionOf(R2DBC_DATASOURCES_PREFIX, datasource, "db-type")));
        }
        if (type == null) {
            type = stringOrNull(requestedProperties.get(datasourceExpressionOf(JPA_PREFIX, datasource, "properties.hibernate.connection.db-type")));
        }
        if (type == null) {
            type = stringOrNull(requestedProperties.get(datasourceExpressionOf(DATASOURCES_PREFIX, datasource, "dialect")));
        }
        if (type == null) {
            type = stringOrNull(requestedProperties.get(datasourceExpressionOf(R2DBC_DATASOURCES_PREFIX, datasource, "dialect")));
        }
        if (type == null) {
            return requireType ? Optional.empty() : DATABASES.stream().filter(database -> "postgres".equals(database.serviceType())).findFirst();
        }
        String normalized = normalize(type);
        return DATABASES.stream()
            .filter(database -> database.aliases().stream().map(ComposePropertyMapper::normalize).anyMatch(normalized::equals))
            .findFirst();
    }

    private Optional<String> jdbcProperty(DatasourceProperty property, DatabaseDefinition database, ComposeService service) {
        return service.publishedPort(database.port())
            .map(port -> switch (property.property()) {
                case "url" -> jdbcUrl(database, service, port);
                case "username" -> username(database, service);
                case "password" -> password(database, service);
                case "driver-class-name" -> database.driverClassName();
                default -> null;
            });
    }

    private Optional<String> r2dbcProperty(DatasourceProperty property,
                                           DatabaseDefinition database,
                                           ComposeService service,
                                           Map<String, Object> requestedProperties) {
        return service.publishedPort(database.port())
            .map(port -> switch (property.property()) {
                case "url" -> "r2dbc:" + database.r2dbcDriver() + "://" + hostPort(port) + "/" + databaseName(database, service, requestedProperties, property.datasource());
                case "username" -> username(database, service);
                case "password" -> password(database, service);
                default -> null;
            });
    }

    private Optional<String> hibernateReactiveProperty(DatasourceProperty property,
                                                       DatabaseDefinition database,
                                                       ComposeService service,
                                                       Map<String, Object> requestedProperties) {
        return service.publishedPort(database.port())
            .map(port -> switch (property.property()) {
                case "properties.hibernate.connection.url" -> jdbcUrl(database, service, port, requestedProperties, property.datasource());
                case "properties.hibernate.connection.username" -> username(database, service);
                case "properties.hibernate.connection.password" -> password(database, service);
                default -> null;
            });
    }

    private static String jdbcUrl(DatabaseDefinition database, ComposeService service, ComposePort port) {
        return jdbcUrl(database, service, port, Map.of(), "default");
    }

    private static String jdbcUrl(DatabaseDefinition database,
                                  ComposeService service,
                                  ComposePort port,
                                  Map<String, Object> requestedProperties,
                                  String datasource) {
        String databaseName = databaseName(database, service, requestedProperties, datasource);
        return switch (database.serviceType()) {
            case "mssql" -> database.jdbcPrefix() + "://" + hostPort(port) + ";databaseName=" + databaseName + ";encrypt=false";
            case "oracle" -> database.jdbcPrefix() + ":@" + hostPort(port) + "/" + databaseName;
            default -> database.jdbcPrefix() + "://" + hostPort(port) + "/" + databaseName;
        };
    }

    private static String username(DatabaseDefinition database, ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.USERNAME, database.userEnv(), database.defaultUsername());
    }

    private static String password(DatabaseDefinition database, ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.PASSWORD, database.passwordEnv(), database.defaultPassword());
    }

    private static String databaseName(DatabaseDefinition database, ComposeService service, Map<String, Object> requestedProperties, String datasource) {
        Object requestedDbName = requestedProperties.get(datasourceExpressionOf(DATASOURCES_PREFIX, datasource, "db-name"));
        if (requestedDbName == null) {
            requestedDbName = requestedProperties.get(datasourceExpressionOf(R2DBC_DATASOURCES_PREFIX, datasource, "db-name"));
        }
        if (requestedDbName != null) {
            return String.valueOf(requestedDbName);
        }
        String defaultDatabase = database.defaultDatabase() == null ? username(database, service) : database.defaultDatabase();
        return service.labelOrEnvironment(ComposeLabels.DATABASE, database.databaseEnv(), defaultDatabase);
    }

    private static String mongoServerName(String propertyName) {
        String suffix = propertyName.substring(MONGODB_SERVERS_PREFIX.length());
        int separator = suffix.indexOf('.');
        if (separator < 1 || separator == suffix.length() - 1) {
            return null;
        }
        return "uri".equals(suffix.substring(separator + 1)) ? suffix.substring(0, separator) : null;
    }

    private static String mongodbUri(ResolutionContext context) {
        String database = context.service().labelOrEnvironment(ComposeLabels.DATABASE, "MONGO_INITDB_DATABASE", "");
        String path = database.isBlank() ? "" : "/" + database;
        return "mongodb://" + hostPort(context.port()) + path;
    }

    private static String redisUri(ResolutionContext context) {
        if (context.service().environment().containsKey("REDIS_PASSWORD")) {
            LOG.warn("Docker Compose Redis service '{}' declares REDIS_PASSWORD. Password-protected Redis Compose mapping is not inferred; add explicit application configuration or use the default provider.", context.service().name());
            return null;
        }
        return "redis://" + hostPort(context.port());
    }

    private static String rabbitMqUri(ResolutionContext context) {
        return "amqp://" + rabbitMqUsername(context) + ":" + rabbitMqPassword(context) + "@" + hostPort(context.port());
    }

    private static String rabbitMqUsername(ResolutionContext context) {
        return context.service().labelOrEnvironment(ComposeLabels.USERNAME, "RABBITMQ_DEFAULT_USER", "guest");
    }

    private static String rabbitMqPassword(ResolutionContext context) {
        return context.service().labelOrEnvironment(ComposeLabels.PASSWORD, "RABBITMQ_DEFAULT_PASS", "guest");
    }

    private static String azuriteConnectionString(ResolutionContext context) {
        return "DefaultEndpointsProtocol=http;"
            + "AccountName=" + DEFAULT_AZURE_ACCOUNT_NAME + ";"
            + "AccountKey=" + DEFAULT_AZURE_ACCOUNT_KEY + ";"
            + "BlobEndpoint=" + endpoint(context.service(), 10000, DEFAULT_AZURE_ACCOUNT_NAME) + ";"
            + "QueueEndpoint=" + endpoint(context.service(), 10001, DEFAULT_AZURE_ACCOUNT_NAME) + ";"
            + "TableEndpoint=" + endpoint(context.service(), 10002, DEFAULT_AZURE_ACCOUNT_NAME) + ";";
    }

    private static String keycloakIssuer(ResolutionContext context) {
        String realm = context.service().labelOrEnvironment("io.micronaut.test-resources.realm", "KEYCLOAK_REALM", "micronaut");
        return http(context.port()) + "/realms/" + realm;
    }

    private static String endpoint(ComposeService service, int targetPort, String path) {
        return service.publishedPort(targetPort)
            .map(port -> http(port) + "/" + path)
            .orElse("");
    }

    private static ServiceDefinition service(String serviceType,
                                             Set<String> aliases,
                                             String imageHint,
                                             int port,
                                             Map<String, Function<ResolutionContext, String>> properties) {
        Set<String> allAliases = new LinkedHashSet<>();
        allAliases.add(serviceType);
        allAliases.add(imageHint);
        allAliases.addAll(aliases);
        return new ServiceDefinition(serviceType, allAliases, port, properties);
    }

    private static Map<String, ServiceDefinition> propertyMap() {
        Map<String, ServiceDefinition> result = new LinkedHashMap<>();
        for (ServiceDefinition service : SERVICES) {
            for (String property : service.properties().keySet()) {
                result.put(property, service);
            }
        }
        return result;
    }

    private static Collection<String> distinct(Collection<String> left, Collection<String> right) {
        Set<String> values = new LinkedHashSet<>(left);
        values.addAll(right);
        return values;
    }

    private static String datasourceExpressionOf(String prefix, String datasource, String property) {
        return prefix + datasource + "." + property;
    }

    private static String hostPort(ComposePort port) {
        return port.host() + ":" + port.publishedPort();
    }

    private static String http(ComposePort port) {
        return "http://" + hostPort(port);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record DatabaseDefinition(
        String serviceType,
        Set<String> aliases,
        int port,
        String jdbcPrefix,
        String r2dbcDriver,
        String driverClassName,
        String userEnv,
        String passwordEnv,
        String databaseEnv,
        String defaultUsername,
        String defaultPassword,
        String defaultDatabase
    ) {
    }

    private record ServiceDefinition(
        String serviceType,
        Set<String> aliases,
        int port,
        Map<String, Function<ResolutionContext, String>> properties
    ) {
    }

    private record ResolutionContext(ComposeService service, ComposePort port) {
    }

    private record DatasourceProperty(String datasource, String property) {
        static DatasourceProperty parse(String propertyName, String prefix) {
            String remainder = propertyName.substring(prefix.length());
            int separator = remainder.indexOf('.');
            if (separator < 1 || separator == remainder.length() - 1) {
                return null;
            }
            return new DatasourceProperty(remainder.substring(0, separator), remainder.substring(separator + 1));
        }
    }
}
