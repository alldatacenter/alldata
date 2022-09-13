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

package org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.tubemq.corebase.balance.ConsumerEvent;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerEventManager {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerEventManager.class);

    private final ConcurrentHashMap<String/* consumerId */, LinkedList<ConsumerEvent>> disconnectEventMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* consumerId */, LinkedList<ConsumerEvent>> connectEventMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* group */, AtomicInteger> groupUnfinishedCountMap =
            new ConcurrentHashMap<>();

    private final ConsumerInfoHolder consumerHolder;

    public ConsumerEventManager(ConsumerInfoHolder consumerHolder) {
        this.consumerHolder = consumerHolder;
    }

    public boolean addDisconnectEvent(String consumerId,
                                      ConsumerEvent event) {
        LinkedList<ConsumerEvent> eventList =
                disconnectEventMap.get(consumerId);
        if (eventList == null) {
            eventList = new LinkedList<>();
            LinkedList<ConsumerEvent> tmptList =
                    disconnectEventMap.putIfAbsent(consumerId, eventList);
            if (tmptList == null) {
                MasterSrvStatsHolder.incSvrBalDisConConsumerCnt();
            } else {
                eventList = tmptList;
            }
        }
        synchronized (eventList) {
            return eventList.add(event);
        }
    }

    public boolean addConnectEvent(String consumerId,
                                   ConsumerEvent event) {
        LinkedList<ConsumerEvent> eventList =
                connectEventMap.get(consumerId);
        if (eventList == null) {
            eventList = new LinkedList<>();
            LinkedList<ConsumerEvent> tmptList =
                    connectEventMap.putIfAbsent(consumerId, eventList);
            if (tmptList == null) {
                MasterSrvStatsHolder.incSvrBalConEventConsumerCnt();
            } else {
                eventList = tmptList;
            }
        }
        synchronized (eventList) {
            return eventList.add(event);
        }
    }

    /**
     * Peek a consumer event from event map,
     * disconnect event have priority over connect event
     *
     * @param consumerId the consumer id
     * @return the head of event list, or null if event list is empty
     */
    public ConsumerEvent peek(String consumerId) {
        String group =
                consumerHolder.getGroupName(consumerId);
        if (group != null) {
            ConcurrentHashMap<String, LinkedList<ConsumerEvent>> currentEventMap =
                    hasDisconnectEvent(group)
                            ? disconnectEventMap : connectEventMap;
            LinkedList<ConsumerEvent> eventList =
                    currentEventMap.get(consumerId);
            if (eventList != null) {
                synchronized (eventList) {
                    return eventList.peek();
                }
            }
        } else {
            logger.warn(new StringBuilder(512)
                    .append("No group by consumer ")
                    .append(consumerId).toString());
        }
        return null;
    }

    /**
     * Removes and returns the first consumer event from event map
     * disconnect event have priority over connect event
     *
     * @param consumerId    the consumer id that need removed
     * @return              the first consumer removed from the event map
     */
    public ConsumerEvent removeFirst(String consumerId) {
        ConsumerEvent event = null;
        String group = consumerHolder.getGroupName(consumerId);
        boolean selDisConnMap = hasDisconnectEvent(group);
        ConcurrentHashMap<String, LinkedList<ConsumerEvent>> currentEventMap =
                selDisConnMap ? disconnectEventMap : connectEventMap;
        LinkedList<ConsumerEvent> eventList = currentEventMap.get(consumerId);
        if (eventList != null) {
            synchronized (eventList) {
                if (CollectionUtils.isNotEmpty(eventList)) {
                    event = eventList.removeFirst();
                    if (eventList.isEmpty()) {
                        currentEventMap.remove(consumerId);
                        if (selDisConnMap) {
                            MasterSrvStatsHolder.decSvrBalDisConConsumerCnt();
                        } else {
                            MasterSrvStatsHolder.decSvrBalConEventConsumerCnt();
                        }
                    }
                }
            }
        }
        if (event != null) {
            StringBuilder sBuilder = new StringBuilder(512);
            sBuilder.append("[Event Removed] ");
            logger.info(event.toStrBuilder(sBuilder).toString());
        }
        return event;
    }

    public int getUnfinishedCount(String groupName) {
        if (groupName == null) {
            return 0;
        }
        AtomicInteger unfinishedCount =
                groupUnfinishedCountMap.get(groupName);
        if (unfinishedCount == null) {
            return 0;
        }
        return unfinishedCount.get();
    }

    /**
     * Update the rounds of consumer groups dealing with balancing tasks
     *
     * @param groupHasUnfinishedEvent   the consumer groups dealing with balancing tasks
     */
    public void updateUnfinishedCountMap(Set<String> groupHasUnfinishedEvent) {
        if (groupHasUnfinishedEvent.isEmpty()) {
            groupUnfinishedCountMap.clear();
        } else {
            for (String oldGroup : groupUnfinishedCountMap.keySet()) {
                if (oldGroup != null) {
                    if (!groupHasUnfinishedEvent.contains(oldGroup)) {
                        groupUnfinishedCountMap.remove(oldGroup);
                    }
                }
            }
            for (String newGroup : groupHasUnfinishedEvent) {
                if (newGroup != null) {
                    AtomicInteger unfinishedCount =
                            groupUnfinishedCountMap.get(newGroup);
                    if (unfinishedCount == null) {
                        AtomicInteger newCount = new AtomicInteger(0);
                        unfinishedCount =
                                groupUnfinishedCountMap.putIfAbsent(newGroup, newCount);
                        if (unfinishedCount == null) {
                            unfinishedCount = newCount;
                        }
                    }
                    unfinishedCount.incrementAndGet();
                }
            }
        }
    }

    public void removeAll(String consumerId) {
        LinkedList<ConsumerEvent> eventInfos =
                disconnectEventMap.remove(consumerId);
        if (eventInfos != null) {
            MasterSrvStatsHolder.decSvrBalDisConConsumerCnt();
        }
        eventInfos = connectEventMap.remove(consumerId);
        if (eventInfos != null) {
            MasterSrvStatsHolder.decSvrBalConEventConsumerCnt();
        }
    }

    /**
     * Check if event map have event including disconnect event and connect event
     *
     * @return true if event map is not empty, otherwise false
     */
    public boolean hasEvent() {
        for (Map.Entry<String, LinkedList<ConsumerEvent>> entry
                : disconnectEventMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                return true;
            }
        }
        for (Map.Entry<String, LinkedList<ConsumerEvent>> entry
                : connectEventMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if disconnect event map have event
     *
     * @param group    the group name needs a query
     * @return true if disconnect event map not empty otherwise false
     */
    public boolean hasDisconnectEvent(String group) {
        List<String> consumerIdList =
                consumerHolder.getConsumerIdList(group);
        if (CollectionUtils.isNotEmpty(consumerIdList)) {
            for (String consumerId : consumerIdList) {
                if (consumerId == null) {
                    continue;
                }
                List<ConsumerEvent> eventList =
                        disconnectEventMap.get(consumerId);
                if (eventList != null) {
                    synchronized (eventList) {
                        if (CollectionUtils.isNotEmpty(eventList)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get all consumer id which have unfinished event
     *
     * @return consumer id set
     */
    public Set<String> getUnProcessedIdSet() {
        Set<String> consumerIdSet = new HashSet<>();
        for (Map.Entry<String, LinkedList<ConsumerEvent>> entry
                : disconnectEventMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                consumerIdSet.add(entry.getKey());
            }
        }
        for (Map.Entry<String, LinkedList<ConsumerEvent>> entry
                : connectEventMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                consumerIdSet.add(entry.getKey());
            }
        }
        return consumerIdSet;
    }

    public void clear() {
        disconnectEventMap.clear();
        connectEventMap.clear();
    }

    @Override
    public String toString() {
        return "ConsumerEventManager [disconnectEventMap=" + disconnectEventMap + ", connectEventMap="
                + connectEventMap + "]";
    }
}
