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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Maps Compose services to Micronaut configuration properties.
 */
@Internal
final class ComposePropertyMapper {
    private static final Logger LOG = LoggerFactory.getLogger(ComposePropertyMapper.class);
    private static final String DATASOURCES_PREFIX = "datasources.";
    private static final String POSTGRES_SERVICE = "postgres";
    private static final String REDIS_SERVICE = "redis";
    private static final String RABBITMQ_SERVICE = "rabbitmq";
    private static final Set<String> POSTGRES_TYPES = Set.of("postgres", "postgresql", "pg");

    Optional<String> resolve(String propertyName, Map<String, Object> requestedProperties, ComposeProject project) {
        if (propertyName.startsWith(DATASOURCES_PREFIX)) {
            return resolvePostgres(propertyName, requestedProperties, project);
        }
        if ("redis.uri".equals(propertyName)) {
            return findSingle(project, REDIS_SERVICE, service -> service.publishedPort(6379).isPresent())
                .flatMap(this::redisUri);
        }
        if (propertyName.startsWith("rabbitmq.")) {
            return findSingle(project, RABBITMQ_SERVICE, service -> service.publishedPort(5672).isPresent())
                .flatMap(service -> rabbitMqProperty(propertyName, service));
        }
        return Optional.empty();
    }

    private Optional<String> resolvePostgres(String propertyName, Map<String, Object> requestedProperties, ComposeProject project) {
        DatasourceProperty datasourceProperty = DatasourceProperty.parse(propertyName);
        if (datasourceProperty == null || !datasourceProperty.supported()) {
            return Optional.empty();
        }
        if (!requestedDbTypeAllowsPostgres(datasourceProperty.datasource(), requestedProperties)) {
            return Optional.empty();
        }
        return findSingle(
            project,
            POSTGRES_SERVICE,
            service -> service.publishedPort(5432).isPresent() && matchesDatasource(service, datasourceProperty.datasource())
        ).flatMap(service -> postgresProperty(datasourceProperty.property(), service));
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
            return serviceType.equals(label.get()) || (POSTGRES_SERVICE.equals(serviceType) && "postgresql".equals(label.get()));
        }
        String image = service.image().toLowerCase(Locale.ROOT);
        return switch (serviceType) {
            case POSTGRES_SERVICE -> image.contains(POSTGRES_SERVICE);
            case REDIS_SERVICE -> image.contains(REDIS_SERVICE);
            case RABBITMQ_SERVICE -> image.contains(RABBITMQ_SERVICE);
            default -> false;
        };
    }

    private boolean matchesDatasource(ComposeService service, String datasource) {
        String explicitDatasource = service.labels().get(ComposeLabels.DATASOURCE);
        if (explicitDatasource != null) {
            return explicitDatasource.equalsIgnoreCase(datasource);
        }
        return "default".equals(datasource);
    }

    private boolean requestedDbTypeAllowsPostgres(String datasource, Map<String, Object> requestedProperties) {
        Object type = requestedProperties.get(datasourceExpressionOf(datasource, "db-type"));
        if (type != null) {
            return POSTGRES_TYPES.contains(String.valueOf(type).toLowerCase(Locale.ROOT));
        }
        Object dialect = requestedProperties.get(datasourceExpressionOf(datasource, "dialect"));
        return dialect == null || POSTGRES_TYPES.contains(String.valueOf(dialect).toLowerCase(Locale.ROOT));
    }

    private Optional<String> postgresProperty(String property, ComposeService service) {
        return switch (property) {
            case "url" -> service.publishedPort(5432)
                .map(port -> "jdbc:postgresql://" + port.host() + ":" + port.publishedPort() + "/" + database(service));
            case "username" -> Optional.of(username(service));
            case "password" -> Optional.of(password(service));
            case "driver-class-name" -> Optional.of("org.postgresql.Driver");
            default -> Optional.empty();
        };
    }

    private Optional<String> redisUri(ComposeService service) {
        if (service.environment().containsKey("REDIS_PASSWORD")) {
            LOG.warn("Docker Compose Redis service '{}' declares REDIS_PASSWORD. Password-protected Redis Compose mapping is not inferred; add explicit application configuration or use the default provider.", service.name());
            return Optional.empty();
        }
        return service.publishedPort(6379)
            .map(port -> "redis://" + port.host() + ":" + port.publishedPort());
    }

    private Optional<String> rabbitMqProperty(String propertyName, ComposeService service) {
        String username = service.labelOrEnvironment(ComposeLabels.USERNAME, "RABBITMQ_DEFAULT_USER", "guest");
        String password = service.labelOrEnvironment(ComposeLabels.PASSWORD, "RABBITMQ_DEFAULT_PASS", "guest");
        return switch (propertyName) {
            case "rabbitmq.uri" -> service.publishedPort(5672)
                .map(port -> "amqp://" + username + ":" + password + "@" + port.host() + ":" + port.publishedPort());
            case "rabbitmq.username" -> Optional.of(username);
            case "rabbitmq.password" -> Optional.of(password);
            default -> Optional.empty();
        };
    }

    private String username(ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.USERNAME, "POSTGRES_USER", POSTGRES_SERVICE);
    }

    private String password(ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.PASSWORD, "POSTGRES_PASSWORD", POSTGRES_SERVICE);
    }

    private String database(ComposeService service) {
        return service.labelOrEnvironment(ComposeLabels.DATABASE, "POSTGRES_DB", username(service));
    }

    private static String datasourceExpressionOf(String datasource, String property) {
        return DATASOURCES_PREFIX + datasource + "." + property;
    }

    private record DatasourceProperty(String datasource, String property) {
        private static final Set<String> SUPPORTED_PROPERTIES = Set.of("url", "username", "password", "driver-class-name");

        static DatasourceProperty parse(String propertyName) {
            String remainder = propertyName.substring(DATASOURCES_PREFIX.length());
            int separator = remainder.indexOf('.');
            if (separator < 1 || separator == remainder.length() - 1) {
                return null;
            }
            return new DatasourceProperty(remainder.substring(0, separator), remainder.substring(separator + 1));
        }

        boolean supported() {
            return SUPPORTED_PROPERTIES.contains(property);
        }
    }
}
