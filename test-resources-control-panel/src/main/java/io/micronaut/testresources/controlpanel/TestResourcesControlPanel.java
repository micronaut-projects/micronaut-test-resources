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

import io.micronaut.controlpanel.core.AbstractControlPanel;
import io.micronaut.controlpanel.core.config.ControlPanelConfiguration;

/**
 * A control panel for test resource resolvers.
 * Each resolver will result in the creation of a separate control panel.
 * The control panel is responsible for showing the properties resolved
 * by this particular test resources resolver.
 */
public class TestResourcesControlPanel extends AbstractControlPanel<TestResourcesControlPanelBody> {
    private final ControlPanelPropertyResolutionListener resolutionListener;
    private final String id;

    public TestResourcesControlPanel(String id,
                                     String name,
                                     ControlPanelPropertyResolutionListener resolutionListener) {
        super(id, createConfiguration(id, name));
        this.id = id;
        this.resolutionListener = resolutionListener;
    }

    @Override
    public String getBadge() {
        return String.valueOf(
            resolutionListener.findById(id).size()
        );
    }

    public int getErrorCount() {
        return resolutionListener.findErrorsById(id).size();
    }

    @Override
    public TestResourcesControlPanelBody getBody() {
        return new TestResourcesControlPanelBody(
            resolutionListener.findById(id),
            resolutionListener.findErrorsById(id)
        );
    }

    @Override
    public View getBodyView() {
        return new View("/views/test-resources/body");
    }

    @Override
    public View getDetailedView() {
        return new View("/views/test-resources/detail");
    }

    private static ControlPanelConfiguration createConfiguration(String id, String name) {
        var controlPanelConfiguration = new ControlPanelConfiguration(id);
        controlPanelConfiguration.setTitle(name);
        controlPanelConfiguration.setIcon("fa-flask-vial");
        return controlPanelConfiguration;
    }
}
