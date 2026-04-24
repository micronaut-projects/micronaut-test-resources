package io.micronaut.testresources.embedded

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.convert.ConversionService
import io.micronaut.core.io.ResourceLoader
import io.micronaut.core.value.PropertyResolver
import io.micronaut.testresources.core.TestResourcesResolutionException
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Stream

class EmbeddedTestResourcesPropertySourceLoaderWarningSpec extends Specification {
    private final Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    private final TestAppender appender = new TestAppender()

    void setup() {
        EmbeddedDisabledResolverWarningSupport.resetWarnings()
        appender.start()
        rootLogger.addAppender(appender)
    }

    void cleanup() {
        rootLogger.detachAppender(appender)
        appender.stop()
        EmbeddedDisabledResolverWarningSupport.resetWarnings()
    }

    @Unroll
    void "embedded key discovery and expression resolution warn once for disabled provider value #value"() {
        given:
        def resourceLoader = new TestResourceLoader([
            "test-resources.kafka.enabled": value,
            "kafka.test-port"             : 12345
        ])
        def loader = new EmbeddedTestResourcesPropertySourceLoader()
        def resolver = new EmbeddedTestResourcesPropertyExpressionResolver()

        when:
        def firstKeys = loader.load("test-resources", resourceLoader).orElseThrow().iterator().toList()
        def secondKeys = loader.load("test-resources", resourceLoader).orElseThrow().iterator().toList()
        resolver.resolve(resourceLoader, ConversionService.SHARED, "auto.test.resources.kafka.bootstrap-servers", String)

        then:
        def e = thrown(TestResourcesResolutionException)
        e.message.contains("kafka.bootstrap-servers")
        firstKeys == []
        secondKeys == []
        appender.events.count {
            it.level == Level.WARN &&
                it.formattedMessage == "Test resources provider for Apache Kafka is disabled"
        } == 1

        cleanup:
        resolver.close()

        where:
        value << [false, "false"]
    }

    private static final class TestResourceLoader implements ResourceLoader, PropertyResolver {
        private final Map<String, Object> properties

        private TestResourceLoader(Map<String, Object> properties) {
            this.properties = properties
        }

        @Override
        boolean containsProperty(String name) {
            properties.containsKey(name)
        }

        @Override
        boolean containsProperties(String name) {
            properties.keySet().any { it == name || it.startsWith("${name}.") }
        }

        @Override
        <T> Optional<T> getProperty(String name, ArgumentConversionContext<T> conversionContext) {
            def value = properties.get(name)
            if (value == null) {
                return Optional.empty()
            }
            return ConversionService.SHARED.convert(value, conversionContext)
        }

        @Override
        Collection<List<String>> getPropertyPathMatches(String pathPattern) {
            []
        }

        @Override
        Collection<String> getPropertyEntries(String name) {
            properties.keySet()
                .findAll { it.startsWith("${name}.") }
                .collect { it.substring(name.length() + 1) }
        }

        @Override
        Map<String, Object> getProperties(String name) {
            properties.findAll { key, ignored -> key.startsWith("${name}.") }
                .collectEntries { key, value -> [(key.substring(name.length() + 1)): value] }
        }

        @Override
        Optional<InputStream> getResourceAsStream(String path) {
            Optional.empty()
        }

        @Override
        Optional<URL> getResource(String path) {
            Optional.empty()
        }

        @Override
        Stream<URL> getResources(String path) {
            Stream.empty()
        }

        @Override
        boolean supportsPrefix(String path) {
            false
        }

        @Override
        ResourceLoader forBase(String basePath) {
            this
        }
    }

    private static final class TestAppender extends AppenderBase<ILoggingEvent> {
        final List<ILoggingEvent> events = []

        @Override
        protected void append(ILoggingEvent eventObject) {
            events << eventObject
        }
    }
}
