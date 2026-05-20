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
package io.micronaut.testresources.r2dbc.mariadb;

import io.micronaut.testresources.compose.AbstractComposeDatabaseTestResourcesProvider;

import java.util.Set;

/**
 * Resolves MariaDB R2DBC properties from Docker Compose services.
 */
public final class R2DBCMariaDBComposeTestResourcesProvider extends AbstractComposeDatabaseTestResourcesProvider {
    public R2DBCMariaDBComposeTestResourcesProvider() {
        super(Kind.R2DBC, new Metadata(
            "mariadb",
            Set.of("mariadb", "maria"),
            3306,
            "jdbc:mariadb",
            "mariadb",
            "org.mariadb.jdbc.Driver",
            "MARIADB_USER",
            "MARIADB_PASSWORD",
            "MARIADB_DATABASE",
            "test",
            "test",
            "test"
        ));
    }
}
