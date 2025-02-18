/*
 * Copyright 2017-2024 original authors
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
package io.micronaut.testresources.solr;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn an OpenSearch test container.
 */
public class SolrTestResourceProvider extends AbstractTestContainersProvider<SolrContainer> {

    public static final String SIMPLE_NAME = "solr";
    public static final String DEFAULT_IMAGE = "solr:9.8.0";
    public static final String DISPLAY_NAME = "Solr Search Server";
    public static final String MICRONAUT_SOLR_ENDPOINT = "micronaut.solr.hosts";
    public static final String MICRONAUT_ZOOKEEPER_HOSTS = "micronaut.solr.zk-hosts";
    public static final List<String> RESOLVABLE_PROPERTIES = List.of(
        MICRONAUT_SOLR_ENDPOINT,
        MICRONAUT_ZOOKEEPER_HOSTS
    );

    private static final String COLLECTION_NAME_PROPERTY = "solr.collection";
    private static final String CONFIG_NAME_PROPERTY = "solr.config.name";
    private static final String CONFIG_URL_PROPERTY = "solr.config.url";
    private static final String SCHEMA_URL_PROPERTY = "solr.schema.url";
    private static final String ZOOKEEPER_ENABLED_PROPERTY = "solr.zookeeper.enabled";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return List.of(
            MICRONAUT_SOLR_ENDPOINT,
            MICRONAUT_ZOOKEEPER_HOSTS,
            COLLECTION_NAME_PROPERTY,
            CONFIG_NAME_PROPERTY,
            CONFIG_URL_PROPERTY,
            SCHEMA_URL_PROPERTY,
            ZOOKEEPER_ENABLED_PROPERTY
        );
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected SolrContainer createContainer(DockerImageName imageName,
                                            Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        SolrContainer container = new SolrContainer(DockerImageName.parse(DEFAULT_IMAGE).asCompatibleSubstituteFor("solr")) {
            @Override
            protected void configure() {
                this.addExposedPorts(8983, 9983);
                this.withAccessToHost(true);

                // Apply Zookeeper configuration
                boolean zookeeperEnabled = Boolean.parseBoolean(
                    String.valueOf(requestedProperties.getOrDefault(ZOOKEEPER_ENABLED_PROPERTY, "true"))
                );
                this.withZookeeper(zookeeperEnabled);

                // Set the collection name if provided
                String collectionName = (String) requestedProperties.get(COLLECTION_NAME_PROPERTY);
                if (collectionName != null) {
                    this.withCollection(collectionName);
                }

                // Set configuration if both name and URL are provided
                String configName = (String) requestedProperties.get(CONFIG_NAME_PROPERTY);
                String configUrlStr = (String) requestedProperties.get(CONFIG_URL_PROPERTY);
                if (configName != null && configUrlStr != null) {
                    try {
                        URL configUrl = new URL(configUrlStr);
                        this.withConfiguration(configName, configUrl);
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid configuration URL: " + configUrlStr, e);
                    }
                }

                // Set schema if URL is provided
                String schemaUrlStr = (String) requestedProperties.get(SCHEMA_URL_PROPERTY);
                if (schemaUrlStr != null) {
                    try {
                        URL schemaUrl = new URL(schemaUrlStr);
                        this.withSchema(schemaUrl);
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid schema URL: " + schemaUrlStr, e);
                    }
                }

                String command = zookeeperEnabled ? "solr -c -f" : "solr -f";
                this.setCommand(command);
                this.waitStrategy =
                    new LogMessageWaitStrategy()
                        .withRegEx(".*Server Started.*")
                        .withStartupTimeout(Duration.of(180, ChronoUnit.SECONDS));
            }
        };

        return container;
    }


    @Override
    protected Optional<String> resolveProperty(String propertyName, SolrContainer container) {
        if (MICRONAUT_SOLR_ENDPOINT.equals(propertyName)) {
            return Optional.of("http://" + container.getHost() + ":" + container.getSolrPort() + "/solr");
        } else if (MICRONAUT_ZOOKEEPER_HOSTS.equals(propertyName)) {
            return Optional.of(container.getHost() + ":" + container.getZookeeperPort());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return RESOLVABLE_PROPERTIES.contains(propertyName);
    }


}
