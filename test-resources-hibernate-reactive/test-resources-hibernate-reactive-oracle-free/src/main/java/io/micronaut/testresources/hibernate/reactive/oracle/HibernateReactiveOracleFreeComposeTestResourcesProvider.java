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

import io.micronaut.testresources.compose.AbstractComposeDatabaseTestResourcesProvider;

import java.util.Set;

/**
 * Resolves Oracle Free Hibernate Reactive properties from Docker Compose services.
 */
public final class HibernateReactiveOracleFreeComposeTestResourcesProvider extends AbstractComposeDatabaseTestResourcesProvider {
    public HibernateReactiveOracleFreeComposeTestResourcesProvider() {
        super(Kind.HIBERNATE_REACTIVE, new Metadata(
            "oracle",
            Set.of("oracle", "oracle-free", "oracle-xe"),
            1521,
            "jdbc:oracle:thin",
            "oracle",
            "oracle.jdbc.OracleDriver",
            "ORACLE_USER",
            "ORACLE_PASSWORD",
            "ORACLE_DATABASE",
            "test",
            "test",
            "freepdb1"
        ));
    }
}
