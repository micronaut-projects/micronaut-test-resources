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
package io.micronaut.testresources.controlpanel;

import io.micronaut.controlpanel.core.AbstractControlPanel;
import io.micronaut.controlpanel.core.config.ControlPanelConfiguration;
import io.micronaut.testresources.testcontainers.DockerSupport;
import io.micronaut.testresources.testcontainers.TestContainers;
import jakarta.inject.Singleton;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

/**
 * A control panel which is responsible for showing the state of Docker images
 * managed by the test resources service.
 */
@Singleton
public class DockerHealthControlPanel extends AbstractControlPanel<DockerHealth> {
    private static final String NAME = "docker";

    protected DockerHealthControlPanel() {
        super(NAME, createConfiguration());
    }

    private static ControlPanelConfiguration createConfiguration() {
        var controlPanelConfiguration = new ControlPanelConfiguration(NAME);
        controlPanelConfiguration.setTitle("Docker");
        controlPanelConfiguration.setIcon("fa-flask-vial");
        controlPanelConfiguration.setOrder(-1);
        return controlPanelConfiguration;
    }

    @Override
    public String getBadge() {
        var body = getBody();
        return String.valueOf(body.runningContainers());
    }

    @Override
    public DockerHealth getBody() {
        var dockerAvailable = DockerSupport.isDockerAvailable();
        if (dockerAvailable) {
            var factory = DockerClientFactory.instance();
            try {
                int runningContainers = TestContainers.listAll()
                    .values()
                    .stream()
                    .map(List::size)
                    .reduce(0, Integer::sum);
                List<TestResourcesContainer> containers = TestContainers.listAll()
                    .entrySet()
                    .stream()
                    .flatMap(entry -> {
                        var scope = entry.getKey();
                        var values = entry.getValue();
                        return values.stream().map(c -> new TestResourcesContainer(
                            scope.toString(),
                            c.getContainerId(),
                            c.getContainerName(),
                            networkOf(c),
                            c.getDockerImageName()
                        ));
                    })
                    .toList();
                var info = factory.getInfo();
                return new DockerHealth(Status.AVAILABLE, info, runningContainers, containers);
            } catch (Exception ex) {
            }
        }
        return new DockerHealth(Status.UNAVAILABLE,  null, 0, List.of());
    }

    private static String networkOf(GenericContainer<?> c) {
        var network = c.getNetwork();
        return network == null ? "" : network.getId();
    }
}
