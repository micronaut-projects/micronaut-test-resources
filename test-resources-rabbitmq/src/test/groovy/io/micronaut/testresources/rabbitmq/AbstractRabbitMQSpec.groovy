package io.micronaut.testresources.rabbitmq

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import groovy.transform.Canonical
import io.micronaut.context.annotation.Prototype
import io.micronaut.core.annotation.Introspected
import io.micronaut.rabbitmq.annotation.RabbitClient
import io.micronaut.rabbitmq.annotation.RabbitListener
import io.micronaut.rabbitmq.connect.ChannelInitializer
import io.micronaut.testresources.testcontainers.AbstractTestContainersSpec

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

abstract class AbstractRabbitMQSpec extends AbstractTestContainersSpec {
    @Override
    String getScopeName() {
        'rabbitmq'
    }

    @Prototype
    static class SomeBean {
        String message = 'hello'
    }

    @Introspected
    @Canonical
    static class Book {
        String title
    }

    @jakarta.inject.Singleton
    static class ChannelPoolListener extends ChannelInitializer {
        @Override
        void initialize(Channel channel, String name) throws IOException {
            channel.exchangeDeclare("micronaut", BuiltinExchangeType.DIRECT, true)
            channel.queueDeclare("analytics", true, false, false, null)
            channel.queueBind("analytics", "micronaut", "analytics")
        }
    }

    @RabbitClient("micronaut")
    static interface Publisher {

        @io.micronaut.rabbitmq.annotation.Binding("analytics")
        void updateAnalytics(Book book)
    }

    @RabbitListener
    static class Consumer {

        LinkedBlockingDeque<Book> books = []

        @io.micronaut.rabbitmq.annotation.Queue("analytics")
        void updateAnalytics(Book book) {
            books << book
        }

        Book getBook() {
            return books.poll(30, TimeUnit.SECONDS)
        }
    }
}
