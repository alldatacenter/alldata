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

package org.apache.inlong.sort.protocol;

import org.apache.inlong.common.enums.MetaField;

import java.util.Set;

/**
 * The Metadata class defines a standard protocol to support meta fields
 */
public interface Metadata {

    /**
     * Get the metadata key of MetaField that supported by Extract Nodes or Load Nodes
     *
     * @param metaField The meta field
     * @return The key of metadata
     */
    default String getMetadataKey(MetaField metaField) {
        if (!supportedMetaFields().contains(metaField)) {
            throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                    this.getClass().getSimpleName(), metaField));
        }
        String metadataKey;
        switch (metaField) {
            case TABLE_NAME:
                metadataKey = "table_name";
                break;
            case COLLECTION_NAME:
                metadataKey = "collection_name";
                break;
            case SCHEMA_NAME:
                metadataKey = "schema_name";
                break;
            case DATABASE_NAME:
                metadataKey = "database_name";
                break;
            case OP_TS:
                metadataKey = "op_ts";
                break;

            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                        this.getClass().getSimpleName(), metaField));
        }
        return metadataKey;
    }

    /**
     * Get metadata type MetaField that supported by Extract Nodes or Load Nodes
     *
     * @param metaField The meta field
     * @return The type of metadata that based on Flink SQL types
     */
    default String getMetadataType(MetaField metaField) {
        if (!supportedMetaFields().contains(metaField)) {
            throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                    this.getClass().getSimpleName(), metaField));
        }
        String metadataType;
        switch (metaField) {
            case TABLE_NAME:
            case DATABASE_NAME:
            case OP_TYPE:
            case DATA_CANAL:
            case DATA:
            case DATA_DEBEZIUM:
            case COLLECTION_NAME:
            case SCHEMA_NAME:
            case KEY:
            case VALUE:
            case HEADERS_TO_JSON_STR:
                metadataType = "STRING";
                break;
            case OP_TS:
            case TS:
            case TIMESTAMP:
                metadataType = "TIMESTAMP_LTZ(3)";
                break;
            case IS_DDL:
                metadataType = "BOOLEAN";
                break;
            case SQL_TYPE:
                metadataType = "MAP<STRING, INT>";
                break;
            case MYSQL_TYPE:
                metadataType = "MAP<STRING, STRING>";
                break;
            case ORACLE_TYPE:
                metadataType = "MAP<STRING, STRING>";
                break;
            case PK_NAMES:
                metadataType = "ARRAY<STRING>";
                break;
            case HEADERS:
                metadataType = "MAP<STRING, BINARY>";
                break;
            case BATCH_ID:
            case PARTITION:
            case OFFSET:
                metadataType = "BIGINT";
                break;
            case UPDATE_BEFORE:
                metadataType = "ARRAY<MAP<STRING, STRING>>";
                break;
            case DATA_BYTES:
            case DATA_BYTES_DEBEZIUM:
            case DATA_BYTES_CANAL:
                metadataType = "BYTES";
                break;
            default:
                throw new UnsupportedOperationException(String.format("Unsupport meta field for %s: %s",
                        this.getClass().getSimpleName(), metaField));
        }
        return metadataType;
    }

    /**
     * Is virtual.
     * By default, the planner assumes that a metadata column can be used for both reading and writing.
     * However, in many cases an external system provides more read-only metadata fields than writable fields.
     * Therefore, it is possible to exclude metadata columns from persisting using the VIRTUAL keyword.
     *
     * @param metaField The meta field
     * @return true if it is virtual else false
     */
    boolean isVirtual(MetaField metaField);

    /**
     * Supported meta field set
     *
     * @return The set of supported meta field
     */
    Set<MetaField> supportedMetaFields();

    /**
     * Format string by Flink SQL
     *
     * @param metaField The meta field
     * @return The string that format by Flink SQL
     */
    default String format(MetaField metaField) {
        if (metaField == MetaField.PROCESS_TIME) {
            return "AS PROCTIME()";
        }
        return String.format("%s METADATA FROM '%s'%s",
                getMetadataType(metaField), getMetadataKey(metaField), isVirtual(metaField) ? " VIRTUAL" : "");
    }
}
