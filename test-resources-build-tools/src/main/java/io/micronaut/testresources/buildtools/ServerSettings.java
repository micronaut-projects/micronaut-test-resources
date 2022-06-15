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
package io.micronaut.testresources.buildtools;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the server settings, once started.
 */
public final class ServerSettings {
    private final int port;
    private final String accessToken;
    private final Integer clientTimeout;

    public ServerSettings(int port, String accessToken, Integer clientTimeout) {
        this.port = port;
        this.accessToken = accessToken;
        this.clientTimeout = clientTimeout;
    }

    public int getPort() {
        return port;
    }

    public Optional<String> getAccessToken() {
        return Optional.ofNullable(accessToken);
    }

    public Optional<Integer> getClientTimeout() {
        return Optional.ofNullable(clientTimeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServerSettings that = (ServerSettings) o;

        if (port != that.port) {
            return false;
        }
        if (!Objects.equals(accessToken, that.accessToken)) {
            return false;
        }
        return Objects.equals(clientTimeout, that.clientTimeout);
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + (accessToken != null ? accessToken.hashCode() : 0);
        result = 31 * result + (clientTimeout != null ? clientTimeout.hashCode() : 0);
        return result;
    }
}
