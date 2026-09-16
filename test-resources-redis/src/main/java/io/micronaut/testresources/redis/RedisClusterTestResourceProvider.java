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
package io.micronaut.testresources.redis;

import io.micronaut.testresources.core.DefaultTestResourceImages;
import com.redis.testcontainers.RedisClusterContainer;
import io.micronaut.testresources.testcontainers.AbstractTestContainersProvider;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.micronaut.testresources.redis.RedisConfigurationSupport.findInitialPort;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.findIp;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.findMasterCount;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.findNotifyKeyspaceEvents;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.findSlavesPerMasterCount;
import static io.micronaut.testresources.redis.RedisConfigurationSupport.isClusterMode;

/**
 * A test resource provider which will spawn a Redis cluster test container.
 */
public class RedisClusterTestResourceProvider extends AbstractTestContainersProvider<RedisClusterContainer> {

    public static final String REDIS_URIS = "redis.uris";
    public static final String DISPLAY_NAME = "Redis";
    public static final String SIMPLE_NAME = "redis";
    public static final String DEFAULT_IMAGE = DefaultTestResourceImages.DEFAULT_REDIS_CLUSTER_IMAGE;

    private static final List<String> SUPPORTED_PROPERTIES_LIST = List.of(REDIS_URIS);
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(REDIS_URIS);
    private static final String NOTIFY_KEYSPACE_EVENTS_CONFIG_FORMAT = "notify-keyspace-events %s\n";
    private static final String REDIS_CLUSTER_LOCALE = "C.UTF-8";
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
        int masters = findMasterCount(testResourcesConfig);
        int slavesPerMaster = findSlavesPerMasterCount(testResourcesConfig);
        int initialPort = findInitialPort(testResourcesConfig);
        redisClusterContainer.withMasters(masters);
        redisClusterContainer.withSlavesPerMaster(slavesPerMaster);
        redisClusterContainer.withInitialPort(initialPort);
        redisClusterContainer.withIP(findIp(testResourcesConfig));
        // The 7.2.5 image does not generate en_US.UTF-8 correctly. Use the
        // built-in locale so all Redis processes can start (see
        // https://github.com/Grokzen/docker-redis-cluster/issues/169).
        redisClusterContainer.withEnv("LANG", REDIS_CLUSTER_LOCALE);
        redisClusterContainer.withEnv("LC_ALL", REDIS_CLUSTER_LOCALE);
        redisClusterContainer.setPortBindings(portBindings(initialPort, masters, slavesPerMaster));

        String clusterConfig = CLUSTER_CONFIG + findNotifyKeyspaceEvents(testResourcesConfig)
            .map(NOTIFY_KEYSPACE_EVENTS_CONFIG_FORMAT::formatted)
            .orElse("");
        redisClusterContainer.withCopyToContainer(Transferable.of(clusterConfig), "/redis-conf/redis-cluster.tmpl");

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

    private static List<String> portBindings(int initialPort, int masters, int slavesPerMaster) {
        int totalNodes = masters * (slavesPerMaster + 1);
        List<String> portBindings = new ArrayList<>(totalNodes);
        for (int i = 0; i < totalNodes; i++) {
            int port = initialPort + i;
            portBindings.add(port + ":" + port);
        }
        return portBindings;
    }
}
