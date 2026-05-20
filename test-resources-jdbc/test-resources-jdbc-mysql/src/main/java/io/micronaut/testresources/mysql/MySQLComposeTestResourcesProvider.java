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
package io.micronaut.testresources.mysql;

import io.micronaut.testresources.compose.AbstractComposeDatabaseTestResourcesProvider;

import java.util.Set;

/**
 * Resolves MySQL JDBC properties from Docker Compose services.
 */
public final class MySQLComposeTestResourcesProvider extends AbstractComposeDatabaseTestResourcesProvider {
    public MySQLComposeTestResourcesProvider() {
        super(Kind.JDBC, new Metadata(
            "mysql",
            Set.of("mysql"),
            3306,
            "jdbc:mysql",
            "mysql",
            "com.mysql.cj.jdbc.Driver",
            "MYSQL_USER",
            "MYSQL_PASSWORD",
            "MYSQL_DATABASE",
            "test",
            "test",
            "test"
        ));
    }
}
