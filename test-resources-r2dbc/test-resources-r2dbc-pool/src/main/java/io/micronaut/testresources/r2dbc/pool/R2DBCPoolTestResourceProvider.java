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
package io.micronaut.testresources.r2dbc.pool;

import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.r2dbc.core.R2dbcSupport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A test resource provider for configuring the R2DBC pool.
 */
public class R2DBCPoolTestResourceProvider implements TestResourcesResolver {

    private static final String PROTOCOL = "options.protocol";
    private static final String DRIVER = "options.driver";
    private static final List<String> RESOLVABLE_KEYS = Collections.unmodifiableList(Arrays.asList(
        PROTOCOL,
        DRIVER
    ));

    @Override
    public List<String> getRequiredPropertyEntries() {
        return R2dbcSupport.REQUIRED_PROPERTY_ENTRIES_LIST;
    }

    @Override
    public List<String> getRequiredProperties(String expression) {
        return R2dbcSupport.findRequiredProperties(expression);
    }

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        return R2dbcSupport.findResolvableProperties(propertyEntries, RESOLVABLE_KEYS);
    }

    @Override
    public Optional<String> resolve(String propertyName, Map<String, Object> properties, Map<String, Object> testResourcesConfig) {
        if (!propertyName.startsWith(R2dbcSupport.R2DBC_PREFIX)) {
            return Optional.empty();
        }
        String baseDatasourceExpression = R2dbcSupport.removeR2dbPrefixFrom(propertyName);
        String datasource = R2dbcSupport.datasourceNameFrom(baseDatasourceExpression);
        if (R2dbcSupport.r2dbDatasourceExpressionOf(datasource, PROTOCOL).equals(propertyName)) {
            Object dialect = properties.get(R2dbcSupport.r2dbDatasourceExpressionOf(datasource, R2dbcSupport.DIALECT));
            if (dialect != null) {
                return Optional.of(String.valueOf(dialect).replace("_", "").toLowerCase(Locale.ROOT));
            }
        } else if (R2dbcSupport.r2dbDatasourceExpressionOf(datasource, DRIVER).equals(propertyName)) {
            return Optional.of("pool");
        }
        return Optional.empty();
    }

}
