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

import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class TestContainerMetadata {
    private final String id;
    private final String imageName;
    private final Map<String, Integer> exposedPorts;
    private final Set<String> hostNames;

    private final Map<String, String> rwFsBinds;
    private final Map<String, String> roFsBinds;
    private final String command;
    private final String workingDirectory;
    private final Map<String, String> env;
    private final Map<String, String> labels;
    private final Duration startupTimeout;
    private final List<CopyFileToContainer> fileCopies;

    TestContainerMetadata(String id,
                          String imageName,
                          Map<String, Integer> exposedPorts,
                          Set<String> hostNames,
                          Map<String, String> rwFsBinds,
                          Map<String, String> roFsBinds,
                          String command,
                          String workingDirectory,
                          Map<String, String> env,
                          Map<String, String> labels,
                          Duration startupTimeout,
                          List<CopyFileToContainer> fileCopies) {
        this.id = id;
        this.imageName = imageName;
        this.exposedPorts = exposedPorts;
        this.hostNames = hostNames;
        this.rwFsBinds = rwFsBinds;
        this.roFsBinds = roFsBinds;
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.env = env;
        this.labels = labels;
        this.startupTimeout = startupTimeout;
        this.fileCopies = fileCopies;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getImageName() {
        return Optional.ofNullable(imageName);
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

    public Optional<String> getCommand() {
        return Optional.ofNullable(command);
    }

    public Optional<String> getWorkingDirectory() {
        return Optional.ofNullable(workingDirectory);
    }

    public Optional<Duration> getStartupTimeout() {
        return Optional.ofNullable(startupTimeout);
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public List<CopyFileToContainer> getFileCopies() {
        return fileCopies;
    }

    public static final class CopyFileToContainer {
        private final MountableFile file;
        private final String destination;

        public CopyFileToContainer(MountableFile file, String destination) {
            this.file = file;
            this.destination = destination;
        }

        public MountableFile getFile() {
            return file;
        }

        public String getDestination() {
            return destination;
        }
    }
}
