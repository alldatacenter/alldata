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

package org.apache.inlong.sort.base.dirty;

/**
 * Dirty type enum
 */
public enum DirtyType {

    /**
     * Undefined dirty type
     */
    UNDEFINED("Undefined"),
    /**
     * Field mapping error, it refers to the field mapping error between source
     * and sink or between the flink system and the external system.
     * For example, the number of fields contained in the flink system
     * is greater than the number of fields written to the external system, etc.
     */
    FIELD_MAPPING_ERROR("FieldMappingError"),
    /**
     * Data type mapping error, it refers to the data type mismatch between the source and the sink
     * or between the flink system and the external system.
     */
    DATA_TYPE_MAPPING_ERROR("DataTypeMappingError"),
    /**
     * Deserialize error,deserialize errors often occur at the source side that requires decoding, such as kafka, etc.
     */
    DESERIALIZE_ERROR("DeserializeError"),
    /**
     * Serialize error, it occur at the sink side that requires encoding, such as kafka, etc.
     */
    SERIALIZE_ERROR("SerializeError"),
    /**
     * Key deserialize error,deserialize errors often occur at the source side that requires decoding, such as kafka,
     * etc.
     */
    KEY_DESERIALIZE_ERROR("KeyDeserializeError"),
    /**
     * Key serialize error, it occur at the sink side that requires encoding, such as kafka, etc.
     */
    KEY_SERIALIZE_ERROR("KeySerializeError"),
    /**
     * Value deserialize error,deserialize errors often occur at the source side that requires decoding, such as kafka,
     * etc.
     */
    VALUE_DESERIALIZE_ERROR("ValueDeserializeError"),
    /**
     * Value serialize error, it occur at the sink side that requires encoding, such as kafka, etc.
     */
    VALUE_SERIALIZE_ERROR("ValueSerializeError"),
    /**
     * Batch load error
     */
    BATCH_LOAD_ERROR("BatchLoadError"),
    /**
     * Retry load error
     */
    RETRY_LOAD_ERROR("RetryLoadError"),
    /**
     * Unsupported data type
     */
    UNSUPPORTED_DATA_TYPE("UnsupportedDataType"),
    /**
     * Json process error
     */
    JSON_PROCESS_ERROR("JsonProcessError"),
    /**
     * Table identifier parse error
     */
    TABLE_IDENTIFIER_PARSE_ERROR("TableIdentifierParseError"),
    /**
     * Extract schema error
     */
    EXTRACT_SCHEMA_ERROR("ExtractSchemaError"),
    /**
     * Extract RowData error
     */
    EXTRACT_ROWDATA_ERROR("ExtractRowDataError"),
    /**
     * Index generate error
     */
    INDEX_GENERATE_ERROR("IndexGenerateError"),
    /**
     * Index id generate error
     */
    INDEX_ID_GENERATE_ERROR("IndexIdGenerateError"),
    /**
     * Index routing error
     */
    INDEX_ROUTING_ERROR("IndexRoutingError"),
    /**
     * Document parse error
     */
    DOCUMENT_PARSE_ERROR("DocumentParseError"),
    ;

    private final String format;

    DirtyType(String format) {
        this.format = format;
    }

    public String format() {
        return format;
    }
}
