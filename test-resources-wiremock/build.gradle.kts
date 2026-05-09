plugins {
    id("io.micronaut.build.internal.testcontainers-module")
}

description = """
Provides support for launching a WireMock test container.
"""

dependencies {
    api(libs.managed.wiremock.testcontainers)
}
