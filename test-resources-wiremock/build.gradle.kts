plugins {
    id("io.micronaut.build.internal.testcontainers-module")
}

description = """
Provides core support for Wiremock test resources.
"""

dependencies {
    api(libs.managed.testcontainers.wiremock)
}

micronautBuild {
    // new module, so disable binary check for now
    binaryCompatibility {
        enabled.set(false)
    }
}
