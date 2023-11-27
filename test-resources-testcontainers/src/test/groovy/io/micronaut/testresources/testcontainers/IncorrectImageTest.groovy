package io.micronaut.testresources.testcontainers

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
class IncorrectImageTest extends Specification {

    @Inject
    ApplicationContext applicationContext

    void "reasonable error message if an image doesn't exist"() {
        when:
        applicationContext.getProperty("incorrect-image.host", String)

        then:
        Exception ex = thrown()
        ex.message.contains """Status 404: {"message":"pull access denied for this-image-does-not-exist, repository does not exist or may require 'docker login': denied: requested access to the resource is denied"}"""
    }
}
