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

import org.apache.inlong.sort.protocol.ddl.enums.AlterType;

import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Alter column expression.
 */
@JsonInclude(Include.NON_NULL)
@Data
public class AlterColumn {

    @JsonProperty("alterType")
    private AlterType alterType;

    @JsonProperty("newColumn")
    private Column newColumn;

    @JsonProperty("oldColumn")
    private Column oldColumn;

    @JsonCreator
    public AlterColumn(@JsonProperty("alterType") AlterType alterType,
            @JsonProperty("newColumn") Column newColumn,
            @JsonProperty("oldColumn") Column oldColumn) {
        this.alterType = alterType;
        this.newColumn = newColumn;
        this.oldColumn = oldColumn;
    }

    public AlterColumn(@JsonProperty("alterType") AlterType alterType) {
        this.alterType = alterType;
    }
}
