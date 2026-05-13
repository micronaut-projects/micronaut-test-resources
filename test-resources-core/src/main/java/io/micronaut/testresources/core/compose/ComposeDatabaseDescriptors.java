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
    private static final String POSTGRES_NAME = "postgres";
    private static final String MYSQL_NAME = "mysql";
    private static final String MARIADB_NAME = "mariadb";
    private static final String MSSQL_NAME = "mssql";
    private static final String ORACLE_NAME = "oracle";
    private static final String TEST = "test";

    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor POSTGRES =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
            POSTGRES_NAME,
            Set.of(POSTGRES_NAME, "postgresql", "pg"),
            5432,
            "jdbc:postgresql",
            "postgresql",
            "org.postgresql.Driver",
            "POSTGRES_USER",
            "POSTGRES_PASSWORD",
            "POSTGRES_DB",
            POSTGRES_NAME,
            POSTGRES_NAME,
            null
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor MYSQL =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
            MYSQL_NAME,
            Set.of(MYSQL_NAME),
            3306,
            "jdbc:mysql",
            MYSQL_NAME,
            "com.mysql.cj.jdbc.Driver",
            "MYSQL_USER",
            "MYSQL_PASSWORD",
            "MYSQL_DATABASE",
            TEST,
            TEST,
            TEST
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor MARIADB =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
            MARIADB_NAME,
            Set.of(MARIADB_NAME, "maria"),
            3306,
            "jdbc:mariadb",
            MARIADB_NAME,
            "org.mariadb.jdbc.Driver",
            "MARIADB_USER",
            "MARIADB_PASSWORD",
            "MARIADB_DATABASE",
            TEST,
            TEST,
            TEST
        );
    public static final ComposeDatabaseResolverSupport.DatabaseDescriptor MSSQL =
        new ComposeDatabaseResolverSupport.DatabaseDescriptor(
            MSSQL_NAME,
            Set.of(MSSQL_NAME, "sqlserver", "sql-server", "microsoftsqlserver"),
            1433,
            "jdbc:sqlserver",
            MSSQL_NAME,
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
            ORACLE_NAME,
            Set.of(ORACLE_NAME, "oracle-free", "oracle-xe"),
            1521,
            "jdbc:oracle:thin",
            ORACLE_NAME,
            "oracle.jdbc.OracleDriver",
            "ORACLE_USER",
            "ORACLE_PASSWORD",
            "ORACLE_DATABASE",
            TEST,
            TEST,
            "freepdb1"
        );

    private ComposeDatabaseDescriptors() {
    }
}
