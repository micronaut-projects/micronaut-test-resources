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
package io.micronaut.testresources.server;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * A filter used to restrict access to the server: the server
 * should only be available from a loopback connection, and
 * if an access token is specified, then the request should
 * include the access token header.
 *
 * This is to avoid services abusing the server to spawn
 * containers: when the server is started, a random access key
 * should be generated, and passed to the allowed clients.
 */
@Filter("/**")
public class AccessFilter implements HttpServerFilter {
    private final AccessConfiguration accessConfiguration;

    public AccessFilter(AccessConfiguration accessConfiguration) {
        this.accessConfiguration = accessConfiguration;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (request.getRemoteAddress().getAddress().isLoopbackAddress()) {
            String serverToken = accessConfiguration.getAccessToken();
            if (serverToken != null) {
                String clientToken = request.getHeaders().get(AccessConfiguration.ACCESS_TOKEN);
                if (!serverToken.equals(clientToken)) {
                    return Flux.just(HttpResponse.unauthorized());
                }
            }
            return chain.proceed(request);
        }
            return Flux.just(HttpResponse.unauthorized());
    }
}
