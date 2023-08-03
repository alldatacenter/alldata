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

package org.apache.inlong.sort.protocol.ddl.operations;

import org.apache.inlong.sort.protocol.ddl.enums.OperationType;
import org.apache.inlong.sort.protocol.ddl.expressions.Column;
import org.apache.inlong.sort.protocol.ddl.indexes.Index;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

/**
 * CreateTableOperation represents a create table operation
 * it can be "create table like" or "create table with columns and indexes"
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("createTableOperation")
@JsonInclude(Include.NON_NULL)
@Data
public class CreateTableOperation extends Operation {

    @JsonProperty("columns")
    private List<Column> columns;

    @JsonProperty("indexes")
    private List<Index> indexes;

    @JsonProperty("likeTable")
    private String likeTable;

    @JsonProperty("comment")
    private String comment;

    @JsonCreator
    public CreateTableOperation(@JsonProperty("columns") List<Column> columns,
            @JsonProperty("indexes") List<Index> indexes,
            @JsonProperty("likeTable") String likeTable,
            @JsonProperty("comment") String comment) {
        super(OperationType.CREATE);
        this.columns = columns;
        this.indexes = indexes;
        this.likeTable = likeTable;
        this.comment = comment;
    }

    public CreateTableOperation() {
        super(OperationType.CREATE);
    }

}
