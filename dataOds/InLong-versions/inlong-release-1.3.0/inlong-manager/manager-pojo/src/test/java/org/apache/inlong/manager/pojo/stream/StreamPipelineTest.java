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
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Stream pipeline service test for check has circle.
 */
public class StreamPipelineTest {

    @Test
    public void testCheckHasCircle() {
        StreamPipeline streamPipeline = new StreamPipeline();
        streamPipeline.addRelation(new StreamNodeRelation(Sets.newHashSet("A", "B"), Sets.newHashSet("C")));
        streamPipeline.addRelation(new StreamNodeRelation(Sets.newHashSet("C"), Sets.newHashSet("D")));
        streamPipeline.addRelation(new StreamNodeRelation(Sets.newHashSet("D"), Sets.newHashSet("E", "F")));
        streamPipeline.addRelation(new StreamNodeRelation(Sets.newHashSet("F"), Sets.newHashSet("G")));
        streamPipeline.addRelation(new StreamNodeRelation(Sets.newHashSet("E"), Sets.newHashSet("H", "C")));
        Pair<Boolean, Pair<String, String>> circleState = streamPipeline.hasCircle();
        Assertions.assertTrue(circleState.getLeft());
        Assertions.assertTrue(Sets.newHashSet("E", "C").contains(circleState.getRight().getLeft()));
        Assertions.assertTrue(Sets.newHashSet("E", "C").contains(circleState.getRight().getRight()));
    }

}
