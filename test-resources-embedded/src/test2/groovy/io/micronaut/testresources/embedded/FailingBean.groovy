package io.micronaut.testresources.embedded

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Singleton
@CompileStatic
class FailingBean {
    @Value('my-property')
    String property
}
