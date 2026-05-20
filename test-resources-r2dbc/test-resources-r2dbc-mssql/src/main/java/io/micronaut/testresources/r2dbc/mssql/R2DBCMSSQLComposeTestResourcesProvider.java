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
package io.micronaut.testresources.r2dbc.mssql;

import io.micronaut.testresources.compose.AbstractComposeDatabaseTestResourcesProvider;

import java.util.Set;

/**
 * Resolves MSSQL R2DBC properties from Docker Compose services.
 */
public final class R2DBCMSSQLComposeTestResourcesProvider extends AbstractComposeDatabaseTestResourcesProvider {
    public R2DBCMSSQLComposeTestResourcesProvider() {
        super(Kind.R2DBC, new Metadata(
            "mssql",
            Set.of("mssql", "sqlserver", "sql-server", "microsoftsqlserver"),
            1433,
            "jdbc:sqlserver",
            "mssql",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "MSSQL_USER",
            "MSSQL_PASSWORD",
            "MSSQL_DATABASE",
            "SA",
            "A_Str0ng_Required_Password",
            "test"
        ));
    }
}
