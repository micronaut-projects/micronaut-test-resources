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
package io.micronaut.testresources.hibernate.reactive.mssql;

import io.micronaut.testresources.compose.AbstractComposeDatabaseTestResourcesProvider;

import java.util.Set;

/**
 * Resolves MSSQL Hibernate Reactive properties from Docker Compose services.
 */
public final class HibernateReactiveMSSQLComposeTestResourcesProvider extends AbstractComposeDatabaseTestResourcesProvider {
    private static final String SERVICE_TYPE = "mssql";

    public HibernateReactiveMSSQLComposeTestResourcesProvider() {
        super(Kind.HIBERNATE_REACTIVE, new Metadata(
            SERVICE_TYPE,
            Set.of(SERVICE_TYPE, "sqlserver", "sql-server", "microsoftsqlserver"),
            1433,
            "jdbc:sqlserver",
            SERVICE_TYPE,
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
