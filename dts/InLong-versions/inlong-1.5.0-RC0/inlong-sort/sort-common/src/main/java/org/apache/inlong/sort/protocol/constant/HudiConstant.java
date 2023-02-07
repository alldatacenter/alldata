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

package org.apache.inlong.sort.protocol.constant;

/**
 * Hudi option constant
 */
public class HudiConstant {

    /**
     * Connector key
     */
    public static final String CONNECTOR_KEY = "connector";

    /**
     * 'connector' = 'hudi-inlong'
     */
    public static final String CONNECTOR = "hudi-inlong";

    public static final String ENABLE_CODE = "true";

    /**
     * Asynchronously sync Hive meta to HMS, default false
     */
    public static final String HUDI_OPTION_HIVE_SYNC_ENABLED = "hive_sync.enabled";

    /**
     * Database name for hive sync, default 'default'
     */
    public static final String HUDI_OPTION_HIVE_SYNC_DB = "hive_sync.db";

    /**
     * Table name for hive sync, default 'unknown'
     */
    public static final String HUDI_OPTION_HIVE_SYNC_TABLE = "hive_sync.table";

    /**
     * File format for hive sync, default 'PARQUET'
     */
    public static final String HUDI_OPTION_HIVE_SYNC_FILE_FORMAT = "hive_sync.file_format";

    /**
     * Mode to choose for Hive ops. Valid values are hms, jdbc and hiveql, default 'hms'
     */
    public static final String HUDI_OPTION_HIVE_SYNC_MODE = "hive_sync.mode";

    /**
     * The HMS mode use the hive meta client to sync metadata.
     */
    public static final String HUDI_OPTION_HIVE_SYNC_MODE_HMS_VALUE = "hms";

    /**
     * Metastore uris for hive sync, default ''
     */
    public static final String HUDI_OPTION_HIVE_SYNC_METASTORE_URIS = "hive_sync.metastore.uris";

    /**
     * Base path for the target hoodie table.
     * The path would be created if it does not exist,
     * otherwise a Hoodie table expects to be initialized successfully
     */
    public static final String HUDI_OPTION_DEFAULT_PATH = "path";

    /**
     * Database name that will be used for incremental query.If different databases have the same table name during
     * incremental query,
     * we can set it to limit the table name under a specific database
     */
    public static final String HUDI_OPTION_DATABASE_NAME = "hoodie.database.name";

    /**
     * Table name that will be used for registering with Hive. Needs to be same across runs.
     */
    public static final String HUDI_OPTION_TABLE_NAME = "hoodie.table.name";

    /**
     * Record key field. Value to be used as the `recordKey` component of `HoodieKey`.
     * Actual value will be obtained by invoking .toString() on the field value. Nested fields can be specified using
     * the dot notation eg: `a.b.c`
     */
    public static final String HUDI_OPTION_RECORD_KEY_FIELD_NAME = "hoodie.datasource.write.recordkey.field";

    /**
     * Partition path field. Value to be used at the partitionPath component of HoodieKey.
     * Actual value obtained by invoking .toString()
     */
    public static final String HUDI_OPTION_PARTITION_PATH_FIELD_NAME = "hoodie.datasource.write.partitionpath.field";

    /**
     * The prefix of ddl attr parsed from frontend advanced properties.
     */
    public static final String DDL_ATTR_PREFIX = "ddl.";

    /**
     * The property key of advanced properties.
     */
    public static final String EXTEND_ATTR_KEY_NAME = "keyName";

    /**
     * The property value of advanced properties.
     */
    public static final String EXTEND_ATTR_VALUE_NAME = "keyValue";

    /**
     * Check interval for streaming read of SECOND, default 1 minute
     */
    public static final String READ_STREAMING_CHECK_INTERVAL = "read.streaming.check-interval";

    /**
     * Whether to read as streaming source, default false
     */
    public static final String READ_AS_STREAMING = "read.streaming.enabled";

    /**
     * Start commit instant for reading, the commit time format should be 'yyyyMMddHHmmss',
     * by default reading from the latest instant for streaming read
     */
    public static final String READ_START_COMMIT = "read.start-commit";

    /**
     * Whether to skip compaction instants for streaming read,
     * there are two cases that this option can be used to avoid reading duplicates:
     * 1) you are definitely sure that the consumer reads faster than any compaction instants,
     * usually with delta time compaction strategy that is long enough, for e.g, one week;
     * 2) changelog mode is enabled, this option is a solution to keep data integrity
     */
    public static final String READ_STREAMING_SKIP_COMPACT = "read.streaming.skip_compaction";

    /**
     * Hudi supported catalog type
     */
    public enum CatalogType {

        /**
         * Data stored in hive metastore.
         */
        HIVE,
        /**
         * Data stored in hadoop filesystem.
         */
        HADOOP,
        /**
         * Data stored in hybris metastore.
         */
        HYBRIS;

        /**
         * get catalogType from name
         */
        public static CatalogType forName(String name) {
            for (CatalogType value : values()) {
                if (value.name().equals(name)) {
                    return value;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupport catalogType:%s", name));
        }
    }
}
