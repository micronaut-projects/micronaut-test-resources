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

import io.micronaut.testresources.core.compose.ComposeAwareTestResourcesResolver;
import io.micronaut.testresources.core.compose.ComposeResolverSupport;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves Solr properties from Docker Compose services.
 */
public final class SolrComposeTestResourceProvider extends SolrTestResourceProvider implements ComposeAwareTestResourcesResolver {
    private static final ComposeResolverSupport.ServiceDescriptor SOLR =
        new ComposeResolverSupport.ServiceDescriptor("solr", List.of(), 8983);

    @Override
    public String getDisplayName() {
        return "Docker Compose Solr";
    }

    @Override
    protected Optional<String> resolveWithoutContainer(String propertyName,
                                                       Map<String, Object> properties,
                                                       Map<String, Object> testResourcesConfig) {
        Optional<String> solr = ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            SOLR,
            context -> true,
            context -> MICRONAUT_SOLR_ENDPOINT.equals(propertyName) ? context.http() + "/solr" : null);
        if (solr.isPresent() || !MICRONAUT_ZOOKEEPER_HOSTS.equals(propertyName)) {
            return solr;
        }
        return ComposeResolverSupport.resolve(
            propertyName,
            properties,
            testResourcesConfig,
            new ComposeResolverSupport.ServiceDescriptor("solr-zookeeper", List.of("zookeeper", "zk"), 9983),
            context -> true,
            ComposeResolverSupport.ResolutionContext::hostPort);
    }
}
