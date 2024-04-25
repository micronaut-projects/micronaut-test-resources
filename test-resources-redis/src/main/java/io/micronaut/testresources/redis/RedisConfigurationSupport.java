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

import com.redis.testcontainers.RedisClusterContainer;

import java.util.Map;
import java.util.Optional;

/**
 * Internal class to deal with the test resources
 * redis configuration block.
 */
abstract class RedisConfigurationSupport {
    public static final String CONFIG_REDIS_CLUSTER_MODE = "containers.redis.cluster-mode";
    public static final String CONFIG_REDIS_CLUSTER_MASTERS = "containers.redis.cluster.masters";
    public static final String CONFIG_REDIS_CLUSTER_SLAVES = "containers.redis.cluster.slaves-per-master";
    public static final String CONFIG_REDIS_CLUSTER_INITIAL_PORT = "containers.redis.cluster.initial-port";
    public static final String CONFIG_REDIS_CLUSTER_IP = "containers.redis.cluster.ip";
    public static final String CONFIG_REDIS_CLUSTER_NOTIFY_KEYSPACE_EVENTS = "containers.redis.cluster.notify-keyspace-events";

    private RedisConfigurationSupport() {

    }

    static boolean isClusterMode(Map<String, Object> testResourcesConfig) {
        Boolean clusterMode = (Boolean) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_MODE, false);
        return Boolean.TRUE.equals(clusterMode);
    }

    static int findMasterCount(Map<String, Object> testResourcesConfig) {
        return (Integer) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_MASTERS, RedisClusterContainer.DEFAULT_MASTERS);
    }

    static int findSlavesPerMasterCount(Map<String, Object> testResourcesConfig) {
        return (Integer) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_SLAVES, RedisClusterContainer.DEFAULT_SLAVES_PER_MASTER);
    }

    static int findInitialPort(Map<String, Object> testResourcesConfig) {
        return (Integer) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_INITIAL_PORT, RedisClusterContainer.DEFAULT_INITIAL_PORT);
    }

    static String findIp(Map<String, Object> testResourcesConfig) {
        return (String) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_IP, RedisClusterContainer.DEFAULT_IP);
    }

    static Optional<String> findNotifyKeyspaceEvents(Map<String, Object> testResourcesConfig) {
        String notifyKeyspaceEvents = (String) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_NOTIFY_KEYSPACE_EVENTS, null);
        return Optional.ofNullable(notifyKeyspaceEvents);
    }
}
