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
        [
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
        ]
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
    Optional<String> resolve(String name, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if ("missing" == name) {
            return Optional.empty()
        }
        if ("throws" == name) {
            throw new RuntimeException("Something bad happened")
        }
        if ("datasources.default.url" == name) {
            return Optional.of("jdbc:postgresql://localhost:15432/demo")
        }
        if ("datasources.default.username" == name) {
            return Optional.of("demo_user")
        }
        if ("datasources.default.password" == name) {
            return Optional.of("demo_secret")
        }
        if ("datasources.default.driver-class-name" == name) {
            return Optional.of("org.postgresql.Driver")
        }
        if ("datasources.analytics.url" == name) {
            return Optional.of("jdbc:mysql://localhost:13306/analytics")
        }
        if ("datasources.analytics.username" == name) {
            return Optional.of("analytics_user")
        }
        if ("datasources.analytics.password" == name) {
            return Optional.of("analytics_secret")
        }
        if ("datasources.analytics.driver-class-name" == name) {
            return Optional.of("com.mysql.cj.jdbc.Driver")
        }
        Optional.of("value for $name".toString())
    }

    @Get("/close/all")
    void closeAll() {

    }
}
