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
package io.micronaut.testresources.solr;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves Solr properties from Docker Compose services.
 */
public final class SolrComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public SolrComposeTestResourcesProvider() {
        super("solr", Set.of(), 8983, List.of("micronaut.solr.hosts", "solr.config.url", "solr.schema.url"));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return switch (context.propertyName()) {
            case "micronaut.solr.hosts" -> context.http() + "/solr";
            case "solr.config.url", "solr.schema.url" -> context.properties().get(context.propertyName()) == null ? null : String.valueOf(context.properties().get(context.propertyName()));
            default -> null;
        };
    }
}
