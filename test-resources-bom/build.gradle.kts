plugins {
    id("io.micronaut.build.internal.test-resources-base")
    id("io.micronaut.build.internal.bom")
}

micronautBuild {
    binaryCompatibility {
        // preparing major version
        enabledAfter("4.0.0")
    }
}
