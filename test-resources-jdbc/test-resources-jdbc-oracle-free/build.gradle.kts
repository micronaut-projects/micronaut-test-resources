plugins {
    id("io.micronaut.build.internal.jdbc-module")
}

description = """
Provides support for launching a Oracle Free test container.
"""

dependencies {
    compileOnly(projects.micronautTestResourcesCompose)
    implementation(libs.managed.testcontainers.oracle.free)
    testRuntimeOnly(mnSql.ojdbc11)
}
