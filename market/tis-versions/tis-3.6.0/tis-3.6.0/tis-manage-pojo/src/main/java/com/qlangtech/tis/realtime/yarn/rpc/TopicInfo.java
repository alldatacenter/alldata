/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.realtime.yarn.rpc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年9月28日
 */
public class TopicInfo {

    Map<String, Set<String>> /* tags */
    topicWithTags = new HashMap<>();

    public void addTag(String topic, Set<String> tgs) {
        this.getTagSet(topic).addAll(tgs);
    }

    public void addTag(String topic, String tag) {
        this.getTagSet(topic).add(tag);
    }

    private Set<String> getTagSet(String topic) {
        Set<String> tags = topicWithTags.get(topic);
        if (tags == null) {
            synchronized (this) {
                tags = topicWithTags.get(topic);
                if (tags == null) {
                    tags = new HashSet<>();
                    topicWithTags.put(topic, tags);
                }
            }
        }
        return tags;
    }

    public Map<String, Set<String>> getTopicWithTags() {
        return this.topicWithTags;
    }

    public void setTopicWithTags(Map<String, Set<String>> topicWithTags) {
        this.topicWithTags = topicWithTags;
    }
}
