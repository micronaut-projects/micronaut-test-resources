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
package io.micronaut.testresources.elasticsearch;

import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider which will spawn an ElasticSearch test container.
 */
public class ElasticsearchTestResourceProvider extends AbstractTestContainersProvider<ElasticsearchContainer> {

    public static final String ELASTICSEARCH_HOSTS = "elasticsearch.http-hosts";
    public static final String SIMPLE_NAME = "elasticsearch";
    public static final String DEFAULT_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch";
    public static final String DEFAULT_TAG = "8.4.3";

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return Collections.singletonList(ELASTICSEARCH_HOSTS);
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
    protected ElasticsearchContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        if ("latest".equals(imageName.getVersionPart())) {
            // ElasticSearch does't provide a latest tag, so we use a hardcoded version
            imageName = imageName.withTag(DEFAULT_TAG);
        }
        ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(imageName);
        elasticsearchContainer.withEnv("xpack.security.enabled", "false");
        return elasticsearchContainer;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, ElasticsearchContainer container) {
        if (ELASTICSEARCH_HOSTS.equals(propertyName)) {
            return Optional.of("http://" + container.getHttpHostAddress());
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfiguration) {
        return ELASTICSEARCH_HOSTS.equals(propertyName);
    }
}
