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
package io.micronaut.testresources.hibernate.reactive.oracle;

import io.micronaut.testresources.hibernate.reactive.core.AbstractHibernateReactiveTestResourceProvider;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * A test resource provider which will spawn an Oracle XE test container.
 *
 * @deprecated Use <code>oracle</code> instead.
 */
@Deprecated(since = "2.4.0", forRemoval = true)
public class HibernateReactiveOracleXETestResourceProvider extends AbstractHibernateReactiveTestResourceProvider<OracleContainer> {
    public static final String DISPLAY_NAME = "Oracle Database (Hibernate Reactive)";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return "oracle-xe";
    }

    @Override
    protected String getDefaultImageName() {
        return "gvenzl/oracle-xe:slim-faststart";
    }

    @Override
    protected OracleContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return new OracleContainer(imageName);
    }

}
