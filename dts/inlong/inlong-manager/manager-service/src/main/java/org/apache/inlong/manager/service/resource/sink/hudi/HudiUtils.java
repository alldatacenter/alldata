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

package org.apache.inlong.manager.service.resource.sink.hudi;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * The utility of hudi.
 */
public class HudiUtils {

    private static final String COMMIT_TIME_METADATA_FILE_NAME = "_Hudi_commit_time";
    private static final String COMMIT_SEQNO_METADATA_FILE_NAME = "_Hudi_commit_seqno";
    private static final String RECORD_KEY_METADATA_FILE_NAME = "_Hudi_record_key";
    private static final String PARTITION_PATH_METADATA_FILE_NAME = "_Hudi_partition_path";
    private static final String METADATA_FILE_NAME = "_Hudi_file_name";
    private static final String OPERATION_METADATA_FILE_NAME = "_Hudi_operation";

    public static final String IS_QUERY_AS_RO_TABLE = "Hudi.query.as.ro.table";

    private static final Set<String> HUDI_METADATA_FILES =
            Sets.newHashSet(COMMIT_TIME_METADATA_FILE_NAME, COMMIT_SEQNO_METADATA_FILE_NAME,
                    RECORD_KEY_METADATA_FILE_NAME, PARTITION_PATH_METADATA_FILE_NAME, METADATA_FILE_NAME,
                    OPERATION_METADATA_FILE_NAME);
    private static final String PARQUET_REALTIME_INPUT_FORMAT_NAME =
            "org.apache.hudi.hadoop.realtime.HudiParquetRealtimeInputFormat";
    private static final String PARQUET_INPUT_FORMAT_NAME = "org.apache.hudi.hadoop.HudiParquetInputFormat";
    private static final String HFILE_REALTIME_INPUT_FORMAT_NAME =
            "org.apache.hudi.hadoop.realtime.HudiHFileRealtimeInputFormat";
    private static final String HFILE_INPUT_FORMAT_NAME = "org.apache.hudi.hadoop.HudiHFileInputFormat";
    private static final String ORC_INPUT_FORMAT_NAME = "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat";
    private static final String PARQUET_HIVE_SER_DE_CLASS_NAME =
            "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe";
    private static final String ORC_SER_DE_CLASS_NAME = "org.apache.hadoop.hive.ql.io.orc.OrcSerde";
    private static final String MAPRED_PARQUET_OUTPUT_FORMAT_NAME =
            "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat";
    private static final String ORC_OUTPUT_FORMAT_NAME = "org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat";

    /**
     * Check if metadata file.
     */
    public static boolean isMetadataFile(String fileName) {
        return HUDI_METADATA_FILES.contains(fileName);
    }

    /**
     * Get the InputFormat class name.
     */
    public static String getInputFormatClassName(HudiFileFormat baseFileFormat, boolean realtime) {
        switch (baseFileFormat) {
            case PARQUET:
                if (realtime) {
                    return PARQUET_REALTIME_INPUT_FORMAT_NAME;
                } else {
                    return PARQUET_INPUT_FORMAT_NAME;
                }
            case HFILE:
                if (realtime) {
                    return HFILE_REALTIME_INPUT_FORMAT_NAME;
                } else {
                    return HFILE_INPUT_FORMAT_NAME;
                }
            case ORC:
                return ORC_INPUT_FORMAT_NAME;
            default:
                throw new RuntimeException("Hudi InputFormat not implemented for base file format " + baseFileFormat);
        }
    }

    /**
     * Get the OutputFormat class name.
     */
    public static String getOutputFormatClassName(HudiFileFormat baseFileFormat) {
        switch (baseFileFormat) {
            case PARQUET:
            case HFILE:
                return MAPRED_PARQUET_OUTPUT_FORMAT_NAME;
            case ORC:
                return ORC_OUTPUT_FORMAT_NAME;
            default:
                throw new RuntimeException("No OutputFormat for base file format " + baseFileFormat);
        }
    }

    /**
     * Get the Ser and DeSer class name.
     */
    public static String getSerDeClassName(HudiFileFormat baseFileFormat) {
        switch (baseFileFormat) {
            case PARQUET:
            case HFILE:
                return PARQUET_HIVE_SER_DE_CLASS_NAME;
            case ORC:
                return ORC_SER_DE_CLASS_NAME;
            default:
                throw new RuntimeException("No SerDe for base file format " + baseFileFormat);
        }
    }
}
