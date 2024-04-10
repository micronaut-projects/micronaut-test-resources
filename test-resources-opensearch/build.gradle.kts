plugins {
    id("io.micronaut.build.internal.testcontainers-module")
}

description = """
Provides core support for OpenSearch test resources.
"""

dependencies {
    api(libs.managed.opensearch.testcontainers)
}

micronautBuild {
    // new module, so disable binary check for now
    binaryCompatibility {
        enabled.set(false)
    }
}
