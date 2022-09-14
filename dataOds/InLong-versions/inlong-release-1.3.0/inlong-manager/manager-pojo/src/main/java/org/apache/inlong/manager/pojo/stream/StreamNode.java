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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.List;
import java.util.Set;

/**
 * Stream node, including data node name, pre node name, post node name, field list.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StreamNode {

    protected Set<String> preNodes;

    protected Set<String> postNodes;

    protected List<StreamField> fieldList;

    public void addPre(String pre) {
        Preconditions.checkNotEmpty(pre, "Pre node should not be empty");
        if (preNodes == null) {
            preNodes = Sets.newHashSet();
        }
        preNodes.add(pre);
    }

    public void addPost(String post) {
        Preconditions.checkNotEmpty(post, "Post node should not be empty");
        if (postNodes == null) {
            postNodes = Sets.newHashSet();
        }
        postNodes.add(post);
    }
}
