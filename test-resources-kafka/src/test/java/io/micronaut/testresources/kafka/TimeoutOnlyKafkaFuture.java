/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.testresources.kafka;

import org.apache.kafka.common.KafkaFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class TimeoutOnlyKafkaFuture<T> extends KafkaFuture<T> {

    @Override
    public CompletionStage<T> toCompletionStage() {
        return CompletableFuture.failedFuture(new TimeoutException());
    }

    @Override
    public <R> KafkaFuture<R> thenApply(BaseFunction<T, R> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KafkaFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> biConsumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean complete(T value) {
        return false;
    }

    @Override
    protected boolean completeExceptionally(Throwable throwable) {
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throw new ExecutionException(new TimeoutException());
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new TimeoutException();
    }

    @Override
    public T getNow(T valueIfAbsent) throws InterruptedException, ExecutionException {
        throw new ExecutionException(new TimeoutException());
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isCompletedExceptionally() {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
