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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A simple implementation of the test resources client.
 */
@SuppressWarnings("unchecked")
@Internal
public class DefaultTestResourcesClient implements TestResourcesClient {
    public static final String ACCESS_TOKEN = "Access-Token";

    private static final String RESOLVABLE_PROPERTIES_URI = "/list";
    private static final String REQUIRED_PROPERTIES_URI = "/requirements/expr";
    private static final String REQUIRED_PROPERTY_ENTRIES_URI = "/requirements/entries";
    private static final String CLOSE_ALL_URI = "/close/all";
    private static final String CLOSE_URI = "/close";
    private static final String RESOLVE_URI = "/resolve";
    private static final Argument<List<String>> LIST_OF_STRING = Argument.LIST_OF_STRING;
    private static final Argument<String> STRING = Argument.STRING;
    private static final Argument<Boolean> BOOLEAN = Argument.BOOLEAN;

    private final JsonMapper jsonMapper;
    private final String baseUri;
    private final HttpClient client;

    private final String accessToken;
    private final Duration clientTimeout;

    public DefaultTestResourcesClient(String baseUri, String accessToken, int clientReadTimeout) {
        this.baseUri = baseUri;
        clientTimeout = Duration.ofSeconds(clientReadTimeout);
        this.client = HttpClient.newBuilder()
            .build();
        this.accessToken = accessToken;
        this.jsonMapper = JsonMapper.createDefault();
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("propertyEntries", propertyEntries);
        properties.put("testResourcesConfig", testResourcesConfig);
        return request(RESOLVABLE_PROPERTIES_URI, LIST_OF_STRING,
            r -> POST(r, properties)
        );
    }

    @Override
    public Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("properties", properties);
        params.put("testResourcesConfig", testResourcesConfiguration);
        return Optional.ofNullable(request(RESOLVE_URI, STRING, r -> POST(r, params)));
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return request(REQUIRED_PROPERTIES_URI + "/" + expression, LIST_OF_STRING, this::GET);
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return request(REQUIRED_PROPERTY_ENTRIES_URI, LIST_OF_STRING, this::GET);
    }

    @Override
    public boolean closeAll() {
        return request(CLOSE_ALL_URI, BOOLEAN, this::GET);
    }

    @Override
    public boolean closeScope(@Nullable String id) {
        return request(CLOSE_URI + "/" + id, BOOLEAN, this::GET);
    }

    @SuppressWarnings({"java:S100", "checkstyle:MethodName"})
    private void POST(HttpRequest.Builder request, Object o) {
        request.POST(HttpRequest.BodyPublishers.ofByteArray(writeValueAsBytes(o)));
    }

    @SuppressWarnings({"java:S100", "checkstyle:MethodName"})
    private void GET(HttpRequest.Builder request) {
        request.GET();
    }

    private <T> T request(String path, Argument<T> type, Consumer<? super HttpRequest.Builder> config) {
        var request = HttpRequest.newBuilder()
            .uri(uri(path))
            .timeout(clientTimeout);
        request = request.header("User-Agent", "Micronaut Test Resources Client")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json");
        if (accessToken != null) {
            request = request.header(ACCESS_TOKEN, accessToken);
        }
        config.accept(request);
        try {
            var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            var body = response.body();
            if (response.statusCode() == 200) {
                return jsonMapper.readValue(body, type);
            }
            throw new TestResourcesException("Test resources service failed with this error: " + body);
        } catch (IOException e) {
            throw new TestResourcesException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestResourcesException(e);
        }
    }

    private URI uri(String path) {
        try {
            return new URI(baseUri + path);
        } catch (URISyntaxException e) {
            throw new TestResourcesException(e);
        }
    }

    private byte[] writeValueAsBytes(Object o) {
        try {
            return jsonMapper.writeValueAsBytes(o);
        } catch (IOException e) {
            throw new TestResourcesException(e);
        }
    }
}
