/*
 * Copyright 2017-2024 original authors
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

import io.micronaut.testresources.core.ResolverLoader;
import io.micronaut.testresources.core.TestResourcesResolver;
import io.micronaut.testresources.embedded.TestResourcesResolverLoader;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ResolverLoader} that loads {@link TestResourcesResolver} instances from both Java
 * service loading and the Micronaut application context.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 2.4.0
 */
@Singleton
public class CompositeResolverLoader implements ResolverLoader {

    private final List<TestResourcesResolver> resolvers;

    public CompositeResolverLoader(List<InjectableTestResourcesResolver> resolverBeans) {
        var loader = TestResourcesResolverLoader.getInstance();
        this.resolvers = new ArrayList<>(resolverBeans.size() + loader.getResolvers().size());
        this.resolvers.addAll(resolverBeans);
        this.resolvers.addAll(loader.getResolvers());
    }

    @Override
    public List<TestResourcesResolver> getResolvers() {
        return resolvers;
    }
}
