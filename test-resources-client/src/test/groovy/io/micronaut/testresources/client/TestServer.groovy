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
        Result.of([
            "dummy1",
            "dummy2",
            "missing",
            "throws",
            "datasources.default.url",
            "datasources.default.username",
            "datasources.default.password",
            "datasources.default.driver-class-name",
            "datasources.analytics.url",
            "datasources.analytics.username",
            "datasources.analytics.password",
            "datasources.analytics.driver-class-name"
        ])
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
        if ("throws" == name) {
            throw new RuntimeException("Something bad happened")
        }
        if ("datasources.default.url" == name) {
            return Result.asOptional("jdbc:postgresql://localhost:15432/demo")
        }
        if ("datasources.default.username" == name) {
            return Result.asOptional("demo_user")
        }
        if ("datasources.default.password" == name) {
            return Result.asOptional("demo_secret")
        }
        if ("datasources.default.driver-class-name" == name) {
            return Result.asOptional("org.postgresql.Driver")
        }
        if ("datasources.analytics.url" == name) {
            return Result.asOptional("jdbc:mysql://localhost:13306/analytics")
        }
        if ("datasources.analytics.username" == name) {
            return Result.asOptional("analytics_user")
        }
        if ("datasources.analytics.password" == name) {
            return Result.asOptional("analytics_secret")
        }
        if ("datasources.analytics.driver-class-name" == name) {
            return Result.asOptional("com.mysql.cj.jdbc.Driver")
        }
        Result.asOptional("value for $name".toString())
    }

    @Get("/close/all")
    Result<Boolean> closeAll() {
        Result.TRUE
    }
}
