package io.micronaut.testresources.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.scheduling.TaskScheduler
import io.micronaut.testresources.core.ResolverLoader
import io.micronaut.testresources.core.ToggableTestResourcesResolver
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Unroll

class TestResourcesControllerWarningSpec extends Specification {
    private final Logger logger = (Logger) LoggerFactory.getLogger(TestResourcesController)
    private final TestAppender appender = new TestAppender()

    void setup() {
        appender.start()
        logger.addAppender(appender)
    }

    void cleanup() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Unroll
    void "repeated resolution attempts warn once for disabled provider value #value"() {
        given:
        def resolver = new TestResolver()
        def controller = new TestResourcesController(
            [],
            Stub(EmbeddedServer),
            Stub(ApplicationContext),
            Stub(ResolverLoader) {
                getResolvers() >> [resolver]
            },
            Stub(TaskScheduler)
        )

        when:
        def first = controller.resolve("kafka.bootstrap.servers", [:], ["kafka.enabled": value])
        def second = controller.resolve("kafka.bootstrap.servers", [:], ["kafka.enabled": value])

        then:
        !first.present
        !second.present
        resolver.resolveCalls == 0
        appender.events*.formattedMessage == ["Test resources provider for Apache Kafka is disabled"]
        appender.events*.level == [Level.WARN]

        where:
        value << [false, "false"]
    }

    private static final class TestResolver implements ToggableTestResourcesResolver {
        int resolveCalls

        @Override
        String getName() {
            return "kafka"
        }

        @Override
        String getDisplayName() {
            return "Apache Kafka"
        }

        @Override
        List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
            return ["kafka.bootstrap.servers"]
        }

        @Override
        Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
            resolveCalls++
            return Optional.of("should-not-resolve")
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
