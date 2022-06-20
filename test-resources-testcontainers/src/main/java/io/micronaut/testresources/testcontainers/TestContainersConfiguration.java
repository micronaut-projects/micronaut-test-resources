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

import io.micronaut.context.annotation.EachProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents the mutable view of the container metadata, for
 * documentation purposes.
 * See {@link TestContainerMetadata} for the immutable version
 * which is used internally.
 */
@SuppressWarnings("unused")
@EachProperty("test-resources.containers")
final class TestContainersConfiguration {
    private String imageName;
    private String imageTag;
    private List<String> hostnames;
    private Map<String, Integer> exposedPorts;
    private Map<String, String> roFsBind;
    private Map<String, String> rwFsBind;
    private String command;
    private String workingDirectory;
    private Map<String, String> env;
    private Map<String, String> labels;
    private String startupTimeout;
    private Map<String, String> copyToContainer;
    private String memory;
    private String swapMemory;
    private String sharedMemory;

    /**
     * Returns the name of the docker image to use for the test resources container.
     * @return the docker image name.
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * The name of the docker image to use.
     * @param imageName the docker image name.
     */
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /**
     * The image tag.
     * @return the image tag.
     */
    public String getImageTag() {
        return imageTag;
    }

    /**
     * The image tag, in case it's not specified in the image name or
     * that you want to specifically override the default tag but not
     * the image name.
     * @param imageTag the image tag
     */
    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    /**
     * Returns the names of the properties which will be set
     * to the host name of the container.
     * @return the host name properties.
     */
    public List<String> getHostnames() {
        return hostnames;
    }

    /**
     * Names of the properties which will be set to the
     * host name of the container (e.g. if "some.host", then
     * the "some.host" property will be set to the value of
     * the container host name)
     *
     * @param hostnames the host name properties.
     */
    public void setHostnames(List<String> hostnames) {
        this.hostnames = hostnames;
    }

    /**
     * The map of property names to port names.
     * @return The map of property names to port names.
     */
    public Map<String, Integer> getExposedPorts() {
        return exposedPorts;
    }

    /**
     * Sets the names of the properties which will be set to
     * the exposed port of the container. For example, if the
     * container exposes port 25 and that you need the "smtp.port"
     * property to be set to the value of the container port,
     * then the key needs to be set to "smtp.port" and the value to 25.
     * @param exposedPorts the map of property names to port names.
     */
    public void setExposedPorts(Map<String, Integer> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    /**
     * Returns the map of read-only system bindings.
     * @return the read-only system bindings.
     */
    public Map<String, String> getRoFsBind() {
        return roFsBind;
    }

    /**
     * A map where the key is a path in the host filesystem and the value is
     * a path in the container filesystem.
     * The path will be mounted read-only.
     * @param roFwBind the map of read-only fs bindings.
     */
    public void setRoFsBind(Map<String, String> roFwBind) {
        this.roFsBind = roFwBind;
    }

    /**
     * Returns the map of read-write system bindings.
     * @return the read-write system bindings.
     */
    public Map<String, String> getRwFsBind() {
        return rwFsBind;
    }

    /**
     * A map where the key is a path in the host filesystem and the value is
     * a path in the container filesystem.
     * The path will be mounted read-write.
     * @param rwFsBind the map of read-write fs bindings.
     */
    public void setRwFsBind(Map<String, String> rwFsBind) {
        this.rwFsBind = rwFsBind;
    }

    /**
     * The container command.
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * The container command, for example: "./gradlew run".
     * @param command the container command
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * The working directory for the container.
     * @return the working directory
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * The working directory of the container.
     * @param workingDirectory the working directory
     */
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * The map of environment variables.
     * @return the map of environment variables.
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     * The environment variables to set in the container.
     * @param env the environment variables
     */
    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    /**
     * The labels to set on the started container.
     * @return the labels
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * The labels to set on the started container.
     * @param labels the labels
     */
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     * The startup timeout of the container.
     * @return the startup timeout
     */
    public String getStartupTimeout() {
        return startupTimeout;
    }

    /**
     * The startup timer of the container, for example: "60s".
     * @param startupTimeout the startup timeout
     */
    public void setStartupTimeout(String startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    /**
     * The files to be copied to the container.
     * @return the files to be copied to the container.
     */
    public Map<String, String> getCopyToContainer() {
        return copyToContainer;
    }

    /**
     * The files to be copied to the container. The key represents a
     * path on the host filesystem and the value represents a path
     * on the container filesystem.
     *
     * If the key is prefixed by <code>classpath:</code> then the
     * key will represent a path of an entry on classpath.
     * @param copyToContainer the files to be copied to the container.
     */
    public void setCopyToContainer(Map<String, String> copyToContainer) {
        this.copyToContainer = copyToContainer;
    }

    /**
     * The memory limit of the container.
     * @return the memory limit
     */
    public String getMemory() {
        return memory;
    }

    /**
     * The memory limit of the container.
     * Can be expressed in bytes, kilobytes (eg 600kb), megabytes (e.g 256mb),
     * or gigabytes (e.g 1.5g).
     * @param memory the memory limit
     */
    public void setMemory(String memory) {
        this.memory = memory;
    }

    /**
     * The swap memory limit of the container.
     * @return the swap memory limit
     */
    public String getSwapMemory() {
        return swapMemory;
    }

    /**
     * The swap memory limit of the container.
     * Can be expressed in bytes, kilobytes (eg 600kb), megabytes (e.g 256mb),
     * or gigabytes (e.g 1.5g).
     * @param swapMemory the memory limit
     */
    public void setSwapMemory(String swapMemory) {
        this.swapMemory = swapMemory;
    }

    /**
     * The shared memory limit of the container.
     * @return the shared memory limit
     */
    public String getSharedMemory() {
        return sharedMemory;
    }

    /**
     * The shared memory limit of the container.
     * Can be expressed in bytes, kilobytes (eg 600kb), megabytes (e.g 256mb),
     * or gigabytes (e.g 1.5g).
     * @param sharedMemory the memory limit
     */
    public void setSharedMemory(String sharedMemory) {
        this.sharedMemory = sharedMemory;
    }
}
