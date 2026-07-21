plugins {
    id("io.micronaut.build.internal.jdbc-module")
}

description = """
Provides support for launching a Oracle XE test container.
"""

dependencies {
    compileOnly(projects.micronautTestResourcesCompose)
    implementation(libs.managed.testcontainers.oracle.xe)
    testRuntimeOnly(mnSql.ojdbc11)
}
