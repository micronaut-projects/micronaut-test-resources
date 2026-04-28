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
package io.micronaut.testresources.core

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DefaultTestResourceImagesTest extends Specification {

    private static final Set<String> EXPECTED_ALIASES = [
        'azurite',
        'consul',
        'couchbase',
        'elasticsearch',
        'hazelcast',
        'hivemq',
        'infinispan',
        'kafka',
        'keycloak',
        'localstack',
        'mariadb',
        'minio',
        'mongodb',
        'mssql',
        'mysql',
        'mysql_community',
        'neo4j',
        'opensearch',
        'oracle_free',
        'oracle_xe',
        'postgres',
        'pulsar',
        'rabbitmq',
        'redis',
        'redis_cluster',
        'seaweedfs',
        'solr',
        'vault'
    ] as Set

    def "loads the default image manifest as runtime defaults"() {
        expect:
        DefaultTestResourceImages.images().keySet() == EXPECTED_ALIASES
        DefaultTestResourceImages.DEFAULT_AZURITE_IMAGE == DefaultTestResourceImages.image('azurite')
        DefaultTestResourceImages.DEFAULT_CONSUL_IMAGE == DefaultTestResourceImages.image('consul')
        DefaultTestResourceImages.DEFAULT_COUCHBASE_IMAGE == DefaultTestResourceImages.image('couchbase')
        DefaultTestResourceImages.DEFAULT_ELASTICSEARCH_IMAGE == DefaultTestResourceImages.image('elasticsearch')
        DefaultTestResourceImages.DEFAULT_HAZELCAST_IMAGE == DefaultTestResourceImages.image('hazelcast')
        DefaultTestResourceImages.DEFAULT_HIVEMQ_IMAGE == DefaultTestResourceImages.image('hivemq')
        DefaultTestResourceImages.DEFAULT_INFINISPAN_IMAGE == DefaultTestResourceImages.image('infinispan')
        DefaultTestResourceImages.DEFAULT_KAFKA_IMAGE == DefaultTestResourceImages.image('kafka')
        DefaultTestResourceImages.DEFAULT_KEYCLOAK_IMAGE == DefaultTestResourceImages.image('keycloak')
        DefaultTestResourceImages.DEFAULT_LOCALSTACK_IMAGE == DefaultTestResourceImages.image('localstack')
        DefaultTestResourceImages.DEFAULT_MARIADB_IMAGE == DefaultTestResourceImages.image('mariadb')
        DefaultTestResourceImages.DEFAULT_MINIO_IMAGE == DefaultTestResourceImages.image('minio')
        DefaultTestResourceImages.DEFAULT_MONGODB_IMAGE == DefaultTestResourceImages.image('mongodb')
        DefaultTestResourceImages.DEFAULT_MSSQL_IMAGE == DefaultTestResourceImages.image('mssql')
        DefaultTestResourceImages.DEFAULT_MYSQL_IMAGE == DefaultTestResourceImages.image('mysql')
        DefaultTestResourceImages.DEFAULT_MYSQL_COMMUNITY_IMAGE == DefaultTestResourceImages.image('mysql_community')
        DefaultTestResourceImages.DEFAULT_NEO4J_IMAGE == DefaultTestResourceImages.image('neo4j')
        DefaultTestResourceImages.DEFAULT_OPENSEARCH_IMAGE == DefaultTestResourceImages.image('opensearch')
        DefaultTestResourceImages.DEFAULT_ORACLE_FREE_IMAGE == DefaultTestResourceImages.image('oracle_free')
        DefaultTestResourceImages.DEFAULT_ORACLE_XE_IMAGE == DefaultTestResourceImages.image('oracle_xe')
        DefaultTestResourceImages.DEFAULT_POSTGRES_IMAGE == DefaultTestResourceImages.image('postgres')
        DefaultTestResourceImages.DEFAULT_PULSAR_IMAGE == DefaultTestResourceImages.image('pulsar')
        DefaultTestResourceImages.DEFAULT_RABBITMQ_IMAGE == DefaultTestResourceImages.image('rabbitmq')
        DefaultTestResourceImages.DEFAULT_REDIS_IMAGE == DefaultTestResourceImages.image('redis')
        DefaultTestResourceImages.DEFAULT_REDIS_CLUSTER_IMAGE == DefaultTestResourceImages.image('redis_cluster')
        DefaultTestResourceImages.DEFAULT_SEAWEEDFS_IMAGE == DefaultTestResourceImages.image('seaweedfs')
        DefaultTestResourceImages.DEFAULT_SOLR_IMAGE == DefaultTestResourceImages.image('solr')
        DefaultTestResourceImages.DEFAULT_VAULT_IMAGE == DefaultTestResourceImages.image('vault')
    }

    def "manifest uses pinned Docker image tags Renovate can update"() {
        expect:
        DefaultTestResourceImages.images().every { alias, image ->
            assert imageNamePart(image).contains(':'): "$alias is not pinned: $image"
            assert !image.endsWith(':latest'): "$alias uses a rolling latest tag: $image"
            true
        }
    }

    def "published default image documentation matches the manifest"() {
        given:
        Path root = repositoryRoot()

        expect:
        documentedDefaults().every { fileName, aliases ->
            String doc = Files.readString(root.resolve("src/main/docs/guide/$fileName"))
            aliases.every { alias ->
                assert doc.contains("`${DefaultTestResourceImages.image(alias)}`") ||
                    doc.contains(DefaultTestResourceImages.image(alias)): "$fileName does not document $alias"
                true
            }
        }
    }

    private static Map<String, List<String>> documentedDefaults() {
        [
            'modules-azure.adoc': ['azurite'],
            'modules-couchbase.adoc': ['couchbase'],
            'modules-databases.adoc': ['mariadb', 'mysql_community', 'oracle_xe', 'oracle_free', 'postgres', 'mssql'],
            'modules-elasticsearch.adoc': ['elasticsearch'],
            'modules-hashicorp-consul.adoc': ['consul'],
            'modules-hashicorp-vault.adoc': ['vault'],
            'modules-hazelcast.adoc': ['hazelcast'],
            'modules-infinispan.adoc': ['infinispan'],
            'modules-kafka.adoc': ['kafka'],
            'modules-localstack.adoc': ['localstack'],
            'modules-minio.adoc': ['minio'],
            'modules-mongodb.adoc': ['mongodb'],
            'modules-mqtt.adoc': ['hivemq'],
            'modules-neo4j.adoc': ['neo4j'],
            'modules-oauth2.adoc': ['keycloak'],
            'modules-opensearch.adoc': ['opensearch'],
            'modules-pulsar.adoc': ['pulsar'],
            'modules-rabbitmq.adoc': ['rabbitmq'],
            'modules-redis.adoc': ['redis', 'redis_cluster'],
            'modules-seaweedfs.adoc': ['seaweedfs']
        ]
    }

    private static String imageNamePart(String image) {
        int lastSlash = image.lastIndexOf('/')
        lastSlash == -1 ? image : image.substring(lastSlash + 1)
    }

    private static Path repositoryRoot() {
        Path path = Paths.get('').toAbsolutePath()
        while (path != null && !Files.exists(path.resolve('settings.gradle'))) {
            path = path.parent
        }
        assert path != null
        path
    }
}
