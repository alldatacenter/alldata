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

package org.apache.inlong.manager.pojo.stream;

import com.google.common.collect.Sets;
import lombok.Data;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.Set;

/**
 * Stream node relation info, including input node name list, output node name list.
 */
@Data
public class StreamNodeRelation {

    private Set<String> inputNodes;

    private Set<String> outputNodes;

    public StreamNodeRelation() {
        this(Sets.newHashSet(), Sets.newHashSet());
    }

    public StreamNodeRelation(Set<String> inputNodes, Set<String> outputNodes) {
        this.inputNodes = inputNodes;
        this.outputNodes = outputNodes;
    }

    public void addInputNode(String inputNode) {
        Preconditions.checkNotEmpty(inputNode, "Input node should not be empty");
        inputNodes.add(inputNode);
    }

    public void addOutputNode(String outputNode) {
        Preconditions.checkNotEmpty(outputNode, "Input node should not be empty");
        outputNodes.add(outputNode);
    }
}
