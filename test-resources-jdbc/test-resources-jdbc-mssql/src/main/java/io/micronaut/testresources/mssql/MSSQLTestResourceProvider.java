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
package io.micronaut.testresources.mssql;

import io.micronaut.testresources.jdbc.AbstractJdbcTestResourceProvider;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LicenseAcceptance;

import java.util.Map;

/**
 * A test resource provider which will spawn a MS SQL test container.
 */
public class MSSQLTestResourceProvider extends AbstractJdbcTestResourceProvider<MSSQLServerContainer<?>> {

    public static final String DEFAULT_IMAGE_NAME = "mcr.microsoft.com/mssql/server:2019-CU16-GDR1-ubuntu-20.04";
    public static final String DISPLAY_NAME = "MSSQL";

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return "mssql";
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE_NAME;
    }

    @Override
    protected MSSQLServerContainer<?> createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return createMSSQLContainer(imageName, getSimpleName(), testResourcesConfig);
    }

    public static MSSQLServerContainer<?> createMSSQLContainer(DockerImageName imageName, String simpleName, Map<String, Object> testResourcesConfig) {
        MSSQLServerContainer<?> container = new MSSQLServerContainer<>(imageName);
        String licenseKey = "containers." + simpleName + ".accept-license";
        if (shouldAcceptLicense(licenseKey, testResourcesConfig)) {
            container.acceptLicense();
        } else {
            try {
                LicenseAcceptance.assertLicenseAccepted(imageName.toString());
            } catch (IllegalStateException ex) {
                throw new IllegalStateException("You must set the property 'test-resources." + licenseKey + "' to true in order to use a Microsoft SQL Server test container", ex);
            }
        }
        return container;
    }

    /**
     *
     * @param licenseKey License Key
     * @param testResourcesConfig Test Resources Configuration
     * @return {@code false} if no value found in Test Resources Configuration for the license key, otherwise it returns the object if it is a  boolean, or it parses the value to a boolean using {@link Boolean#parseBoolean(String)}.
     */
    public static boolean shouldAcceptLicense(String licenseKey, Map<String, Object> testResourcesConfig) {
        Object obj = testResourcesConfig.get(licenseKey);
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean b) {
            return b;
        } else {
            return Boolean.parseBoolean(obj.toString());
        }
    }
}
