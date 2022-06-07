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
package io.micronaut.testresources.testcontainers;

import java.util.Map;
import java.util.Set;

final class TestContainerMetadata {
    private final String id;
    private final String imageName;
    private final Map<String, Integer> exposedPorts;
    private final Set<String> hostNames;

    private final Map<String, String> rwFsBinds;
    private final Map<String, String> roFsBinds;

    TestContainerMetadata(String id,
                          String imageName,
                          Map<String, Integer> exposedPorts,
                          Set<String> hostNames,
                          Map<String, String> rwFsBinds,
                          Map<String, String> roFsBinds) {
        this.id = id;
        this.imageName = imageName;
        this.exposedPorts = exposedPorts;
        this.hostNames = hostNames;
        this.rwFsBinds = rwFsBinds;
        this.roFsBinds = roFsBinds;
    }

    public String getId() {
        return id;
    }

    public String getImageName() {
        return imageName;
    }

    public Map<String, Integer> getExposedPorts() {
        return exposedPorts;
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    public Map<String, String> getRwFsBinds() {
        return rwFsBinds;
    }

    public Map<String, String> getRoFsBinds() {
        return roFsBinds;
    }
}
