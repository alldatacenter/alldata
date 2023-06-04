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

package org.apache.inlong.sort.protocol;

import com.google.common.base.Preconditions;
import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.inlong.sort.protocol.node.Node;
import org.apache.inlong.sort.protocol.transformation.relation.NodeRelation;

import java.io.Serializable;
import java.util.List;

/**
 * The concept of StreamInfo is the same as that of inlong stream
 * It belongs to a group, and a group can contain one or more stream
 */
@Data
public class StreamInfo implements Serializable {

    private static final long serialVersionUID = 82342770067926123L;

    @JsonProperty("streamId")
    private String streamId;
    @JsonProperty("nodes")
    private List<Node> nodes;
    @JsonProperty("relations")
    private List<NodeRelation> relations;

    /**
     * Information of stream.
     * 
     * @param streamId Uniquely identifies of GroupInfo
     * @param nodes The node list that StreamInfo contains
     * @param relations The relation list that StreamInfo contains,
     *         it represents the relation between nodes of StreamInfo
     */
    @JsonCreator
    public StreamInfo(@JsonProperty("streamId") String streamId, @JsonProperty("nodes") List<Node> nodes,
            @JsonProperty("relations") List<NodeRelation> relations) {
        this.streamId = Preconditions.checkNotNull(streamId, "streamId is null");
        this.nodes = Preconditions.checkNotNull(nodes, "nodes is null");
        Preconditions.checkState(!nodes.isEmpty(), "nodes is empty");
        this.relations = Preconditions.checkNotNull(relations, "relations is null");
        Preconditions.checkState(!relations.isEmpty(), "relations is empty");
    }
}
