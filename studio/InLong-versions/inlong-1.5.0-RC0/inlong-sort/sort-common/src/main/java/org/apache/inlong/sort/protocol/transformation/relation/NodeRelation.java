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
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Node relation base class which defines the simplest one-to-one relation
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FullOuterJoinRelation.class, name = "fullOuterJoin"),
        @JsonSubTypes.Type(value = InnerJoinNodeRelation.class, name = "innerJoin"),
        @JsonSubTypes.Type(value = LeftOuterJoinNodeRelation.class, name = "leftOuterJoin"),
        @JsonSubTypes.Type(value = RightOuterJoinNodeRelation.class, name = "rightOutJoin"),
        @JsonSubTypes.Type(value = InnerTemporalJoinRelation.class, name = "innerTemporalJoin"),
        @JsonSubTypes.Type(value = LeftOuterTemporalJoinRelation.class, name = "leftOuterTemporalJoin"),
        @JsonSubTypes.Type(value = IntervalJoinRelation.class, name = "intervalJoin"),
        @JsonSubTypes.Type(value = UnionNodeRelation.class, name = "union"),
        @JsonSubTypes.Type(value = NodeRelation.class, name = "baseRelation")
})
@Data
@NoArgsConstructor
public class NodeRelation implements Serializable {

    private static final long serialVersionUID = 5491943876653981952L;

    @JsonProperty("inputs")
    private List<String> inputs;
    @JsonProperty("outputs")
    private List<String> outputs;

    /**
     * NodeRelation Constructor
     *
     * @param inputs The inputs is a list of input node id
     * @param outputs The outputs is a list of output node id
     */
    @JsonCreator
    public NodeRelation(@JsonProperty("inputs") List<String> inputs,
            @JsonProperty("outputs") List<String> outputs) {
        this.inputs = Preconditions.checkNotNull(inputs, "inputs is null");
        Preconditions.checkState(!inputs.isEmpty(), "inputs is empty");
        this.outputs = Preconditions.checkNotNull(outputs, "outputs is null");
        Preconditions.checkState(!outputs.isEmpty(), "outputs is empty");
    }
}
