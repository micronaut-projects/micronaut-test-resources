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

import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class TestContainerMetadata {
    private final String id;
    private final @Nullable String imageName;
    private final @Nullable String imageTag;
    private final Map<String, Integer> exposedPorts;
    private final Set<String> hostNames;

    private final Map<String, String> rwFsBinds;
    private final Map<String, String> roFsBinds;
    private final Set<String> rwAnonymousVolumes;
    private final Set<String> roAnonymousVolumes;
    private final Set<String> rwTmpfsMappings;
    private final Set<String> roTmpfsMappings;
    private final List<String> command;
    private final @Nullable String workingDirectory;
    private final Map<String, String> env;
    private final Map<String, String> labels;
    private final @Nullable Duration startupTimeout;
    private final List<CopyFileToContainer> fileCopies;
    private final @Nullable Long memory;
    private final @Nullable Long swapMemory;
    private final @Nullable Long sharedMemory;
    private final @Nullable String network;
    private final Set<String> networkAliases;
    private final @Nullable String networkMode;
    private final @Nullable WaitStrategy waitStrategy;
    private final Set<String> dependencies;

    @SuppressWarnings("checkstyle:ParameterNumber")
    TestContainerMetadata(String id,
                          @Nullable String imageName,
                          @Nullable String imageTag,
                          Map<String, Integer> exposedPorts,
                          Set<String> hostNames,
                          Map<String, String> rwFsBinds,
                          Map<String, String> roFsBinds,
                          Set<String> rwAnonymousVolumes,
                          Set<String> roAnonymousVolumes,
                          Set<String> rwTmpfsMappings,
                          Set<String> roTmpfsMappings,
                          List<String> command,
                          @Nullable String workingDirectory,
                          Map<String, String> env,
                          Map<String, String> labels,
                          @Nullable Duration startupTimeout,
                          List<CopyFileToContainer> fileCopies,
                          @Nullable Long memory,
                          @Nullable Long swapMemory,
                          @Nullable Long sharedMemory,
                          @Nullable String network,
                          Set<String> networkAliases,
                          @Nullable String networkMode,
                          @Nullable WaitStrategy waitStrategy,
                          Set<String> dependencies) {
        this.id = id;
        this.imageName = imageName;
        this.imageTag = imageTag;
        this.exposedPorts = exposedPorts;
        this.hostNames = hostNames;
        this.rwFsBinds = rwFsBinds;
        this.roFsBinds = roFsBinds;
        this.rwAnonymousVolumes = rwAnonymousVolumes;
        this.roAnonymousVolumes = roAnonymousVolumes;
        this.rwTmpfsMappings = rwTmpfsMappings;
        this.roTmpfsMappings = roTmpfsMappings;
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
        this.networkMode = networkMode;
        this.waitStrategy = waitStrategy;
        this.dependencies = dependencies;
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

    public Set<String> getRwAnonymousVolumes() {
        return rwAnonymousVolumes;
    }

    public Set<String> getRoAnonymousVolumes() {
        return roAnonymousVolumes;
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

    public Optional<String> getNetworkMode() {
        return Optional.ofNullable(networkMode);
    }

    public Optional<WaitStrategy> getWaitStrategy() {
        return Optional.ofNullable(waitStrategy);
    }

    public Set<String> getRwTmpfsMappings() {
        return rwTmpfsMappings;
    }

    public Set<String> getRoTmpfsMappings() {
        return roTmpfsMappings;
    }

    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
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
