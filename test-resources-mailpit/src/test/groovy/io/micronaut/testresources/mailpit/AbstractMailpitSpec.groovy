package io.micronaut.testresources.mailpit

import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

abstract class AbstractMailpitSpec extends AbstractTestContainersSpec {
    @Override
    String getScopeName() {
        'mailpit'
    }
}
