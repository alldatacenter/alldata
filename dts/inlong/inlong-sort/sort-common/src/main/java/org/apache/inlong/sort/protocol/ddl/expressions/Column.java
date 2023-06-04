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

package org.apache.inlong.sort.protocol.ddl.expressions;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Column represents a column in a table.
 */
@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class Column {

    @JsonProperty("name")
    private String name;
    @JsonProperty("definition")
    private List<String> definition;
    @JsonProperty("jdbcType")
    private int jdbcType;
    @JsonProperty("position")
    private Position position;
    @JsonProperty("isNullable")
    private boolean isNullable;
    @JsonProperty("defaultValue")
    private String defaultValue;
    @JsonProperty("comment")
    private String comment;

    @JsonCreator
    public Column(@JsonProperty("name") String name, @JsonProperty("definition") List<String> definition,
            @JsonProperty("jdbcType") int jdbcType, @JsonProperty("position") Position position,
            @JsonProperty("isNullable") boolean isNullable, @JsonProperty("defaultValue") String defaultValue,
            @JsonProperty("comment") String comment) {
        this.name = name;
        this.definition = definition;
        this.jdbcType = jdbcType;
        this.position = position;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.isNullable = isNullable;
    }

    public Column(@JsonProperty("name") String name) {
        this.name = name;
    }
}
