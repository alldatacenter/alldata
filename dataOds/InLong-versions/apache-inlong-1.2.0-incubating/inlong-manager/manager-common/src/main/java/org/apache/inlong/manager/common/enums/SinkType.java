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

package org.apache.inlong.manager.common.enums;

import java.util.Locale;

public enum SinkType {

    HIVE,
    KAFKA,
    ICEBERG,
    CLICKHOUSE,
    HBASE,
    POSTGRES,
    ELASTICSEARCH,
    SQLSERVER,
    HDFS,
    GREENPLUM,
    MYSQL,


    ;

    public static final String SINK_HIVE = "HIVE";
    public static final String SINK_KAFKA = "KAFKA";
    public static final String SINK_ICEBERG = "ICEBERG";
    public static final String SINK_CLICKHOUSE = "CLICKHOUSE";
    public static final String SINK_HBASE = "HBASE";
    public static final String SINK_POSTGRES = "POSTGRES";
    public static final String SINK_ELASTICSEARCH = "ELASTICSEARCH";
    public static final String SINK_SQLSERVER = "SQLSERVER";
    public static final String SINK_HDFS = "HDFS";
    public static final String SINK_GREENPLUM = "GREENPLUM";
    public static final String SINK_MYSQL = "MYSQL";

    /**
     * Get the SinkType enum via the given sinkType string
     */
    public static SinkType forType(String sinkType) {
        for (SinkType type : values()) {
            if (type.name().equals(sinkType)) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("Illegal sink type for %s", sinkType));
    }

    @Override
    public String toString() {
        return this.name().toUpperCase(Locale.ROOT);
    }

}
