package io.micronaut.testresources.client

import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.testresources.core.TestResourcesResolver

@Controller("/")
@Requires(property = 'server', notEquals = 'false')
 class TestServer implements TestResourcesResolver {

    @Override
    @Post("/list")
    List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        ["dummy1", "dummy2", "missing"]
    }

    @Override
    @Get("/requirements/expr/{expression}")
    List<String> getRequiredProperties(String expression) {
        []
    }

    @Override
    @Get("/requirements/entries")
    List<String> getRequiredPropertyEntries() {
        []
    }

    @Override
    @Post('/resolve')
    Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfiguration) {
        if ("missing" == name) {
            return Optional.empty()
        }
        Optional.of("value for $name".toString())
    }

    @Get("/close/all")
    void closeAll() {

    }
}
