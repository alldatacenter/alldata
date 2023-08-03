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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Operation represents a ddl operation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlterOperation.class, name = "alterOperation"),
        @JsonSubTypes.Type(value = CreateTableOperation.class, name = "createTableOperation"),
        @JsonSubTypes.Type(value = DropTableOperation.class, name = "dropTableOperation"),
        @JsonSubTypes.Type(value = TruncateTableOperation.class, name = "truncateTableOperation"),
        @JsonSubTypes.Type(value = RenameTableOperation.class, name = "renameTableOperation"),
        @JsonSubTypes.Type(value = UnsupportedOperation.class, name = "unsupportedOperation")
})
@Data
@NoArgsConstructor
public abstract class Operation {

    @JsonProperty("operationType")
    private OperationType operationType;

    public Operation(@JsonProperty("operationType") OperationType type) {
        this.operationType = type;
    }

}
