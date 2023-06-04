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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Stream pipeline, save stream node relation list.
 */
@Data
public class StreamPipeline {

    private List<StreamNodeRelation> pipeline;

    public StreamPipeline() {
        this(Lists.newArrayList());
    }

    public StreamPipeline(List<StreamNodeRelation> pipeline) {
        Preconditions.expectNotNull(pipeline, "Pipeline should not be null");
        this.pipeline = pipeline;
    }

    public void addRelation(StreamNodeRelation relation) {
        pipeline.add(relation);
    }

    /**
     * Check if a pipeline has a circle, if it has, return circled node names
     */
    public Pair<Boolean, Pair<String, String>> hasCircle() {
        Map<String, Set<String>> priorityMap = Maps.newHashMap();
        for (StreamNodeRelation relation : pipeline) {
            Set<String> inputNodes = relation.getInputNodes();
            Set<String> outputNodes = relation.getOutputNodes();
            for (String inputNode : inputNodes) {
                for (String outputNode : outputNodes) {
                    priorityMap.computeIfAbsent(inputNode, key -> Sets.newHashSet()).add(outputNode);
                    if (CollectionUtils.isEmpty(priorityMap.get(outputNode))) {
                        continue;
                    }
                    Set<String> priorityNodesOfOutput = priorityMap.get(outputNode);
                    if (priorityNodesOfOutput.contains(inputNode)) {
                        return Pair.of(true, Pair.of(inputNode, outputNode));
                    } else {
                        if (isReach(priorityMap, priorityNodesOfOutput, inputNode)) {
                            return Pair.of(true, Pair.of(inputNode, outputNode));
                        }
                    }
                }
            }
        }
        return Pair.of(false, null);
    }

    private boolean isReach(Map<String, Set<String>> paths, Set<String> inputs, String output) {
        Queue<String> queue = new LinkedList<>(inputs);
        Set<String> preNodes = new HashSet<>(inputs);
        while (!queue.isEmpty()) {
            String node = queue.remove();
            if (paths.get(node) == null) {
                continue;
            }
            Set<String> postNodes = paths.get(node);
            if (postNodes.contains(output)) {
                return true;
            }
            for (String postNode : postNodes) {
                if (!preNodes.contains(postNode)) {
                    preNodes.add(postNode);
                    queue.add(postNode);
                }
            }
        }
        return false;
    }
}
