plugins {
    id("io.micronaut.build.internal.testcontainers-module")
}

description = """
Provides core support for Solr test resources.
"""

dependencies {
    api(libs.managed.solr.testcontainers)
}

micronautBuild {
    // new module, so disable binary check for now
    binaryCompatibility {
        enabled.set(false)
    }
}
