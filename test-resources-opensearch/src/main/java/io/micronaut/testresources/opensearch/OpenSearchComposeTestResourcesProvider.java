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
package io.micronaut.testresources.opensearch;

import io.micronaut.testresources.compose.AbstractComposeTestResourcesProvider;
import io.micronaut.testresources.compose.ComposeTestResourcesProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Resolves OpenSearch properties from Docker Compose services.
 */
public final class OpenSearchComposeTestResourcesProvider extends AbstractComposeTestResourcesProvider {
    public OpenSearchComposeTestResourcesProvider() {
        super("opensearch", Set.of("elasticsearch"), 9200, List.of(
            "micronaut.opensearch.rest-client.http-hosts",
            "micronaut.opensearch.httpclient5.http-hosts"
        ));
    }

    @Override
    public @Nullable String resolve(ComposeTestResourcesProvider.ResolutionContext context) {
        return context.http();
    }
}
