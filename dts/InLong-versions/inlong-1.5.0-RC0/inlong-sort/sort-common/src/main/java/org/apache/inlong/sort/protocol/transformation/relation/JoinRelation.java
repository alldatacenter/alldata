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
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import java.util.List;
import java.util.Map;

/**
 * Join relation abstract class
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FullOuterJoinRelation.class, name = "fullOuterJoin"),
        @JsonSubTypes.Type(value = InnerJoinNodeRelation.class, name = "innerJoin"),
        @JsonSubTypes.Type(value = LeftOuterJoinNodeRelation.class, name = "leftOuterJoin"),
        @JsonSubTypes.Type(value = RightOuterJoinNodeRelation.class, name = "rightOutJoin"),
        @JsonSubTypes.Type(value = InnerTemporalJoinRelation.class, name = "innerTemporalJoin"),
        @JsonSubTypes.Type(value = LeftOuterTemporalJoinRelation.class, name = "leftOuterTemporalJoin"),
        @JsonSubTypes.Type(value = IntervalJoinRelation.class, name = "intervalJoin")
})
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public abstract class JoinRelation extends NodeRelation {

    private static final long serialVersionUID = -213673939512251116L;

    @JsonProperty("joinConditionMap")
    private Map<String, List<FilterFunction>> joinConditionMap;

    /**
     * JoinRelation Constructor
     *
     * @param inputs The inputs is a list of input node id
     * @param outputs The outputs is a list of output node id
     * @param joinConditionMap The joinConditionMap is a map of join conditions
     *         the key of joinConditionMap is the node id of join node
     *         the value of joinConditionMap is a list of join contidition
     */
    public JoinRelation(@JsonProperty("inputs") List<String> inputs,
            @JsonProperty("outputs") List<String> outputs,
            @JsonProperty("joinConditionMap") Map<String, List<FilterFunction>> joinConditionMap) {
        super(inputs, outputs);
        this.joinConditionMap = Preconditions.checkNotNull(joinConditionMap, "joinConditionMap is null");
        Preconditions.checkState(!joinConditionMap.isEmpty(), "joinConditionMap is empty");
    }

    /**
     * Node relation format
     * that is, the relation is converted into a string representation of SQL
     *
     * @return a string representation of SQL
     */
    public abstract String format();
}
