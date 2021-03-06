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
package io.micronaut.testresources.buildtools;

/**
 * Represents a Maven dependency using its group, artifact
 * and version coordinates.
 */
public final class MavenDependency {
    private final String group;
    private final String artifact;
    private final String version;

    public MavenDependency(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MavenDependency that = (MavenDependency) o;

        if (!group.equals(that.group)) {
            return false;
        }
        if (!artifact.equals(that.artifact)) {
            return false;
        }
        return version != null ? version.equals(that.version) : that.version == null;
    }

    public String getModule() {
        return getGroup() + ":" + getArtifact();
    }

    @Override
    public int hashCode() {
        int result = group.hashCode();
        result = 31 * result + artifact.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getModule() + (version == null ? "" : ":" + version);
    }
}
