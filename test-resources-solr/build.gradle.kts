plugins {
    id("io.micronaut.build.internal.testcontainers-module")
}

description = """
Provides core support for Solr test resources.
"""

dependencies {
    api(libs.managed.solr.testcontainers)
}

