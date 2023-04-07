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

import java.util.Map;

/**
 * Internal class to deal with the test resources
 * redis configuration block.
 */
abstract class RedisConfigurationSupport {
    public static final String CONFIG_REDIS_CLUSTER_MODE = "containers.redis.cluster-mode";
    public static final String CONFIG_REDIS_CLUSTER_MASTERS = "containers.redis.cluster.masters";
    public static final String CONFIG_REDIS_CLUSTER_SLAVES = "containers.redis.cluster.slaves-per-master";

    private RedisConfigurationSupport() {

    }

    static boolean isClusterMode(Map<String, Object> testResourcesConfig) {
        Boolean clusterMode = (Boolean) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_MODE, false);
        return Boolean.TRUE.equals(clusterMode);
    }

    static int findMasterCound(Map<String, Object> testResourcesConfig) {
        return (Integer) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_MASTERS, RedisClusterContainer.DEFAULT_MASTERS);
    }

    static int findSlavesPerMasterCount(Map<String, Object> testResourcesConfig) {
        return (Integer) testResourcesConfig.getOrDefault(CONFIG_REDIS_CLUSTER_SLAVES, RedisClusterContainer.DEFAULT_SLAVES_PER_MASTER);
    }
}
