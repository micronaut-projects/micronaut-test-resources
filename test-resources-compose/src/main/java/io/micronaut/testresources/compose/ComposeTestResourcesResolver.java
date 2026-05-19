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

import io.micronaut.testresources.core.ToggableTestResourcesResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Resolves supported missing Micronaut properties from a Testcontainers-managed Docker Compose environment.
 */
public final class ComposeTestResourcesResolver implements ToggableTestResourcesResolver {
    static final int POSTGRES_PORT = 5432;
    static final int REDIS_PORT = 6379;
    private static final Logger LOG = LoggerFactory.getLogger(ComposeTestResourcesResolver.class);
    private static final int ORDER = -10;
    private static final String PREFIX = "datasources";
    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DRIVER = "driver-class-name";
    private static final String DB_NAME = "db-name";
    private static final String TYPE = "db-type";
    private static final String DIALECT = "dialect";
    private static final String RESOURCE_NAME = "test-resources.resource-name";
    private static final String REDIS_URI = "redis.uri";
    private static final Set<String> POSTGRES_TYPES = Set.of("postgres", "postgresql", "pg");

    private final ComposeMetadataParser parser;
    private final ComposeEnvironmentManager manager;

    public ComposeTestResourcesResolver() {
        this(new ComposeMetadataParser(), new ComposeContainerManager());
    }

    ComposeTestResourcesResolver(ComposeMetadataParser parser, ComposeEnvironmentManager manager) {
        this.parser = parser;
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "compose";
    }

    @Override
    public String getDisplayName() {
        return "Docker Compose";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public boolean isEnabled(Map<String, Object> testResourcesConfig) {
        Object enabled = testResourcesConfig.get("compose.enabled");
        if (enabled instanceof Boolean b) {
            return b;
        }
        return enabled != null && Boolean.parseBoolean(String.valueOf(enabled));
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        List<String> resolvable = new ArrayList<>();
        Collection<String> datasources = propertyEntries.getOrDefault(PREFIX, List.of());
        for (String datasource : datasources) {
            resolvable.add(PREFIX + "." + datasource + "." + URL);
            resolvable.add(PREFIX + "." + datasource + "." + USERNAME);
            resolvable.add(PREFIX + "." + datasource + "." + PASSWORD);
            resolvable.add(PREFIX + "." + datasource + "." + DRIVER);
        }
        resolvable.add(REDIS_URI);
        return resolvable;
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return List.of(PREFIX);
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        if (!isDatasourceExpression(expression)) {
            return List.of();
        }
        String datasource = datasourceName(expression);
        return Stream.of(TYPE, DIALECT, DB_NAME, RESOURCE_NAME)
            .map(property -> datasourceProperty(datasource, property))
            .toList();
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        ComposeConfiguration configuration = ComposeConfiguration.from(testResourcesConfig, properties);
        if (!configuration.usable()) {
            return Optional.empty();
        }
        try {
            ComposeProject project = parser.parse(configuration);
            if (REDIS_URI.equals(propertyName)) {
                return resolveRedis(configuration, project, properties);
            }
            if (isDatasourceExpression(propertyName)) {
                return resolvePostgres(propertyName, configuration, project, properties);
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.warn("Unable to resolve {} from Docker Compose; falling back to the normal Test Resources provider: {}", propertyName, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> resolvePostgres(String propertyName,
                                             ComposeConfiguration configuration,
                                             ComposeProject project,
                                             Map<String, Object> properties) {
        String datasource = datasourceName(propertyName);
        if (!postgresRequested(datasource, properties)) {
            return Optional.empty();
        }
        Optional<ComposeService> service = match(project, configuration, serviceCandidate -> isPostgresForDatasource(serviceCandidate, datasource), "PostgreSQL datasource '" + datasource + "'");
        if (service.isEmpty()) {
            return Optional.empty();
        }
        ComposeService postgres = service.get();
        return switch (datasourceProperty(propertyName)) {
            case URL -> manager.endpoint(configuration, project, postgres, POSTGRES_PORT, properties)
                .map(endpoint -> "jdbc:postgresql://" + endpoint.hostPort() + "/" + database(postgres, datasource, properties));
            case USERNAME -> Optional.of(username(postgres));
            case PASSWORD -> Optional.of(password(postgres));
            case DRIVER -> Optional.of("org.postgresql.Driver");
            default -> Optional.empty();
        };
    }

    private Optional<String> resolveRedis(ComposeConfiguration configuration, ComposeProject project, Map<String, Object> properties) {
        Optional<ComposeService> service = match(project, configuration, ComposeTestResourcesResolver::isRedisCandidate, "Redis");
        if (service.isEmpty()) {
            return Optional.empty();
        }
        ComposeService redis = service.get();
        if (redis.labels().containsKey(ComposeLabels.PASSWORD) || redis.environment().containsKey("REDIS_PASSWORD")) {
            LOG.warn("Ignoring Docker Compose Redis service {} because authenticated Redis URI mapping is not supported in the initial implementation", redis.name());
            return Optional.empty();
        }
        return manager.endpoint(configuration, project, redis, REDIS_PORT, properties)
            .map(endpoint -> "redis://" + endpoint.hostPort());
    }

    private Optional<ComposeService> match(ComposeProject project,
                                           ComposeConfiguration configuration,
                                           Predicate<ComposeService> predicate,
                                           String description) {
        List<ComposeService> matches = project.services().stream()
            .filter(service -> !service.ignored())
            .filter(service -> service.activeFor(configuration.profiles()))
            .filter(predicate)
            .toList();
        if (matches.isEmpty()) {
            LOG.debug("No Docker Compose service matched {}", description);
            return Optional.empty();
        }
        if (matches.size() > 1) {
            LOG.warn("Multiple Docker Compose services matched {}: {}. Falling back to the normal Test Resources provider.",
                description,
                matches.stream().map(ComposeService::redactedSummary).toList());
            return Optional.empty();
        }
        LOG.info("Matched Docker Compose service {} for {}", matches.get(0).redactedSummary(), description);
        return Optional.of(matches.get(0));
    }

    private boolean isPostgresForDatasource(ComposeService service, String datasource) {
        if (!isPostgresCandidate(service)) {
            return false;
        }
        String label = service.labels().get(ComposeLabels.DATASOURCE);
        if (label != null) {
            return label.equalsIgnoreCase(datasource);
        }
        return "default".equals(datasource);
    }

    static boolean isPostgresCandidate(ComposeService service) {
        return explicitService(service, POSTGRES_TYPES)
            || imageContains(service, "postgres")
            || service.exposes(POSTGRES_PORT);
    }

    static boolean isRedisCandidate(ComposeService service) {
        return explicitService(service, Set.of("redis"))
            || imageContains(service, "redis")
            || service.exposes(REDIS_PORT);
    }

    private static boolean explicitService(ComposeService service, Set<String> names) {
        return service.serviceLabel().filter(names::contains).isPresent();
    }

    private static boolean imageContains(ComposeService service, String token) {
        return service.image().toLowerCase(Locale.ROOT).contains(token);
    }

    private boolean postgresRequested(String datasource, Map<String, Object> properties) {
        String type = stringValue(properties.get(datasourceProperty(datasource, TYPE)));
        if (type != null) {
            return POSTGRES_TYPES.contains(type.toLowerCase(Locale.ROOT));
        }
        String dialect = stringValue(properties.get(datasourceProperty(datasource, DIALECT)));
        return dialect != null && POSTGRES_TYPES.contains(dialect.toLowerCase(Locale.ROOT));
    }

    private static String username(ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.USERNAME, "POSTGRES_USER", "test");
    }

    private static String password(ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.PASSWORD, "POSTGRES_PASSWORD", "test");
    }

    private static String database(ComposeService service, String datasource, Map<String, Object> properties) {
        String configured = stringValue(properties.get(datasourceProperty(datasource, DB_NAME)));
        if (configured != null) {
            return configured;
        }
        return service.labelOrEnvironment(ComposeLabels.DATABASE, "POSTGRES_DB", "test");
    }

    private static boolean isDatasourceExpression(String expression) {
        return expression.startsWith(PREFIX + ".") && expression.indexOf('.', PREFIX.length() + 1) > 0;
    }

    private static String datasourceName(String expression) {
        int start = PREFIX.length() + 1;
        int end = expression.indexOf('.', start);
        return expression.substring(start, end);
    }

    private static String datasourceProperty(String expression) {
        int start = PREFIX.length() + 1;
        int propertyStart = expression.indexOf('.', start) + 1;
        return expression.substring(propertyStart);
    }

    private static String datasourceProperty(String datasource, String property) {
        return PREFIX + "." + datasource + "." + property;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
