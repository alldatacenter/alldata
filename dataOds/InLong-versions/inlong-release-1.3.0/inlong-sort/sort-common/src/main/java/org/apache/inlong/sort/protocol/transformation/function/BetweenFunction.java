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

package org.apache.inlong.sort.protocol.transformation.function;

import com.google.common.base.Preconditions;
import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.LogicOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * The function for between
 */
@JsonTypeName("betweenFunction")
@Data
public class BetweenFunction implements FilterFunction {

    @Nonnull
    @JsonProperty("field")
    private final FunctionParam field;
    @Nonnull
    @JsonProperty("start")
    private final FunctionParam start;
    @Nonnull
    @JsonProperty("end")
    private final FunctionParam end;
    @Nonnull
    @JsonProperty("logicOperator")
    private final LogicOperator logicOperator;

    @JsonCreator
    public BetweenFunction(
            @Nonnull @JsonProperty("logicOperator") LogicOperator logicOperator,
            @Nonnull @JsonProperty("field") FunctionParam field,
            @Nonnull @JsonProperty("start") FunctionParam start,
            @Nonnull @JsonProperty("end") FunctionParam end) {
        this.field = Preconditions.checkNotNull(field, "field is null");
        this.start = Preconditions.checkNotNull(start, "start is null");
        this.end = Preconditions.checkNotNull(end, "end is null");
        this.logicOperator = Preconditions.checkNotNull(logicOperator, "logicOperator is null");
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(logicOperator, field, start, end);
    }

    @Override
    public String getName() {
        return "BETWEEN";
    }

    @Override
    public String format() {
        String format = "%s %s %s %s AND %s";
        if (logicOperator == EmptyOperator.getInstance()) {
            format = "%s%s %s %s AND %s";
        }
        return String.format(format, logicOperator.format(), field.format(), getName(), start.format(), end.format());
    }
}
