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
package io.micronaut.testresources.server;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The expiry manager will handle pings from the client to keep the server alive.
 */
@Singleton
@InterceptorBean(Ping.class)
public final class ExpiryManager implements MethodInterceptor<Object, Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiryManager.class);

    private static final long MS_IN_ONE_MINUTE = 60_000;
    private final long timeoutMs;

    private long lastAccess;

    public ExpiryManager(@Value("${server.idle.timeout.minutes:60}") int keepAliveMinutes) {
        this.timeoutMs = keepAliveMinutes * MS_IN_ONE_MINUTE;
        ping();
        LOGGER.info("Test resources server will automatically be shutdown if it doesn't receive requests for {} minutes", keepAliveMinutes);
    }

    private void ping() {
        this.lastAccess = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - lastAccess > timeoutMs;
    }

    @Override
    public @Nullable Object intercept(MethodInvocationContext<Object, Object> context) {
        ping();
        return context.proceed();
    }
}
