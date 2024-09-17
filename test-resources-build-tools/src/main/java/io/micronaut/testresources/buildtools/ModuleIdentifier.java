package io.micronaut.testresources.buildtools;

/**
 * Represents a Maven artifact, without version.
 * @param groupId the group id of the artifact
 * @param artifactId the artifact id of the artifact
 */
public record ModuleIdentifier(
    String groupId,
    String artifactId
) {
}
