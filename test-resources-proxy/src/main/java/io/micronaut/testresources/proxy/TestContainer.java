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
package io.micronaut.testresources.proxy;

import io.micronaut.core.annotation.Introspected;

/**
 * Stores metadata about a running test container.
 */
@Introspected
public final class TestContainer {
    private final String name;
    private final String imageName;
    private final String id;

    public TestContainer(String name, String imageName, String id) {
        this.name = name;
        this.imageName = imageName;
        this.id = id;
    }

    /**
     * Returns the name of the container.
     * @return the name of the container
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the docker image name.
     * @return the docker image name
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Returns the docker container id.
     * @return the docker container id.
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "TestContainer{" +
            "name='" + name + '\'' +
            ", imageName='" + imageName + '\'' +
            ", id='" + id + '\'' +
            '}';
    }
}
