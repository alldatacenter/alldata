/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.json.canal;

import org.apache.inlong.sort.formats.json.MysqlBinLogData;
import org.apache.inlong.sort.formats.json.canal.CanalJsonDecodingFormat.ReadableMetadata;

public class CanalUtils {

    public static String getMysqlMetadataKey(ReadableMetadata readableMetadata) {
        switch (readableMetadata) {
            case DATABASE:
                return MysqlBinLogData.MYSQL_METADATA_DATABASE;
            case TABLE:
                return MysqlBinLogData.MYSQL_METADATA_TABLE;
            case IS_DDL:
                return MysqlBinLogData.MYSQL_METADATA_IS_DDL;
            case EVENT_TIMESTAMP:
                return MysqlBinLogData.MYSQL_METADATA_EVENT_TIME;
            default:
                throw new IllegalArgumentException("Not supported yet");
        }
    }
}
