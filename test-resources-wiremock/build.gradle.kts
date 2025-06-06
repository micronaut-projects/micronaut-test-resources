plugins {
    id("io.micronaut.build.internal.testcontainers-module")
}

description = """
Provides core support for Wiremock test resources.
"""

dependencies {
    api(libs.managed.testcontainers.wiremock)
}
