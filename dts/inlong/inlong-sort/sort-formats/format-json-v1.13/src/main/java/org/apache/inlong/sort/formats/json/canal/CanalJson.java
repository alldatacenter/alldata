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

package org.apache.inlong.sort.formats.json.canal;

import org.apache.inlong.sort.protocol.ddl.operations.Operation;

import lombok.Builder;
import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

@Builder
@JsonTypeName("canalJson")
@JsonInclude(Include.NON_NULL)
@Data
public class CanalJson {

    @JsonProperty("data")
    private List<Map<String, Object>> data;
    @JsonProperty("es")
    private long es;
    @JsonProperty("table")
    private String table;
    @JsonProperty("type")
    private String type;
    @JsonProperty("database")
    private String database;
    @JsonProperty("ts")
    private long ts;
    @JsonProperty("sql")
    private String sql;
    @JsonProperty("mysqlType")
    private Map<String, String> mysqlType;
    @JsonProperty("sqlType")
    private Map<String, Integer> sqlType;
    @JsonProperty("isDdl")
    private boolean isDdl;
    @JsonProperty("pkNames")
    private List<String> pkNames;
    @JsonProperty("schema")
    private String schema;
    @JsonProperty("oracleType")
    private Map<String, String> oracleType;
    @JsonProperty("operation")
    private Operation operation;
    @JsonProperty("incremental")
    private Boolean incremental;
    @JsonProperty("dataSourceName")
    private String dataSourceName;

    @JsonCreator
    public CanalJson(@Nullable @JsonProperty("data") List<Map<String, Object>> data,
            @JsonProperty("es") long es,
            @JsonProperty("table") String table,
            @JsonProperty("type") String type,
            @JsonProperty("database") String database,
            @JsonProperty("ts") long ts,
            @JsonProperty("sql") String sql,
            @Nullable @JsonProperty("mysqlType") Map<String, String> mysqlType,
            @Nullable @JsonProperty("sqlType") Map<String, Integer> sqlType,
            @JsonProperty("isDdl") boolean isDdl,
            @Nullable @JsonProperty("pkNames") List<String> pkNames,
            @JsonProperty("schema") String schema,
            @Nullable @JsonProperty("oracleType") Map<String, String> oracleType,
            @JsonProperty("operation") Operation operation,
            @JsonProperty("incremental") Boolean incremental,
            @JsonProperty("dataSourceName") String dataSourceName) {
        this.data = data;
        this.es = es;
        this.table = table;
        this.type = type;
        this.database = database;
        this.ts = ts;
        this.sql = sql;
        this.mysqlType = mysqlType;
        this.sqlType = sqlType;
        this.isDdl = isDdl;
        this.pkNames = pkNames;
        this.schema = schema;
        this.oracleType = oracleType;
        this.operation = operation;
        this.incremental = incremental;
        this.dataSourceName = dataSourceName;
    }

}
