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

import com.github.dockerjava.api.model.Info;
import io.micronaut.core.annotation.Introspected;

import java.util.List;

/**
 * Model for Docker health.
 * @param dockerStatus the docker service status
 * @param info docker service metadata
 * @param runningContainers the number of containers started by test resources
 * @param managedContainers the containers managed by test resources
 * @param startingContainers the list of containers being started
 * @param pullingContainers the list of containers being pulled
 */
@Introspected
public record DockerHealth(
    Status dockerStatus,
    Info info,
    int runningContainers,
    List<TestResourcesContainer> managedContainers,
    List<String> startingContainers,
    List<String> pullingContainers) {

    /**
     * Returns the number of containers which are not yet ready.
     * @return the container count
     */
    public int getInProgress() {
        return startingContainers.size() + pullingContainers.size();
    }
}
