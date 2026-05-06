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
package io.micronaut.testresources.compose;

import io.micronaut.core.annotation.Internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Normalized Docker Compose service metadata.
 *
 * @param name The Compose service name.
 * @param image The configured image name.
 * @param labels The service labels.
 * @param environment The service environment.
 * @param ports The published service ports.
 * @param externallyManaged Whether the service was already running before Test Resources started.
 */
@Internal
record ComposeService(
    String name,
    String image,
    Map<String, String> labels,
    Map<String, String> environment,
    List<ComposePort> ports,
    boolean externallyManaged
) {
    boolean ignored() {
        return Boolean.parseBoolean(labels.getOrDefault(ComposeLabels.IGNORE, "false"));
    }

    Optional<String> serviceLabel() {
        return Optional.ofNullable(labels.get(ComposeLabels.SERVICE))
            .map(s -> s.toLowerCase(Locale.ROOT));
    }

    Optional<ComposePort> publishedPort(int targetPort) {
        return ports.stream()
            .filter(port -> port.targetPort() == targetPort)
            .findFirst();
    }

    String environment(String name, String defaultValue) {
        return environment.getOrDefault(name, defaultValue);
    }

    String labelOrEnvironment(String label, String environmentName, String defaultValue) {
        return labels.getOrDefault(label, environment(environmentName, defaultValue));
    }

    String redactedSummary() {
        return "service=" + name
            + ", image=" + image
            + ", labels=" + SecretRedactor.redact(labels)
            + ", environment=" + SecretRedactor.redact(environment)
            + ", ports=" + ports
            + ", externallyManaged=" + externallyManaged;
    }
}

@Internal
record ComposePort(String host, int publishedPort, int targetPort) {
}
