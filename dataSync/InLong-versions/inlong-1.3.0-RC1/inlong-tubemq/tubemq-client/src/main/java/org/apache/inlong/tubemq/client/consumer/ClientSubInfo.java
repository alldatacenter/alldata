/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.client.consumer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class ClientSubInfo {
    private final ConcurrentHashMap<String/* topic */, TopicProcessor> topicCondRegistry =
            new ConcurrentHashMap<>();
    private boolean requireBound = false;
    private AtomicBoolean isNotAllocated =
            new AtomicBoolean(true);
    private int sourceCount = -1;
    private String sessionKey;
    private long subscribedTime;
    private boolean isSelectBig = true;
    private String requiredPartition = "";
    private Set<String> subscribedTopics = new HashSet<>();
    private Map<String, Long> assignedPartMap = new HashMap<>();
    private Map<String, Boolean> topicFilterMap = new HashMap<>();

    public ClientSubInfo() {

    }

    public boolean getIsNotAllocated() {
        return isNotAllocated.get();
    }

    public boolean compareAndSetIsNotAllocated(boolean expect, boolean update) {
        return this.isNotAllocated.compareAndSet(expect, update);
    }

    public boolean isSubscribedTopicEmpty() {
        return this.subscribedTopics.isEmpty();
    }

    public boolean isSubscribedTopicContain(String topic) {
        return this.subscribedTopics.contains(topic);
    }

    public TopicProcessor getTopicProcessor(String topic) {
        return this.topicCondRegistry.get(topic);
    }

    public void storeConsumeTarget(Map<String, TreeSet<String>> consumeTarget) {
        TopicProcessor topicProcessor;
        for (Map.Entry<String, TreeSet<String>> entry : consumeTarget.entrySet()) {
            topicProcessor = new TopicProcessor(null, entry.getValue());
            this.topicCondRegistry.put(entry.getKey(), topicProcessor);
            this.subscribedTopics.add(entry.getKey());
            this.topicFilterMap.put(entry.getKey(),
                    (!(entry.getValue() == null || entry.getValue().isEmpty())));
        }
        this.requireBound = false;
        this.subscribedTime = System.currentTimeMillis();
    }

    public TopicProcessor putIfAbsentTopicProcessor(String topic,
                                                    TopicProcessor topicProcessor) {
        TopicProcessor topicProcessor1 =
                this.topicCondRegistry.putIfAbsent(topic, topicProcessor);
        if (topicProcessor1 == null) {
            this.subscribedTopics.add(topic);
            Set<String> condSet = topicProcessor.getFilterConds();
            topicFilterMap.put(topic, (!(condSet == null || condSet.isEmpty())));
        }
        return topicProcessor1;
    }

    public void notifyAllMessageListenerStopped() {
        for (Map.Entry<String, TopicProcessor> entry : this.topicCondRegistry.entrySet()) {
            if (entry.getValue() != null) {
                MessageListener listener = entry.getValue().getMessageListener();
                if (listener != null) {
                    listener.stop();
                }
            }
        }
        topicCondRegistry.clear();
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public long getSubscribedTime() {
        return subscribedTime;
    }

    public boolean isSelectBig() {
        return isSelectBig;
    }

    public boolean isRequireBound() {
        return requireBound;
    }

    public boolean isFilterConsume(String topic) {
        Boolean ret = this.topicFilterMap.get(topic);
        if (ret == null) {
            return false;
        }
        return ret;
    }

    public void setNotRequireBound() {
        this.requireBound = false;
        this.subscribedTime = System.currentTimeMillis();
    }

    /**
     * Set Bound Consumption information
     *
     * @param sessionKey     consume session key
     * @param sourceCount    the client count of consume group
     * @param isSelectBig    whether select a bigger data If there is reset conflict
     * @param partOffsetMap  the map of partitionKey and bootstrap offset
     *
     */
    public void setRequireBound(final String sessionKey,
                                final int sourceCount,
                                final boolean isSelectBig,
                                final Map<String, Long> partOffsetMap) {
        this.requireBound = true;
        this.subscribedTime = System.currentTimeMillis();
        this.sessionKey = sessionKey;
        this.isSelectBig = isSelectBig;
        this.sourceCount = sourceCount;
        int count = 0;
        StringBuilder sBuilder = new StringBuilder(256);
        for (Map.Entry<String, Long> entry : partOffsetMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                this.assignedPartMap.put(entry.getKey().trim(), entry.getValue());
                if (count++ > 0) {
                    sBuilder.append(TokenConstants.ARRAY_SEP);
                }
                sBuilder.append(entry.getKey().trim()).append(TokenConstants.EQ).append(entry.getValue());
            }
        }
        this.requiredPartition = sBuilder.toString();
    }

    public String getRequiredPartition() {
        return requiredPartition;
    }

    public Set<String> getSubscribedTopics() {
        return subscribedTopics;
    }

    public Map<String, Long> getAssignedPartMap() {
        return assignedPartMap;
    }

    public Long getAssignedPartOffset(String partitionKey) {
        return this.assignedPartMap.get(partitionKey);
    }

    public ConcurrentHashMap<String, TopicProcessor> getTopicCondRegistry() {
        return topicCondRegistry;
    }
}
