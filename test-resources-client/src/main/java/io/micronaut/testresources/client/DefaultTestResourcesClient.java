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
import io.micronaut.core.type.Argument;
import io.micronaut.http.codec.CodecException;
import io.micronaut.testresources.codec.TestResourcesCodec;
import io.micronaut.testresources.codec.TestResourcesMediaType;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple implementation of the test resources client.
 */
@SuppressWarnings("unchecked")
@Internal
public final class DefaultTestResourcesClient implements TestResourcesClient {
    public static final String ACCESS_TOKEN = "Access-Token";
    private static final Pattern JSON_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
    private static final String INTERNAL_SERVER_ERROR_PREFIX = "Internal Server Error: ";
    private static final String MESSAGE_KEY = "message";
    private static final String SERVER_FAILED_WITHOUT_ERROR_BODY = "Server failed without an error body";

    private static final String RESOLVABLE_PROPERTIES_URI = "/list";
    private static final String REQUIRED_PROPERTIES_URI = "/requirements/expr";
    private static final String REQUIRED_PROPERTY_ENTRIES_URI = "/requirements/entries";
    private static final String CLOSE_ALL_URI = "/close/all";
    private static final String CLOSE_URI = "/close";
    private static final String RESOLVE_URI = "/resolve";
    private static final Argument<List<String>> LIST_OF_STRING = Argument.LIST_OF_STRING;
    private static final Argument<String> STRING = Argument.STRING;
    private static final Argument<Boolean> BOOLEAN = Argument.BOOLEAN;

    private final String baseUri;
    private final HttpClient client;

    private final @Nullable String accessToken;
    private final Duration clientTimeout;
    private final IntellijIdeaDatasourceExporter intellijIdeaDatasourceExporter;

    public DefaultTestResourcesClient(String baseUri, @Nullable String accessToken, int clientReadTimeout) {
        this(baseUri, accessToken, clientReadTimeout, new IntellijIdeaDatasourceExporter());
    }

    DefaultTestResourcesClient(String baseUri,
                               @Nullable String accessToken,
                               int clientReadTimeout,
                               IntellijIdeaDatasourceExporter intellijIdeaDatasourceExporter) {
        this.baseUri = baseUri;
        clientTimeout = Duration.ofSeconds(clientReadTimeout);
        this.client = HttpClient.newBuilder()
            .connectTimeout(clientTimeout)
            .build();
        this.accessToken = accessToken;
        this.intellijIdeaDatasourceExporter = intellijIdeaDatasourceExporter;
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries,
                                                Map<String, Object> testResourcesConfig) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("propertyEntries", propertyEntries);
        properties.put("testResourcesConfig", testResourcesConfig);
        return Objects.requireNonNull(request(RESOLVABLE_PROPERTIES_URI, LIST_OF_STRING,
            r -> POST(r, properties)
        ));
    }

    @Override
    public Optional<String> resolve(String name, Map<String, Object> properties,
                                    Map<String, Object> testResourcesConfig) {
        return resolve(name, properties, testResourcesConfig, "default");
    }

    Optional<String> resolve(String name,
                             Map<String, Object> properties,
                             Map<String, Object> testResourcesConfig,
                             String exporterSessionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("properties", properties);
        params.put("testResourcesConfig", testResourcesConfig);
        Optional<String> resolved = Optional.ofNullable(request(RESOLVE_URI, STRING, r -> POST(r, params)));
        resolved.ifPresent(value -> intellijIdeaDatasourceExporter.export(name, value, testResourcesConfig, exporterSessionId));
        return resolved;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return Objects.requireNonNull(request(REQUIRED_PROPERTIES_URI + "/" + expression, LIST_OF_STRING, this::GET));
    }

    @Override
    public List<String> getRequiredPropertyEntries() {
        return Objects.requireNonNull(request(REQUIRED_PROPERTY_ENTRIES_URI, LIST_OF_STRING, this::GET));
    }

    @Override
    public boolean closeAll() {
        return Objects.requireNonNull(request(CLOSE_ALL_URI, BOOLEAN, this::GET));
    }

    @Override
    public boolean closeScope(@Nullable String id) {
        return Objects.requireNonNull(request(CLOSE_URI + "/" + id, BOOLEAN, this::GET));
    }

    void clearIntellijIdeaDatasourceExport(String exporterSessionId) {
        intellijIdeaDatasourceExporter.clearSession(exporterSessionId);
    }

    @SuppressWarnings({"java:S100", "checkstyle:MethodName"})
    private void POST(HttpRequest.Builder request, Object o) {
        request.POST(HttpRequest.BodyPublishers.ofByteArray(writeValueAsBytes(o)));
    }

    @SuppressWarnings({"java:S100", "checkstyle:MethodName"})
    private void GET(HttpRequest.Builder request) {
        request.GET();
    }

    private <T> @Nullable T request(String path, Argument<T> type,
                                    Consumer<? super HttpRequest.Builder> config) {
        var request = HttpRequest.newBuilder()
            .uri(uri(path))
            .timeout(clientTimeout);
        request = request.header("User-Agent", "Micronaut Test Resources Client")
            .header("Content-Type", TestResourcesMediaType.TEST_RESOURCES_BINARY)
            .header("Accept", TestResourcesMediaType.TEST_RESOURCES_BINARY);
        if (accessToken != null) {
            request = request.header(ACCESS_TOKEN, accessToken);
        }
        config.accept(request);
        try {
            var response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            var body = response.body();
            if (response.statusCode() == 200) {
                return decodeResponse(body, type);
            } else if (response.statusCode() == 500) {
                return handleError(readErrorValue(body));
            } else if (response.statusCode() == 404) {
                return null;
            }
            throw new TestResourcesException(
                "Unexpected response code: " + response.statusCode());
        } catch (ConnectException e) {
            throw new TestResourcesException("Test resource service is not available at " + baseUri, e);
        } catch (IOException e) {
            throw new TestResourcesException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TestResourcesException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> @Nullable T decodeResponse(byte[] body, Argument<T> type) throws IOException {
        @Nullable Object value = readValue(body);
        if (value == null) {
            return null;
        }
        if (STRING.equalsType(type)) {
            return (T) value;
        }
        return (T) value;
    }

    private @Nullable Object readValue(byte[] body) throws IOException {
        if (body.length == 0) {
            return null;
        }
        return TestResourcesCodec.readValue(new ByteArrayInputStream(body));
    }

    private @Nullable Object readErrorValue(byte[] body) throws IOException {
        try {
            return readValue(body);
        } catch (CodecException e) {
            return fallbackErrorBody(body);
        }
    }

    private Map<String, Object> fallbackErrorBody(byte[] body) {
        String text = new String(body, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) {
            return Map.of(MESSAGE_KEY, SERVER_FAILED_WITHOUT_ERROR_BODY);
        }
        Matcher matcher = JSON_MESSAGE.matcher(text);
        if (matcher.find()) {
            return Map.of(MESSAGE_KEY, matcher.group(1));
        }
        return Map.of(MESSAGE_KEY, text);
    }

    private <T> T handleError(@Nullable Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            throw new TestResourcesException(payload == null ? SERVER_FAILED_WITHOUT_ERROR_BODY : payload.toString());
        }
        var allErrors = new LinkedHashSet<String>();
        collectErrors(map, allErrors);
        var errorList = allErrors.stream().toList();
        if (errorList.size() == 1) {
            throw new TestResourcesException(errorList.get(0));
        } else {
            var sb = new StringBuilder();
            sb.append("Server failed with the following errors:\n");
            for (String error : errorList) {
                sb.append(" - ").append(error).append("\n");
            }
            throw new TestResourcesException(sb.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void collectErrors(Map<?, ?> model, LinkedHashSet<String> allErrors) {
        sanitizeError((String) model.get(MESSAGE_KEY)).ifPresent(allErrors::add);
        Object errors = model.get("errors");
        if (errors instanceof List<?> list) {
            for (Object error : list) {
                if (error instanceof Map<?, ?> nested) {
                    collectErrors(nested, allErrors);
                } else if (error != null) {
                    sanitizeError(error.toString()).ifPresent(allErrors::add);
                }
            }
        }
    }

    private static Optional<String> sanitizeError(@Nullable String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        if (message.startsWith(INTERNAL_SERVER_ERROR_PREFIX)) {
            return Optional.of(message.substring(INTERNAL_SERVER_ERROR_PREFIX.length()));
        }
        return Optional.of(message);
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
            var output = new ByteArrayOutputStream();
            TestResourcesCodec.writeValue(o, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new TestResourcesException(e);
        }
    }

}
