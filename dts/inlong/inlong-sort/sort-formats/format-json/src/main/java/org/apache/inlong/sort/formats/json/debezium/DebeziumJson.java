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

package org.apache.inlong.sort.formats.json.debezium;

import io.debezium.relational.history.TableChanges;
import io.debezium.relational.history.TableChanges.TableChange;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.ddl.operations.Operation;

@Builder
@JsonTypeName("canalJson")
@JsonInclude(Include.NON_NULL)
@Data
public class DebeziumJson {

    @JsonProperty("before")
    private Map<String, String> before;
    @JsonProperty("after")
    private Map<String, Object> after;
    @JsonProperty("source")
    private Source source;
    @JsonProperty("tableChange")
    private TableChanges.TableChange tableChange;
    @JsonProperty("tsMs")
    private long tsMs;
    @JsonProperty("op")
    private String op;
    @JsonProperty("incremental")
    private boolean incremental;
    @JsonProperty("ddl")
    private String ddl;
    @JsonProperty("operation")
    private Operation operation;
    @JsonProperty("dataSourceName")
    private String dataSourceName;

    public DebeziumJson(@JsonProperty("before") Map<String, String> before,
            @JsonProperty("after") Map<String, Object> after,
            @JsonProperty("source") Source source,
            @JsonProperty("tableChange") TableChange tableChange,
            @JsonProperty("tsMs") long tsMs, @JsonProperty("op") String op,
            @JsonProperty("incremental") boolean incremental,
            @JsonProperty("ddl") String ddl,
            @JsonProperty("operation") Operation operation,
            @JsonProperty("dataSourceName") String dataSourceName) {
        this.before = before;
        this.after = after;
        this.source = source;
        this.tableChange = tableChange;
        this.tsMs = tsMs;
        this.op = op;
        this.incremental = incremental;
        this.ddl = ddl;
        this.operation = operation;
        this.dataSourceName = dataSourceName;
    }

    @Builder
    @Data
    public static class Source {

        private String name;
        private String db;
        private String table;
        private List<String> pkNames;
        private Map<String, Integer> sqlType;
        private Map<String, String> mysqlType;
    }

}
