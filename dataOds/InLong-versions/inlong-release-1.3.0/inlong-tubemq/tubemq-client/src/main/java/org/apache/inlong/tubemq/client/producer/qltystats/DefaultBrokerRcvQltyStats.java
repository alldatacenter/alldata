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

package org.apache.inlong.tubemq.client.producer.qltystats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.corerpc.RpcServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of BrokerRcvQltyStats.
 */
public class DefaultBrokerRcvQltyStats implements BrokerRcvQltyStats {
    private static final Logger logger =
            LoggerFactory.getLogger(DefaultBrokerRcvQltyStats.class);
    private final TubeClientConfig clientConfig;
    private final RpcServiceFactory rpcServiceFactory;
    private final Thread statisticThread;
    // Broker link quality statistics.
    // Request failure analysis, by broker.
    private final ConcurrentHashMap<Integer, BrokerStatsItemSet> brokerStats =
            new ConcurrentHashMap<>();
    // The request number of the current broker.
    private final ConcurrentHashMap<Integer, AtomicLong> brokerCurSentReqNum =
            new ConcurrentHashMap<>();
    // The statistics of the blocking brokers.
    private final ConcurrentHashMap<Integer, Long> brokerForbiddenMap =
            new ConcurrentHashMap<>();
    // Status:
    // -1: Uninitialized
    // 0: Running
    // 1:Stopped
    private final AtomicInteger statusId = new AtomicInteger(-1);
    private long lastPrintTime = System.currentTimeMillis();
    // Total sent request number.
    private final AtomicLong curTotalSentRequestNum = new AtomicLong(0);
    // The time of last link quality statistic.
    private long lastLinkStatisticTime = System.currentTimeMillis();
    // Analyze the broker quality based on the request response. We calculate the quality metric by
    //     success response number / total request number
    // in a time range. The time range is same when we compare the quality of different brokers.
    // We think the quality is better when the successful ratio is higher. The bad quality brokers
    // will be blocked. The blocking ratio and time can be configured.
    private List<Map.Entry<Integer, BrokerStatsDltTuple>> cachedLinkQualities =
            new ArrayList<>();
    private long lastQualityStatisticTime = System.currentTimeMillis();
    private long printCount = 0;

    /**
     * Initial a broker receive status statistics ojbect
     *
     * @param rpcServiceFactory  the session factory
     * @param producerConfig     the producer configure
     */
    public DefaultBrokerRcvQltyStats(final RpcServiceFactory rpcServiceFactory,
                                     final TubeClientConfig producerConfig) {
        this.clientConfig = producerConfig;
        this.rpcServiceFactory = rpcServiceFactory;
        this.statisticThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isStopped()) {
                    try {
                        statisticDltBrokerStatus();
                    } catch (Throwable e) {
                        //
                    }
                    ThreadUtils.sleep(1000);
                }
            }
        }, "Sent Statistic Thread");
        this.statisticThread.setPriority(Thread.MAX_PRIORITY);
    }

    /**
     * Start the broker statistic thread.
     */
    public void startBrokerStatistic() {
        if (this.statusId.compareAndSet(-1, 0)) {
            this.statisticThread.start();
        }
    }

    /**
     * Check if the statistic thread is stopped.
     *
     * @return status
     */
    public boolean isStopped() {
        return (this.statusId.get() > 0);
    }

    /**
     * Get the partitions of allowed broker.
     *
     * @param brokerPartList broker partition mapping
     * @return partition list
     * @throws TubeClientException  the exception while query
     */
    @Override
    public List<Partition> getAllowedBrokerPartitions(
            Map<Integer, List<Partition>> brokerPartList) throws TubeClientException {
        // #lizard forgives
        List<Partition> partList = new ArrayList<>();
        if ((brokerPartList == null) || (brokerPartList.isEmpty())) {
            throw new TubeClientException("Null brokers to select sent, please try later!");
        }
        long currentWaitCount = this.curTotalSentRequestNum.get();
        if (currentWaitCount >= this.clientConfig.getSessionMaxAllowedDelayedMsgCount()) {
            throw new TubeClientException(new StringBuilder(512)
                    .append("Current delayed messages over max allowed count, allowed is ")
                    .append(this.clientConfig.getSessionMaxAllowedDelayedMsgCount())
                    .append(", current count is ").append(currentWaitCount).toString());
        }
        long curTime = System.currentTimeMillis();
        Set<Integer> allowedBrokerIds = new HashSet<>();
        ConcurrentHashMap<Integer, Long> unAvailableBrokerMap = rpcServiceFactory.getUnavailableBrokerMap();
        for (Map.Entry<Integer, List<Partition>> oldBrokerPartEntry : brokerPartList.entrySet()) {
            Long lastAddTime = unAvailableBrokerMap.get(oldBrokerPartEntry.getKey());
            if ((lastAddTime != null)
                && (curTime - lastAddTime <= clientConfig.getUnAvailableFbdDurationMs())) {
                continue;
            }
            if (this.brokerForbiddenMap.containsKey(oldBrokerPartEntry.getKey())) {
                continue;
            }
            List<Partition> partitionList = oldBrokerPartEntry.getValue();
            if ((partitionList != null) && !partitionList.isEmpty()) {
                Partition partition = partitionList.get(0);
                if (partition != null) {
                    BrokerInfo brokerInfo = partition.getBroker();
                    AtomicLong curMaxSentNum =
                            this.brokerCurSentReqNum.get(brokerInfo.getBrokerId());
                    if ((curMaxSentNum != null)
                            && (curMaxSentNum.get() > this.clientConfig.getLinkMaxAllowedDelayedMsgCount())) {
                        continue;
                    }
                    if (!rpcServiceFactory.isRemoteAddrForbidden(brokerInfo.getBrokerAddr())) {
                        allowedBrokerIds.add(brokerInfo.getBrokerId());
                    }
                }
            }
        }
        if (allowedBrokerIds.isEmpty()) {
            throw new TubeClientException("The brokers of topic are all forbidden!");
        }
        int selectCount = allowedBrokerIds.size();
        int allowedCount = selectCount;
        if (currentWaitCount > this.clientConfig.getSessionWarnDelayedMsgCount()) {
            allowedCount =
                    (int) Math.rint(selectCount * (1 - this.clientConfig.getSessionWarnForbiddenRate()));
        }
        if ((this.cachedLinkQualities.isEmpty()) || (selectCount == allowedCount)) {
            for (Integer selBrokerId : allowedBrokerIds) {
                partList.addAll(brokerPartList.get(selBrokerId));
            }
        } else {
            List<Integer> cachedBrokerIds = new ArrayList<>();
            for (Map.Entry<Integer, BrokerStatsDltTuple> brokerEntry : this.cachedLinkQualities) {
                cachedBrokerIds.add(brokerEntry.getKey());
            }
            for (Integer selBrokerId : allowedBrokerIds) {
                if (!cachedBrokerIds.contains(selBrokerId)) {
                    partList.addAll(brokerPartList.get(selBrokerId));
                    allowedCount--;
                }
                if (allowedCount <= 0) {
                    break;
                }
            }
            if (allowedCount > 0) {
                for (Map.Entry<Integer, BrokerStatsDltTuple> brokerEntry :
                        this.cachedLinkQualities) {
                    if (allowedBrokerIds.contains(brokerEntry.getKey())) {
                        partList.addAll(brokerPartList.get(brokerEntry.getKey()));
                        allowedCount--;
                    }
                    if (allowedCount <= 0) {
                        break;
                    }
                }
            }
        }
        return partList;
    }

    /**
     * Remove a registered broker from the statistic list.
     *
     * @param registeredBrokerIdList   the broker id need to delete
     */
    @Override
    public void removeUnRegisteredBroker(List<Integer> registeredBrokerIdList) {
        for (Integer curBrokerId : brokerStats.keySet()) {
            if (!registeredBrokerIdList.contains(curBrokerId)) {
                brokerStats.remove(curBrokerId);
            }
        }
    }

    @Override
    public void statisticDltBrokerStatus() {
        // #lizard forgives
        long currentTime = System.currentTimeMillis();
        if ((currentTime - this.lastLinkStatisticTime
                < this.clientConfig.getSessionStatisticCheckDuration())
                && (currentTime - this.lastQualityStatisticTime
                < this.clientConfig.getMaxForbiddenCheckDuration())) {
            return;
        }
        if (currentTime - this.lastLinkStatisticTime
                > this.clientConfig.getSessionStatisticCheckDuration()) {
            this.lastLinkStatisticTime = System.currentTimeMillis();
            this.cachedLinkQualities = getCurBrokerSentWaitStats();
        }
        if (System.currentTimeMillis() - this.lastQualityStatisticTime
                < this.clientConfig.getMaxForbiddenCheckDuration()) {
            return;
        }
        StringBuilder sBuilder = new StringBuilder(512);
        this.lastQualityStatisticTime = System.currentTimeMillis();
        if ((printCount++ % 10 == 0) && (!brokerStats.isEmpty())) {
            if (!brokerForbiddenMap.isEmpty()) {
                logger.info(sBuilder.append("[status check]: current response quality respForbiddenMap is ")
                        .append(brokerForbiddenMap.toString()).toString());
                sBuilder.delete(0, sBuilder.length());
            }
            if (!rpcServiceFactory.getForbiddenAddrMap().isEmpty()) {
                logger.info(sBuilder.append("[status check]: current request quality reqForbiddenMap is ")
                        .append(rpcServiceFactory.getForbiddenAddrMap().toString()).toString());
                sBuilder.delete(0, sBuilder.length());
            }
            if (!rpcServiceFactory.getUnavailableBrokerMap().isEmpty()) {
                logger.info(sBuilder.append("[status check]: current service unavailable brokerMap is ")
                    .append(rpcServiceFactory.getUnavailableBrokerMap().toString()).toString());
                sBuilder.delete(0, sBuilder.length());
            }
        }

        boolean changed = false;
        long totalSuccRecNum = 0L;
        HashMap<Integer, BrokerStatsDltTuple> needSelNumTMap =
                new HashMap<>();
        for (Map.Entry<Integer, BrokerStatsItemSet> brokerForbiddenEntry : brokerStats.entrySet()) {
            BrokerStatsItemSet curStatsItemSet = brokerStats.get(brokerForbiddenEntry.getKey());
            if (curStatsItemSet != null) {
                long sendNum = curStatsItemSet.getDltAndSnapshotSendNum();
                long succRecvNum = curStatsItemSet.getDltAndSnapshotRecSucNum();
                if (!brokerForbiddenMap.containsKey(brokerForbiddenEntry.getKey())) {
                    totalSuccRecNum += succRecvNum;
                    needSelNumTMap.put(brokerForbiddenEntry.getKey(),
                            new BrokerStatsDltTuple(succRecvNum, sendNum));
                }
            }
        }
        for (Map.Entry<Integer, Long> brokerForbiddenEntry : brokerForbiddenMap.entrySet()) {
            if (System.currentTimeMillis() - brokerForbiddenEntry.getValue()
                    > this.clientConfig.getMaxForbiddenCheckDuration()) {
                changed = true;
                brokerForbiddenMap.remove(brokerForbiddenEntry.getKey());
            }
        }
        if (needSelNumTMap.isEmpty()) {
            if (changed) {
                if (!brokerForbiddenMap.isEmpty()) {
                    logger.info(sBuilder.append("End statistic 1: forbidden Broker Set is ")
                            .append(brokerForbiddenMap.toString()).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
            }
            return;
        }
        List<Map.Entry<Integer, BrokerStatsDltTuple>> lstData =
                new ArrayList<>(needSelNumTMap.entrySet());
        // Sort the list in ascending order
        Collections.sort(lstData, new BrokerStatsDltTupleComparator(false));
        int filteredBrokerListSize = lstData.size();
        int needHoldCount =
                (int) Math.rint((filteredBrokerListSize + brokerForbiddenMap.size())
                        * clientConfig.getMaxSentForbiddenRate());
        needHoldCount -= brokerForbiddenMap.size();
        if (needHoldCount <= 0) {
            if (changed) {
                if (!brokerForbiddenMap.isEmpty()) {
                    logger.info(sBuilder.append("End statistic 2: forbidden Broker Set is ")
                            .append(brokerForbiddenMap.toString()).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
            }
            return;
        }
        long avgSuccRecNumThreshold = 0L;
        if (filteredBrokerListSize <= 3) {
            totalSuccRecNum -= lstData.get(0).getValue().getSuccRecvNum();
            avgSuccRecNumThreshold = (long) (totalSuccRecNum / (filteredBrokerListSize - 1) * 0.2);
        } else {
            totalSuccRecNum -= lstData.get(0).getValue().getSuccRecvNum();
            totalSuccRecNum -= lstData.get(1).getValue().getSuccRecvNum();
            totalSuccRecNum -= lstData.get(lstData.size() - 1).getValue().getSuccRecvNum();
            avgSuccRecNumThreshold = (long) (totalSuccRecNum / (filteredBrokerListSize - 3) * 0.2);
        }
        ConcurrentHashMap<Integer, Boolean> tmpBrokerForbiddenMap =
                new ConcurrentHashMap<>();
        for (Map.Entry<Integer, BrokerStatsDltTuple> brokerDltNumEntry : lstData) {
            long succRecvNum = brokerDltNumEntry.getValue().getSuccRecvNum();
            long succSendNumThreshold = (long) (brokerDltNumEntry.getValue().getSendNum() * 0.1);
            if ((succRecvNum < avgSuccRecNumThreshold) && (succSendNumThreshold > 2)
                    && (succRecvNum < succSendNumThreshold)) {
                tmpBrokerForbiddenMap.put(brokerDltNumEntry.getKey(), true);
                if (logger.isDebugEnabled()) {
                    logger.debug(sBuilder.append("[forbidden statistic] brokerId=")
                        .append(brokerDltNumEntry.getKey()).append(",succRecvNum=")
                        .append(succRecvNum).append(",avgSuccRecNumThreshold=")
                        .append(avgSuccRecNumThreshold).append(",succSendNumThreshold=")
                        .append(succSendNumThreshold).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
            }
            if ((tmpBrokerForbiddenMap.size() >= needHoldCount)
                    || (succRecvNum >= avgSuccRecNumThreshold)) {
                break;
            }
        }
        for (Integer tmpBrokerId : tmpBrokerForbiddenMap.keySet()) {
            changed = true;
            brokerForbiddenMap.put(tmpBrokerId, System.currentTimeMillis());
        }
        if (changed) {
            if (!brokerForbiddenMap.isEmpty()) {
                logger.info(sBuilder.append("End statistic 3: forbidden Broker Set is ")
                        .append(brokerForbiddenMap.toString()).toString());
                sBuilder.delete(0, sBuilder.length());
            }
        }
    }

    private List<Map.Entry<Integer, BrokerStatsDltTuple>> getCurBrokerSentWaitStats() {
        HashMap<Integer, BrokerStatsDltTuple> needSelNumTMap = new HashMap<>();
        for (Map.Entry<Integer, BrokerStatsItemSet> brokerForbiddenEntry : brokerStats.entrySet()) {
            BrokerStatsItemSet curStatsItemSet = brokerForbiddenEntry.getValue();
            long num = curStatsItemSet.getSendNum() - curStatsItemSet.getReceiveNum();
            if (num < this.clientConfig.getLinkMaxAllowedDelayedMsgCount()) {
                needSelNumTMap.put(brokerForbiddenEntry.getKey(), new BrokerStatsDltTuple(num,
                        curStatsItemSet.getSendNum()));
            }
        }
        List<Map.Entry<Integer, BrokerStatsDltTuple>> lstData =
                new ArrayList<>(needSelNumTMap.entrySet());
        // Sort the list in descending order
        Collections.sort(lstData, new BrokerStatsDltTupleComparator(true));
        return lstData;
    }

    @Override
    public void addSendStatistic(int brokerId) {
        BrokerStatsItemSet curStatsItemSet = brokerStats.get(brokerId);
        if (curStatsItemSet == null) {
            BrokerStatsItemSet newStatsItemSet = new BrokerStatsItemSet();
            curStatsItemSet = brokerStats.putIfAbsent(brokerId, newStatsItemSet);
            if (curStatsItemSet == null) {
                curStatsItemSet = newStatsItemSet;
            }
        }
        curStatsItemSet.incrementAndGetSendNum();
        AtomicLong curBrokerNum = brokerCurSentReqNum.get(brokerId);
        if (curBrokerNum == null) {
            AtomicLong tmpCurBrokerNum = new AtomicLong(0);
            curBrokerNum = brokerCurSentReqNum.putIfAbsent(brokerId, tmpCurBrokerNum);
            if (curBrokerNum == null) {
                curBrokerNum = tmpCurBrokerNum;
            }
        }
        curBrokerNum.incrementAndGet();
        this.curTotalSentRequestNum.incrementAndGet();
    }

    @Override
    public void addReceiveStatistic(int brokerId, boolean isSuccess) {
        BrokerStatsItemSet curStatsItemSet = brokerStats.get(brokerId);
        if (curStatsItemSet != null) {
            curStatsItemSet.incrementAndGetRecNum();
            if (isSuccess) {
                curStatsItemSet.incrementAndGetRecSucNum();
            }
            AtomicLong curBrokerNum = brokerCurSentReqNum.get(brokerId);
            if (curBrokerNum != null) {
                curBrokerNum.decrementAndGet();
            }
            this.curTotalSentRequestNum.decrementAndGet();
        }
    }

    @Override
    public void stopBrokerStatistic() {
        if (this.statusId.get() != 0) {
            return;
        }
        if (this.statusId.compareAndSet(0, 1)) {
            try {
                this.statisticThread.interrupt();
            } catch (Throwable e) {
                //
            }
        }
    }

    @Override
    public String toString() {
        return "lastStatisticTime:" + this.lastLinkStatisticTime + TokenConstants.ATTR_SEP
                + ",lastPrintTime:" + this.lastPrintTime + TokenConstants.ATTR_SEP
                + ",producerMaxSentStatsScanDuration:" + this.clientConfig.getMaxForbiddenCheckDuration()
                + TokenConstants.ATTR_SEP + ",linkMaxAllowedDelayedMsgCount:"
                + this.clientConfig.getLinkMaxAllowedDelayedMsgCount() + TokenConstants.ATTR_SEP
                + ",brokerStats:" + this.brokerStats.toString() + TokenConstants.ATTR_SEP
                + ",brokerForbiddenMap:" + this.brokerForbiddenMap.toString();
    }

    private static class BrokerStatsDltTupleComparator
            implements Comparator<Map.Entry<Integer, BrokerStatsDltTuple>> {
        // Descending sort or ascending sort
        private boolean isDescSort = true;

        public BrokerStatsDltTupleComparator(boolean isDescSort) {
            this.isDescSort = isDescSort;
        }

        @Override
        public int compare(final Map.Entry<Integer, BrokerStatsDltTuple> o1,
                           final Map.Entry<Integer, BrokerStatsDltTuple> o2) {
            if (o1.getValue().getSuccRecvNum() == o2.getValue().getSuccRecvNum()) {
                return 0;
            } else {
                if (o1.getValue().getSuccRecvNum() > o2.getValue().getSuccRecvNum()) {
                    return this.isDescSort ? -1 : 1;
                } else {
                    return this.isDescSort ? 1 : -1;
                }
            }
        }
    }
}
