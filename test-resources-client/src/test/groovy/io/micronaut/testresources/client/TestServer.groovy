package io.micronaut.testresources.client

import io.micronaut.context.annotation.Requires
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.testresources.codec.Result
import io.micronaut.testresources.codec.TestResourcesMediaType

@Controller("/")
@Requires(property = 'server', notEquals = 'false')
@Produces(TestResourcesMediaType.TEST_RESOURCES_BINARY)
@Consumes(TestResourcesMediaType.TEST_RESOURCES_BINARY)
class TestServer {

    @Post("/list")
    Result<List<String>> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        Result.of(["dummy1", "dummy2", "missing"])
    }

    @Get("/requirements/expr/{expression}")
    Result<List<String>> getRequiredProperties(String expression) {
        Result.of([])
    }

    @Get("/requirements/entries")
    Result<List<String>> getRequiredPropertyEntries() {
        Result.of([])
    }

    @Post('/resolve')
    Optional<Result<String>> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if ("missing" == name) {
            return Optional.empty()
        }
        Result.asOptional("value for $name".toString())
    }

    @Get("/close/all")
    Result<Boolean> closeAll() {
        Result.TRUE
    }
}
