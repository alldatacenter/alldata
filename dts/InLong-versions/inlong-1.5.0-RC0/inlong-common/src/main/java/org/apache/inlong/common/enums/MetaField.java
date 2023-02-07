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

package org.apache.inlong.common.enums;

/**
 * Meta field definition enum class
 */
public enum MetaField {

    /**
     * The process time of flink
     */
    PROCESS_TIME,

    /**
     * Name of the schema that contain the row, currently used for Oracle, PostgreSQL, SQLSERVER
     */
    SCHEMA_NAME,

    /**
     * Name of the database that contain the row.
     */
    DATABASE_NAME,

    /**
     * Name of the table that contain the row.
     */
    TABLE_NAME,

    /**
     * It indicates the time that the change was made in the database.
     * If the record is read from snapshot of the table instead of the change stream, the value is always 0
     */
    OP_TS,

    /**
     * Whether the DDL statement. Currently, it is used for MySQL database.
     */
    IS_DDL,

    /**
     * Type of database operation, such as INSERT/DELETE, etc. Currently, it is used for MySQL database.
     */
    OP_TYPE,

    /**
     * Represents a canal json of a record in database (in string format)
     * @deprecated please use DATA_CANAL \ DATA_DEBEZIUM
     */
    DATA,

    /**
     * Represents a canal json of a record in database (in string format)
     */
    DATA_CANAL,

    /**
     * Represents a debezium json of a record in database (in string format)
     */
    DATA_DEBEZIUM,

    /**
     * Represents a canal json of a record in database (in bytes format)
     * @deprecated please use DATA_BYTES_DEBEZIUM \ DATA_CANAL_BYTES
     */
    DATA_BYTES,

    /**
     * Represents a debezium json of a record in database (in bytes format)
     */
    DATA_BYTES_DEBEZIUM,

    /**
     * Represents a canal json of a record in database (in bytes format)
     */
    DATA_BYTES_CANAL,

    /**
     * The value of the field before update. Currently, it is used for MySQL database.
     */
    UPDATE_BEFORE,

    /**
     * Batch id of binlog. Currently, it is used for MySQL database.
     */
    BATCH_ID,

    /**
     * Mapping of sql_type table fields to java data type IDs. Currently, it is used for MySQL database.
     */
    SQL_TYPE,

    /**
     * The current time when the ROW was received and processed. Currently, it is used for MySQL database.
     */
    TS,

    /**
     * The table structure. It is only used for MySQL database
     */
    MYSQL_TYPE,

    /**
     * The table structure. It is only used for Oracle database
     */
    ORACLE_TYPE,

    /**
     * Primary key field name. Currently, it is used for MySQL database.
     */
    PK_NAMES,

    /**
     * Name of the collection that contain the row, it is only used for MongoDB.
     */
    COLLECTION_NAME,

    /**
     * key of the Kafka record, it is only used for Kafka.
     */
    KEY,

    /**
     * value of the Kafka record, it is only used for Kafka.
     */
    VALUE,

    /**
     * Partition ID of the Kafka record, it is only used for Kafka.
     */
    PARTITION,

    /**
     * Headers of the Kafka record as a map of raw bytes, it is only used for Kafka.
     */
    HEADERS,

    /**
     * Headers of the Kafka record as a json string, it is only used for Kafka.
     */
    HEADERS_TO_JSON_STR,

    /**
     * Offset of the Kafka record in the partition., it is only used for Kafka.
     */
    OFFSET,

    /**
     * Timestamp of the Kafka record, it is only used for Kafka.
     */
    TIMESTAMP;

    public static MetaField forName(String name) {
        for (MetaField metaField : values()) {
            if (metaField.name().equals(name)) {
                return metaField;
            }
        }
        throw new UnsupportedOperationException(String.format("Unsupported MetaField=%s", name));
    }
}
