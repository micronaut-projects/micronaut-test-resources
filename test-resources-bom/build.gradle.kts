plugins {
    id("io.micronaut.build.internal.test-resources-base")
    id("io.micronaut.build.internal.bom")
}

micronautBom {
    suppressions {
        acceptedLibraryRegressions.add("micronaut-test-resources-hibernate-reactive-core")
        acceptedLibraryRegressions.add("micronaut-test-resources-hibernate-reactive-mssql")
        acceptedLibraryRegressions.add("micronaut-test-resources-hibernate-reactive-mysql")
        acceptedLibraryRegressions.add("micronaut-test-resources-hibernate-reactive-mariadb")
        acceptedLibraryRegressions.add("micronaut-test-resources-hibernate-reactive-postgresql")
        acceptedLibraryRegressions.add("micronaut-test-resources-hibernate-reactive-oracle-xe")
    }
}
