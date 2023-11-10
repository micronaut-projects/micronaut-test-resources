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
package io.micronaut.testresources.redis;

import com.redis.testcontainers.RedisClusterContainer;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.micronaut.testresources.redis.RedisConfigurationSupport.findMasterCound;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.findSlavesPerMasterCount;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.isClusterMode;

/**
 * A test resource provider which will spawn a Redis cluster test container.
 */
public class RedisClusterTestResourceProvider extends AbstractTestContainersProvider<RedisClusterContainer> {

    public static final String REDIS_URIS = "redis.uris";
    public static final String DISPLAY_NAME = "Redis";
    public static final String SIMPLE_NAME = "redis";
    public static final String DEFAULT_IMAGE = RedisClusterContainer.DEFAULT_IMAGE_NAME.asCanonicalNameString();

    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(REDIS_URIS);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(REDIS_URIS);
    /**
     * Alternate cluster configuration file, workaround for
     * https://github.com/Grokzen/docker-redis-cluster/discussions/149 .
     */
    private static final String CLUSTER_CONFIG = """
        bind ${BIND_ADDRESS}
        port ${PORT}
        cluster-enabled yes
        cluster-config-file nodes.conf
        cluster-node-timeout 5000
        appendonly yes
        dir /redis-data/${PORT}
        protected-mode no
        """;

    @Override
    public List<String> getResolvableProperties(Map<String, Collection<String>> propertyEntries, Map<String, Object> testResourcesConfig) {
        boolean clusterMode = isClusterMode(testResourcesConfig);
        return clusterMode ? SUPPORTED_PROPERTIES_LIST : List.of();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected String getSimpleName() {
        return SIMPLE_NAME;
    }

    @Override
    protected String getDefaultImageName() {
        return DEFAULT_IMAGE;
    }

    @Override
    protected RedisClusterContainer createContainer(DockerImageName imageName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        RedisClusterContainer redisClusterContainer = new RedisClusterContainer(imageName);
        redisClusterContainer.withCopyToContainer(Transferable.of(CLUSTER_CONFIG), "/redis-conf/redis-cluster.tmpl");
        redisClusterContainer.withMasters(findMasterCound(testResourcesConfig));
        redisClusterContainer.withSlavesPerMaster(findSlavesPerMasterCount(testResourcesConfig));
        return redisClusterContainer;
    }

    @Override
    protected Optional<String> resolveProperty(String propertyName, RedisClusterContainer container) {
        if (REDIS_URIS.equals(propertyName)) {
            return Optional.of(String.join(",", container.getRedisURIs()));
        }
        return Optional.empty();
    }

    @Override
    protected boolean shouldAnswer(String propertyName, Map<String, Object> requestedProperties, Map<String, Object> testResourcesConfig) {
        return SUPPORTED_PROPERTIES.contains(propertyName);
    }
}
