/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.plugin.flink.enums;

import lombok.Getter;

/**
 * Connectors corresponding to different datasource types in inlong-sort
 */
@Getter
public enum ConnectorJarType {

    /**
     * extract datasource type
     */
    MONGODB_SOURCE("mongoExtract", "mongodb-cdc"),

    MYSQL_SOURCE("mysqlExtract", "mysql-cdc"),

    KAFKA_SOURCE("kafkaExtract", "kafka"),

    ORACLE_SOURCE("oracleExtract", "oracle-cdc"),

    POSTGRES_SOURCE("postgresExtract", "postgres-cdc"),

    SQLSERVER_SOURCE("sqlserverExtract", "sqlserver-cdc"),

    PULSAR_SOURCE("pulsarExtract", "pulsar"),

    /**
     * load datasource type
     */
    MYSQL_SINK("mysqlLoad", "jdbc"),

    KAFKA_SINK("kafkaLoad", "kafka"),

    ORACLE_SINK("oracleLoad", "jdbc"),

    POSTGRES_SINK("postgresLoad", "jdbc"),

    SQLSERVER_SINK("sqlserverLoad", "jdbc"),

    HBASE_SINK("hbaseLoad", "hbase"),

    TDSQLPOSTGRES_SINK("tdsqlPostgresLoad", "jdbc"),

    GREENPLUM_SINK("greenplumLoad", "jdbc"),

    ELASTICSEARCH_SINK("elasticsearchLoad", "elasticsearch"),

    CLICKHOUSE_SINK("clickHouseLoad", "jdbc"),

    HIVE_SINK("hiveLoad", "hive"),

    ICEBERG_SINK("icebergLoad", "iceberg"),

    HUDI_SINK("hudiLoad", "hudi"),

    HDFS_SINK("fileSystemLoad", ""),
    REDIS_SINK("redisLoad", "redis"),

    ;

    private String sourceType;
    private String connectorType;

    ConnectorJarType(String sourceType, String connectorType) {
        this.connectorType = connectorType;
        this.sourceType = sourceType;
    }

    /**
     * Gets datasource connectorJarType
     *
     * @param type dataSourceType
     * @return ConnectorType
     */
    public static ConnectorJarType getInstance(String type) {
        for (ConnectorJarType value : values()) {
            if (value.getSourceType().equals(type)) {
                return value;
            }
        }
        return null;
    }

}
