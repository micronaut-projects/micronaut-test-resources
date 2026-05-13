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
package io.micronaut.testresources.core.compose;

import io.micronaut.core.annotation.Internal;

import java.util.Set;

/**
 * Shared Compose database descriptors used by provider-owned resolvers.
 */
@Internal
public final class ComposeDatabaseDescriptors {
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor POSTGRES =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
            "postgres",
            Set.of("postgres", "postgresql", "pg"),
            5432,
            "jdbc:postgresql",
            "postgresql",
            "org.postgresql.Driver",
            "POSTGRES_USER",
            "POSTGRES_PASSWORD",
            "POSTGRES_DB",
            "postgres",
            "postgres",
            null
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor MYSQL =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
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
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor MARIADB =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
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
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor MSSQL =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
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
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor ORACLE =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
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
        );

    private ComposeDatabaseDescriptors() {
    }
}
