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

import io.micronaut.testresources.core.Scope;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

interface ComposeEnvironmentManager {
    Optional<ComposeEndpoint> endpoint(ComposeConfiguration configuration,
                                       ComposeProject project,
                                       ComposeService service,
                                       int port,
                                       Map<String, Object> properties);
}

final class ComposeContainerManager implements ComposeEnvironmentManager {
    private static final Map<Key, ManagedEnvironment> ENVIRONMENTS = new LinkedHashMap<>();
    private final List<ComposeTestResourcesProvider> providers;

    ComposeContainerManager() {
        this(ServiceLoader.load(ComposeTestResourcesProvider.class).stream().map(ServiceLoader.Provider::get).toList());
    }

    ComposeContainerManager(List<ComposeTestResourcesProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public Optional<ComposeEndpoint> endpoint(ComposeConfiguration configuration,
                                              ComposeProject project,
                                              ComposeService service,
                                              int port,
                                              Map<String, Object> properties) {
        if (!configuration.usable()) {
            return Optional.empty();
        }
        Scope scope = Scope.from(properties);
        Key key = new Key(scope, configuration.files(), configuration.profiles(), configuration.localCompose(), configuration.dockerImageName(), configuration.projectName());
        synchronized (ENVIRONMENTS) {
            ManagedEnvironment environment = ENVIRONMENTS.computeIfAbsent(key, ignored -> start(configuration, project));
            return Optional.of(new ComposeEndpoint(
                environment.container.getServiceHost(service.instanceName(), port),
                environment.container.getServicePort(service.instanceName(), port)
            ));
        }
    }

    static boolean closeAll() {
        synchronized (ENVIRONMENTS) {
            boolean closed = !ENVIRONMENTS.isEmpty();
            ENVIRONMENTS.values().forEach(ManagedEnvironment::close);
            ENVIRONMENTS.clear();
            return closed;
        }
    }

    static boolean closeScope(String id) {
        Scope scope = Scope.of(id);
        synchronized (ENVIRONMENTS) {
            boolean closed = false;
            var iterator = ENVIRONMENTS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Key, ManagedEnvironment> entry = iterator.next();
                if (scope.includes(entry.getKey().scope)) {
                    entry.getValue().close();
                    iterator.remove();
                    closed = true;
                }
            }
            return closed;
        }
    }

    private ManagedEnvironment start(ComposeConfiguration configuration, ComposeProject project) {
        ComposeContainer container = createContainer(configuration);
        if (!configuration.profiles().isEmpty()) {
            container.withEnv("COMPOSE_PROFILES", String.join(",", configuration.profiles()));
        }
        container.withStartupTimeout(configuration.startupTimeout());
        for (ComposeService service : project.services()) {
            if (service.ignored() || !service.activeFor(configuration.profiles())) {
                continue;
            }
            for (Integer port : exposedPorts(service)) {
                container.withExposedService(service.instanceName(), port, Wait.forListeningPort().withStartupTimeout(configuration.startupTimeout()));
            }
        }
        container.start();
        return new ManagedEnvironment(container);
    }

    private ComposeContainer createContainer(ComposeConfiguration configuration) {
        List<File> files = configuration.files().stream()
            .map(Path::toFile)
            .toList();
        if (configuration.localCompose()) {
            return new ComposeContainer(configuration.projectName(), files);
        }
        return new ComposeContainer(DockerImageName.parse(configuration.dockerImageName()), configuration.projectName(), files);
    }

    List<Integer> exposedPorts(ComposeService service) {
        return providers.stream()
            .filter(provider -> provider.matches(service))
            .flatMap(provider -> provider.getPorts().stream())
            .distinct()
            .toList();
    }

    private record Key(Scope scope, List<Path> files, List<String> profiles, boolean localCompose, String image, String projectName) {
    }

    private record ManagedEnvironment(ComposeContainer container) {
        void close() {
            container.stop();
        }
    }
}
