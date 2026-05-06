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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts, inspects and stops Compose projects owned by Test Resources.
 */
@Internal
final class ComposeProjectManager implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ComposeProjectManager.class);
    private static final List<String> PS_JSON_COMMAND = List.of("ps", "--format", "json");
    private static final List<String> CONFIG_JSON_COMMAND = List.of("config", "--format", "json");

    private final ComposeCli composeCli;
    private final ComposeProjectParser parser;
    private final Map<ComposeProjectKey, ComposeProject> projects = new ConcurrentHashMap<>();

    ComposeProjectManager() {
        this(new ComposeCli.Default(), new ComposeProjectParser());
    }

    ComposeProjectManager(ComposeCli composeCli, ComposeProjectParser parser) {
        this.composeCli = composeCli;
        this.parser = parser;
    }

    ComposeProject getOrCreate(ComposeConfiguration configuration) {
        ComposeProjectKey key = ComposeProjectKey.from(configuration);
        return projects.computeIfAbsent(key, ignored -> inspectOrStart(configuration, key));
    }

    private ComposeProject inspectOrStart(ComposeConfiguration configuration, ComposeProjectKey key) {
        Set<String> runningBeforeStart = runningServices(configuration);
        boolean started = false;
        if (configuration.start()) {
            ComposeCommandResult result = composeCli.run(configuration, List.of("up", "-d", "--wait"));
            if (!result.successful()) {
                if (unsupportedWait(result)) {
                    LOG.warn("Docker Compose does not support 'up --wait'; falling back to 'up -d'. Readiness checks are best effort.");
                    result = composeCli.run(configuration, List.of("up", "-d"));
                }
                if (!result.successful()) {
                    throw new ComposeCliException("Docker Compose failed to start services: " + result.diagnostic());
                }
            }
            started = true;
        }
        ComposeCommandResult config = composeCli.run(configuration, CONFIG_JSON_COMMAND);
        if (!config.successful()) {
            throw new ComposeCliException("Docker Compose failed to render configuration: " + config.diagnostic());
        }
        ComposeCommandResult ps = composeCli.run(configuration, PS_JSON_COMMAND);
        if (!ps.successful()) {
            throw new ComposeCliException("Docker Compose failed to inspect services: " + ps.diagnostic());
        }
        ComposeProject project = new ComposeProject(key, parser.parse(config.output(), ps.output(), runningBeforeStart), started, configuration.stopManaged());
        for (ComposeService service : project.services()) {
            if (service.ignored()) {
                LOG.info("Ignoring Docker Compose service {}", service.name());
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Discovered Docker Compose service {}", service.redactedSummary());
            }
        }
        return project;
    }

    private Set<String> runningServices(ComposeConfiguration configuration) {
        ComposeCommandResult result = composeCli.run(configuration, PS_JSON_COMMAND);
        if (!result.successful()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Docker Compose project has no inspectable running services before startup: {}", result.diagnostic());
            }
            return Set.of();
        }
        return parser.runningServices(result.output());
    }

    private boolean unsupportedWait(ComposeCommandResult result) {
        String diagnostic = result.diagnostic().toLowerCase();
        return diagnostic.contains("unknown flag")
            || diagnostic.contains("unknown shorthand flag")
            || diagnostic.contains("no such option")
            || diagnostic.contains("unknown option");
    }

    @Override
    public void close() throws IOException {
        Map<ComposeConfiguration, List<String>> servicesToStop = new HashMap<>();
        for (ComposeProject project : projects.values()) {
            if (project.startedByTestResources() && project.stopManaged()) {
                List<String> managedServices = project.services()
                    .stream()
                    .filter(service -> !service.externallyManaged())
                    .map(ComposeService::name)
                    .toList();
                if (!managedServices.isEmpty()) {
                    servicesToStop.put(configurationFrom(project.key()), managedServices);
                }
            }
        }
        for (Map.Entry<ComposeConfiguration, List<String>> entry : servicesToStop.entrySet()) {
            List<String> arguments = new ArrayList<>();
            arguments.add("stop");
            arguments.addAll(entry.getValue());
            ComposeCommandResult result = composeCli.run(entry.getKey(), arguments);
            if (!result.successful() && LOG.isWarnEnabled()) {
                LOG.warn("Docker Compose failed to stop managed services {}: {}", entry.getValue(), result.diagnostic());
            }
        }
    }

    private ComposeConfiguration configurationFrom(ComposeProjectKey key) {
        return new ComposeConfiguration(
            true,
            key.workingDirectory(),
            key.files(),
            key.profiles(),
            key.projectName(),
            false,
            true,
            java.time.Duration.ofSeconds(60)
        );
    }
}
