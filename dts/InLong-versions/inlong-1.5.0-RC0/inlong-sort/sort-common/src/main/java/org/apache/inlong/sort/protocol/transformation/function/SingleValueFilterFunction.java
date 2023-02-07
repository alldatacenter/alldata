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
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.LogicOperator;
import org.apache.inlong.sort.protocol.transformation.SingleValueCompareOperator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * filter function for a single value
 */
@JsonTypeName("singleValueFilter")
@Data
@NoArgsConstructor
public class SingleValueFilterFunction implements FilterFunction, Serializable {

    private static final long serialVersionUID = 8953419088907830331L;

    @JsonProperty("source")
    private FunctionParam source;
    @JsonProperty("target")
    private FunctionParam target;
    @JsonProperty("compareOperator")
    private SingleValueCompareOperator compareOperator;
    @JsonProperty("logicOperator")
    private LogicOperator logicOperator;

    @JsonCreator
    public SingleValueFilterFunction(
            @JsonProperty("logicOperator") LogicOperator logicOperator,
            @JsonProperty("source") FunctionParam source,
            @JsonProperty("compareOperator") SingleValueCompareOperator compareOperator,
            @JsonProperty("target") FunctionParam target) {
        this.source = Preconditions.checkNotNull(source, "source is null");
        this.target = Preconditions.checkNotNull(target, "target is null");
        this.compareOperator = Preconditions.checkNotNull(compareOperator, "compareOperator is null");
        this.logicOperator = Preconditions.checkNotNull(logicOperator, "logicOperator is null");
    }

    @Override
    public String getName() {
        return "singleValueFilter";
    }

    @Override
    public List<FunctionParam> getParams() {
        return Arrays.asList(logicOperator, source, compareOperator, target);
    }

    @Override
    public String format() {
        return String.format("%s %s %s %s", logicOperator.format(),
                source.format(), compareOperator.format(), target.format());
    }
}
