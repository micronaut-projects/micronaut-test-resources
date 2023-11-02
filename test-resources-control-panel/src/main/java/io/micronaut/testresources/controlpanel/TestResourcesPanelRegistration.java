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
package io.micronaut.testresources.controlpanel;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.RuntimeBeanDefinition;
import io.micronaut.context.annotation.Context;
import io.micronaut.controlpanel.core.ControlPanel;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.micronaut.testresources.embedded.TestResourcesResolverLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This bean is responsible for creating the panels for each
 * test resources resolver.
 */
@Context
public class TestResourcesPanelRegistration {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourcesPanelRegistration.class);

    public static final Argument<ControlPanel> CONTROL_PANEL_ARGUMENT =
        Argument.of(ControlPanel.class, Argument.ofTypeVariable(Object.class, "E"));

    public TestResourcesPanelRegistration(ApplicationContext beanContext,
                                          ControlPanelPropertyResolutionListener resolutionListener) {
        var loader = TestResourcesResolverLoader.getInstance();
        loader.getResolvers()
            .forEach(e ->
                beanContext.registerBeanDefinition(
                    RuntimeBeanDefinition.builder(CONTROL_PANEL_ARGUMENT,
                            () -> new TestResourcesControlPanel(e.getId(), e.getDisplayName(),
                                resolutionListener)
                        ).singleton(true)
                        // Must use a qualifier or only a single panel will show up
                        .qualifier(Qualifiers.byName("testResources" + e.getId()))
                        .build()
                )
            );
    }

    @EventListener
    public void onStartup(ServerStartupEvent event) {
        var server = event.getSource();
        var port = server.getPort();
        LOGGER.info("Test Resources control panel can be browsed at http://localhost:{}/control-panel", port);
    }

}
