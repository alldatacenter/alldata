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

package org.apache.inlong.sort.protocol.transformation.relation;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * This class defines the interval join relation.In interval join, the join conditions is same as filters,
 * and so we forbid the filters for interval join. And the same time,
 * the joinConditionMap will be allowed to have only one value.
 */
@JsonTypeName("intervalJoin")
@EqualsAndHashCode(callSuper = true)
@Data
public class IntervalJoinRelation extends JoinRelation {

    /**
     * Constructor
     *
     * @param inputs The inputs is a list of input node id
     * @param outputs The outputs is a list of output node id
     * @param joinConditionMap The joinConditionMap is a map of join conditions
     *         the key of joinConditionMap is the node id of join node
     *         the value of joinConditionMap is a list of join contidition
     */
    public IntervalJoinRelation(@JsonProperty("inputs") List<String> inputs,
            @JsonProperty("outputs") List<String> outputs,
            @JsonProperty("joinConditionMap") LinkedHashMap<String, List<FilterFunction>> joinConditionMap) {
        super(inputs, outputs, joinConditionMap);
        Preconditions.checkState(joinConditionMap.size() == 1,
                String.format("The size of joinConditionMap must be one for %s", this.getClass().getSimpleName()));
    }

    @Override
    public String format() {
        throw new UnsupportedOperationException(String.format("Format is not supported for %s",
                this.getClass().getSimpleName()));
    }
}
