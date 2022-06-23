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
    private final String imageTag;
    private final Map<String, Integer> exposedPorts;
    private final Set<String> hostNames;

    private final Map<String, String> rwFsBinds;
    private final Map<String, String> roFsBinds;
    private final List<String> command;
    private final String workingDirectory;
    private final Map<String, String> env;
    private final Map<String, String> labels;
    private final Duration startupTimeout;
    private final List<CopyFileToContainer> fileCopies;
    private final Long memory;
    private final Long swapMemory;
    private final Long sharedMemory;
    private final String network;
    private final Set<String> networkAliases;

    @SuppressWarnings("checkstyle:ParameterNumber")
    TestContainerMetadata(String id,
                          String imageName,
                          String imageTag,
                          Map<String, Integer> exposedPorts,
                          Set<String> hostNames,
                          Map<String, String> rwFsBinds,
                          Map<String, String> roFsBinds,
                          List<String> command,
                          String workingDirectory,
                          Map<String, String> env,
                          Map<String, String> labels,
                          Duration startupTimeout,
                          List<CopyFileToContainer> fileCopies,
                          Long memory,
                          Long swapMemory,
                          Long sharedMemory, String network, Set<String> networkAliases) {
        this.id = id;
        this.imageName = imageName;
        this.imageTag = imageTag;
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
        this.memory = memory;
        this.swapMemory = swapMemory;
        this.sharedMemory = sharedMemory;
        this.network = network;
        this.networkAliases = networkAliases;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getImageName() {
        return Optional.ofNullable(imageName);
    }

    public Optional<String> getImageTag() {
        return Optional.ofNullable(imageTag);
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

    public List<String> getCommand() {
        return command;
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

    public Optional<Long> getMemory() {
        return Optional.ofNullable(memory);
    }

    public Optional<Long> getSwapMemory() {
        return Optional.ofNullable(swapMemory);
    }

    public Optional<Long> getSharedMemory() {
        return Optional.ofNullable(sharedMemory);
    }

    public Optional<String> getNetwork() {
        return Optional.ofNullable(network);
    }

    public Set<String> getNetworkAliases() {
        return networkAliases;
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
