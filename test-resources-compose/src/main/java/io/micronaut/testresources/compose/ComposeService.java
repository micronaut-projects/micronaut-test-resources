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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

record ComposeProject(List<ComposeService> services) {
}

record ComposeService(
    String name,
    String image,
    Map<String, String> labels,
    Map<String, String> environment,
    List<Integer> ports,
    List<String> profiles
) {
    String instanceName() {
        return name + "-1";
    }

    boolean ignored() {
        return Boolean.parseBoolean(labels.getOrDefault(ComposeLabels.IGNORE, "false"));
    }

    boolean activeFor(List<String> activeProfiles) {
        return profiles.isEmpty() || profiles.stream().anyMatch(activeProfiles::contains);
    }

    Optional<String> serviceLabel() {
        return Optional.ofNullable(labels.get(ComposeLabels.SERVICE))
            .map(ComposeService::normalize);
    }

    boolean explicitService(Set<String> names) {
        return serviceLabel().filter(names::contains).isPresent();
    }

    boolean imageContains(String token) {
        return normalize(image).contains(token);
    }

    boolean exposes(int port) {
        return ports.contains(port);
    }

    String labelOrEnvironment(String label, String environmentName, String defaultValue) {
        return labels.getOrDefault(label, environment.getOrDefault(environmentName, defaultValue));
    }

    String redactedSummary() {
        return "service=" + name
            + ", image=" + image
            + ", labels=" + SecretRedactor.redact(labels)
            + ", environment=" + SecretRedactor.redact(environment)
            + ", ports=" + ports
            + ", profiles=" + profiles;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}

record ComposeEndpoint(String host, int port) {
    String hostPort() {
        return host + ":" + port;
    }
}
