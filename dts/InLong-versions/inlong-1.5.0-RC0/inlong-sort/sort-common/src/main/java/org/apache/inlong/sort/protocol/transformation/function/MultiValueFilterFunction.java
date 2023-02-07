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
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.LogicOperator;
import org.apache.inlong.sort.protocol.transformation.MultiValueCompareOperator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * filter for filtering out data with multi-values
 */
@JsonTypeName("multiValueFilter")
@Data
@NoArgsConstructor
public class MultiValueFilterFunction implements FilterFunction {

    @JsonProperty("source")
    private FunctionParam source;
    @JsonProperty("targets")
    private List<FunctionParam> targets;
    @JsonProperty("compareOperator")
    private MultiValueCompareOperator compareOperator;
    @JsonProperty("logicOperator")
    private LogicOperator logicOperator;

    @JsonCreator
    public MultiValueFilterFunction(
            @JsonProperty("source") FunctionParam source,
            @JsonProperty("targets") List<FunctionParam> targets,
            @JsonProperty("compareOperator") MultiValueCompareOperator compareOperator,
            @JsonProperty("logicOperator") LogicOperator logicOperator) {
        this.source = Preconditions.checkNotNull(source, "source is null");
        this.targets = Preconditions.checkNotNull(targets, "targets is null");
        Preconditions.checkState(!targets.isEmpty(), "targets is empty");
        this.compareOperator = Preconditions.checkNotNull(compareOperator, "compareOperator is null");
        this.logicOperator = Preconditions.checkNotNull(logicOperator, "logicOperator is null");
    }

    @Override
    public String format() {
        String targetStr = StringUtils
                .join(targets.stream().map(FunctionParam::format).collect(Collectors.toList()), ",");
        return String.format("%s %s %s (%s)", logicOperator.format(),
                source.format(), compareOperator.format(), targetStr);
    }

    @Override
    public List<FunctionParam> getParams() {
        List<FunctionParam> params = Arrays.asList(logicOperator, source, compareOperator);
        params.addAll(targets);
        return params;
    }

    @Override
    public String getName() {
        return "multiValueFilter";
    }
}
