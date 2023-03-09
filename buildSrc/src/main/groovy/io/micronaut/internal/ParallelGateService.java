/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.internal;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.testing.Test;

/**
 * A build service which responsibility is simply to make sure that
 * we don't execute tests in parallel.
 */
public abstract class ParallelGateService implements BuildService<BuildServiceParameters.None> {
    private static final Logger LOGGER = Logging.getLogger(ParallelGateService.class);

    public static void requiresResource(Project p, String category) {
        requiresResource(p, category, 1);
    }

    public static void requiresResource(Project p, String category, int maxParallelUsages) {
        Provider<ParallelGateService> serviceProvider = p.getGradle().getSharedServices().registerIfAbsent(category + "Semaphore", ParallelGateService.class, spec -> {
            spec.getMaxParallelUsages().set(maxParallelUsages);
        });
        p.getTasks().withType(Test.class).configureEach(test -> {
            test.usesService(serviceProvider);
            //noinspection Convert2Lambda
            test.doFirst(new Action<>() {
                @Override
                public void execute(Task task) {
                    serviceProvider.get().acquired(category);
                }
            });
        });
    }

    private void acquired(String name) {
        LOGGER.info("Acquired semaphore for {}", name);
    }
}
