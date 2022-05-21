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
package io.micronaut.testresources.client;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.uri.UriBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

/**
 * A simple implementation of the test resources proxy client.
 */
@SuppressWarnings("unchecked")
public class DefaultTestResourcesClient implements TestResourcesClient {

    private final BlockingHttpClient client;
    private final URI resolvablePropertiesURI;
    private final URI requiredPropertiesURI;
    private final URI closeAllURI;
    private final URI resolveURI;

    public DefaultTestResourcesClient(HttpClient client) {
        this.client = client.toBlocking();
        this.resolvablePropertiesURI = UriBuilder.of("/proxy").path("/list").build();
        this.requiredPropertiesURI = UriBuilder.of("/proxy").path("/requirements").build();
        this.closeAllURI = UriBuilder.of("/proxy").path("/close/all").build();
        this.resolveURI = UriBuilder.of("/proxy").path("/resolve").build();
    }

    @Override
    public List<String> getResolvableProperties() {
        return doGet(resolvablePropertiesURI, List.class);
    }

    @Override
    public Optional<String> resolve(String name, Map<String, Object> properties) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("properties", properties);
        HttpRequest<?> req = HttpRequest.POST(resolveURI, params)
            .header(USER_AGENT, "Micronaut HTTP Client")
            .header(ACCEPT, "application/json");
        return Optional.ofNullable(client.retrieve(req));
    }

    @Override
    public List<String> getRequiredProperties() {
        return doGet(requiredPropertiesURI, List.class);
    }

    @Override
    public void closeAll() {
        doGet(closeAllURI, String.class);
    }

    private <T> T doGet(URI uri, Class<T> clazz) {
        HttpRequest<?> req = HttpRequest.GET(uri)
            .header(USER_AGENT, "Micronaut HTTP Client")
            .header(ACCEPT, "application/json");
        return client.retrieve(req, clazz);
    }
}
