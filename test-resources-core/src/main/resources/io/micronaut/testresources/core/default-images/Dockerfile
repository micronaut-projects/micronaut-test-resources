# This Dockerfile is not built. It is a Renovate-visible manifest for the
# Docker images used as Micronaut Test Resources provider defaults.
#
# Keep one pinned FROM instruction per default image. The Gradle build generates
# Java constants from this file, so a Renovate update here changes the effective
# default.

# provider: azurite
# property: test-resources.containers.azurite.image-name
FROM mcr.microsoft.com/azure-storage/azurite:3.36.0 AS azurite

# provider: azure-cosmos
# property: test-resources.containers.azure-cosmos.image-name
FROM mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview AS azure_cosmos

# provider: hashicorp-consul
# property: test-resources.containers.hashicorp-consul.image-name
FROM hashicorp/consul:2.0.3 AS consul

# provider: couchbase
# property: test-resources.containers.couchbase.image-name
FROM couchbase/server:8.0.2 AS couchbase

# provider: elasticsearch
# property: test-resources.containers.elasticsearch.image-name
FROM docker.elastic.co/elasticsearch/elasticsearch:9.5.2 AS elasticsearch

# provider: floci
# property: test-resources.containers.floci.image-name
FROM floci/floci:1.7.0 AS floci

# provider: hazelcast
# property: test-resources.containers.hazelcast.image-name
FROM hazelcast/hazelcast:5.7.0-slim AS hazelcast

# provider: hivemq
# property: test-resources.containers.hivemq.image-name
FROM hivemq/hivemq-ce:2026.5 AS hivemq

# provider: infinispan
# property: test-resources.containers.infinispan.image-name
FROM quay.io/infinispan/server:16.2.2 AS infinispan

# provider: kafka
# property: test-resources.containers.kafka.image-name
FROM apache/kafka:4.3.1 AS kafka

# provider: kafka-schema-registry
# property: test-resources.containers.kafka-schema-registry.image-name
FROM confluentinc/cp-schema-registry:8.3.1 AS kafka_schema_registry

# provider: kafka-connect
# property: test-resources.containers.kafka-connect.image-name
FROM confluentinc/cp-kafka-connect:8.3.1 AS kafka_connect

# provider: kafka-ksqldb
# property: test-resources.containers.kafka-ksqldb.image-name
FROM confluentinc/cp-ksqldb-server:8.3.1 AS kafka_ksqldb

# provider: keycloak
# property: test-resources.containers.keycloak.image-name
FROM quay.io/keycloak/keycloak:26.7.2 AS keycloak

# provider: localstack
# property: test-resources.containers.localstack.image-name
FROM localstack/localstack:4.14.0 AS localstack

# provider: mailpit
# property: test-resources.containers.mailpit.image-name
FROM axllent/mailpit:v1.30.7 AS mailpit

# provider: mariadb
# property: test-resources.containers.mariadb.image-name
FROM mariadb:12.3.2 AS mariadb

# provider: minio
# property: test-resources.containers.minio.image-name
FROM minio/minio:RELEASE.2025-09-07T16-13-09Z AS minio

# provider: mongodb
# property: test-resources.containers.mongodb.image-name
FROM mongo:8.3.8 AS mongodb

# provider: mssql
# property: test-resources.containers.mssql.image-name
FROM mcr.microsoft.com/mssql/server:2022-CU24-ubuntu-22.04 AS mssql

# provider: mysql
# property: test-resources.containers.mysql.image-name
FROM mysql:26.7.0 AS mysql

# provider: mysql
# property: test-resources.containers.mysql.image-name
FROM container-registry.oracle.com/mysql/community-server:26.7.1 AS mysql_community

# provider: neo4j
# property: test-resources.containers.neo4j.image-name
FROM neo4j:5.26.29 AS neo4j

# provider: opensearch
# property: test-resources.containers.opensearch.image-name
FROM opensearchproject/opensearch:3.8.0 AS opensearch

# provider: opentelemetry
# property: test-resources.containers.opentelemetry.image-name
FROM grafana/otel-lgtm:0.30.2 AS opentelemetry

# provider: oracle
# property: test-resources.containers.oracle.image-name
FROM gvenzl/oracle-free:slim-faststart AS oracle_free

# provider: oracle-xe
# property: test-resources.containers.oracle-xe.image-name
FROM gvenzl/oracle-xe:slim-faststart AS oracle_xe

# provider: postgres
# property: test-resources.containers.postgres.image-name
FROM postgres:18.6 AS postgres

# provider: pulsar
# property: test-resources.containers.pulsar.image-name
FROM apachepulsar/pulsar:4.2.4 AS pulsar

# provider: rabbitmq
# property: test-resources.containers.rabbitmq.image-name
FROM rabbitmq:4.3.5 AS rabbitmq

# provider: redis
# property: test-resources.containers.redis.image-name
FROM redis:8.10.0 AS redis

# provider: redis-cluster
# property: test-resources.containers.redis.image-name
FROM grokzen/redis-cluster:7.2.5 AS redis_cluster

# provider: seaweedfs
# property: test-resources.containers.seaweedfs.image-name
FROM chrislusf/seaweedfs:4.42 AS seaweedfs

# provider: solr
# property: test-resources.containers.solr.image-name
FROM solr:10.0.0 AS solr

# provider: hashicorp-vault
# property: test-resources.containers.hashicorp-vault.image-name
FROM vault:1.13.3 AS vault
