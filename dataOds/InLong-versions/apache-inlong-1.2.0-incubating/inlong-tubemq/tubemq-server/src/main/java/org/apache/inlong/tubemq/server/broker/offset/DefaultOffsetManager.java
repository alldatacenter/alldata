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

package org.apache.inlong.tubemq.server.broker.offset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.daemon.AbstractDaemonService;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.broker.BrokerConfig;
import org.apache.inlong.tubemq.server.broker.msgstore.MessageStore;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.broker.offset.offsetstorage.OffsetStorage;
import org.apache.inlong.tubemq.server.broker.offset.offsetstorage.OffsetStorageInfo;
import org.apache.inlong.tubemq.server.broker.offset.offsetstorage.ZkOffsetStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default offset manager.
 * Conduct consumer's commit offset operation and consumer's offset that has consumed but not committed.
 */
public class DefaultOffsetManager extends AbstractDaemonService implements OffsetService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultOffsetManager.class);
    private final BrokerConfig brokerConfig;
    private final OffsetStorage zkOffsetStorage;
    private final ConcurrentHashMap<String/* group */,
            ConcurrentHashMap<String/* topic - partitionId*/, OffsetStorageInfo>> cfmOffsetMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* group */,
            ConcurrentHashMap<String/* topic - partitionId*/, Long>> tmpOffsetMap =
            new ConcurrentHashMap<>();

    public DefaultOffsetManager(final BrokerConfig brokerConfig) {
        super("[Offset Manager]", brokerConfig.getZkConfig().getZkCommitPeriodMs());
        this.brokerConfig = brokerConfig;
        zkOffsetStorage = new ZkOffsetStorage(brokerConfig.getZkConfig(),
                true, brokerConfig.getBrokerId());
        super.start();
    }

    @Override
    protected void loopProcess(long intervalMs) {
        while (!super.isStopped()) {
            try {
                Thread.sleep(intervalMs);
                commitCfmOffsets(false);
            } catch (InterruptedException e) {
                logger.warn("[Offset Manager] Daemon commit thread has been interrupted");
                return;
            } catch (Throwable t) {
                logger.error("[Offset Manager] Daemon commit thread throw error ", t);
            }
        }
    }

    @Override
    public void close(long waitTimeMs) {
        if (super.stop()) {
            return;
        }
        logger.info("[Offset Manager] begin reserve temporary Offset.....");
        this.commitTmpOffsets();
        logger.info("[Offset Manager] begin reserve final Offset.....");
        this.commitCfmOffsets(true);
        this.zkOffsetStorage.close();
        logger.info("[Offset Manager] Offset Manager service stopped!");
    }

    /**
     * Load offset.
     *
     * @param msgStore       the message store instance
     * @param group          the consume group name
     * @param topic          the consumed topic name
     * @param partitionId    the consumed partition id
     * @param readStatus     the consume behavior of the consumer group
     * @param reqOffset      the bootstrap offset
     * @param sBuilder       the string buffer
     * @return               the loaded offset information
     */
    @Override
    public OffsetStorageInfo loadOffset(final MessageStore msgStore, final String group,
                                        final String topic, int partitionId, int readStatus,
                                        long reqOffset, final StringBuilder sBuilder) {
        OffsetStorageInfo regInfo;
        long indexMaxOffset = msgStore.getIndexMaxOffset();
        long indexMinOffset = msgStore.getIndexMinOffset();
        long defOffset =
                (readStatus == TBaseConstants.CONSUME_MODEL_READ_NORMAL)
                        ? indexMinOffset : indexMaxOffset;
        String offsetCacheKey = getOffsetCacheKey(topic, partitionId);
        regInfo = loadOrCreateOffset(group, topic, partitionId, offsetCacheKey, defOffset);
        getAndResetTmpOffset(group, offsetCacheKey);
        final long curOffset = regInfo.getOffset();
        final boolean isFirstCreate = regInfo.isFirstCreate();
        if ((reqOffset >= 0)
                || (readStatus == TBaseConstants.CONSUME_MODEL_READ_FROM_MAX_ALWAYS)) {
            long adjOffset = indexMaxOffset;
            if (readStatus != TBaseConstants.CONSUME_MODEL_READ_FROM_MAX_ALWAYS) {
                adjOffset = MixedUtils.mid(reqOffset, indexMinOffset, indexMaxOffset);
            }
            regInfo.getAndSetOffset(adjOffset);
        }
        sBuilder.append("[Offset Manager]");
        switch (readStatus) {
            case TBaseConstants.CONSUME_MODEL_READ_FROM_MAX_ALWAYS:
                sBuilder.append(" Consume From Max Offset Always");
                break;
            case TBaseConstants.CONSUME_MODEL_READ_FROM_MAX:
                sBuilder.append(" Consume From Max Offset");
                break;
            default: {
                sBuilder.append(" Normal Offset");
            }
        }
        if (!isFirstCreate) {
            sBuilder.append(",Continue");
        }
        long currentOffset = regInfo.getOffset();
        long offsetDelta = indexMaxOffset - currentOffset;
        logger.info(sBuilder.append(",requestOffset=").append(reqOffset)
                .append(",loadedOffset=").append(curOffset)
                .append(",currentOffset=").append(currentOffset)
                .append(",maxOffset=").append(indexMaxOffset)
                .append(",offsetDelta=").append(offsetDelta)
                .append(",lagLevel=").append(getLagLevel(offsetDelta))
                .append(",group=").append(group)
                .append(",topic-part=").append(offsetCacheKey).toString());
        sBuilder.delete(0, sBuilder.length());
        return regInfo;
    }

    /**
     * Get offset by parameters.
     *
     * @param msgStore       the message store instance
     * @param group          the consume group name
     * @param topic          the consumed topic name
     * @param partitionId    the consumed partition id
     * @param isManCommit    whether manual commit offset
     * @param lastConsumed   whether the latest fetch is consumed
     * @param sb             the string buffer
     * @return               the current offset
     */
    @Override
    public long getOffset(final MessageStore msgStore, final String group,
                          final String topic, int partitionId,
                          boolean isManCommit, boolean lastConsumed,
                          final StringBuilder sb) {
        String offsetCacheKey = getOffsetCacheKey(topic, partitionId);
        OffsetStorageInfo regInfo =
                loadOrCreateOffset(group, topic, partitionId, offsetCacheKey, 0);
        long requestOffset = regInfo.getOffset();
        if (isManCommit) {
            requestOffset = requestOffset + getTmpOffset(group, topic, partitionId);
        } else {
            if (lastConsumed) {
                requestOffset = commitOffset(group, topic, partitionId, true);
            }
        }
        final long maxOffset = msgStore.getIndexMaxOffset();
        final long minOffset = msgStore.getIndexMinOffset();
        if (requestOffset >= maxOffset) {
            if (requestOffset > maxOffset && brokerConfig.isUpdateConsumerOffsets()) {
                logger.warn(sb
                        .append("[Offset Manager] requestOffset is bigger than maxOffset, reset! requestOffset=")
                        .append(requestOffset).append(",maxOffset=").append(maxOffset)
                        .append(",group=").append(group)
                        .append(",topic-part=").append(offsetCacheKey).toString());
                sb.delete(0, sb.length());
                setTmpOffset(group, offsetCacheKey, maxOffset - requestOffset);
                if (!isManCommit) {
                    requestOffset = commitOffset(group, topic, partitionId, true);
                }
            }
            return -requestOffset;
        } else if (requestOffset < minOffset) {
            logger.warn(sb
                    .append("[Offset Manager] requestOffset is lower than minOffset, reset! requestOffset=")
                    .append(requestOffset).append(",minOffset=").append(minOffset)
                    .append(",group=").append(group)
                    .append(",topic-part=").append(offsetCacheKey).toString());
            sb.delete(0, sb.length());
            setTmpOffset(group, offsetCacheKey, minOffset - requestOffset);
            requestOffset = commitOffset(group, topic, partitionId, true);
        }
        return requestOffset;
    }

    @Override
    public long getOffset(final String group, final String topic, int partitionId) {
        OffsetStorageInfo regInfo =
                loadOrCreateOffset(group, topic, partitionId,
                        getOffsetCacheKey(topic, partitionId), 0);
        return regInfo.getOffset();
    }

    @Override
    public void bookOffset(final String group, final String topic, int partitionId,
                           int readDalt, boolean isManCommit, boolean isMsgEmpty,
                           final StringBuilder sb) {
        if (readDalt == 0) {
            return;
        }
        String offsetCacheKey = getOffsetCacheKey(topic, partitionId);
        if (isManCommit) {
            long tmpOffset = getTmpOffset(group, topic, partitionId);
            setTmpOffset(group, offsetCacheKey, readDalt + tmpOffset);
        } else {
            setTmpOffset(group, offsetCacheKey, readDalt);
            if (isMsgEmpty) {
                commitOffset(group, topic, partitionId, true);
            }
        }
    }

    /**
     * Commit offset.
     *
     * @param group         the consume group name
     * @param topic         the consumed topic name
     * @param partitionId   the consumed partition id
     * @param isConsumed    whether the latest fetch is consumed
     * @return              the current offset
     */
    @Override
    public long commitOffset(final String group, final String topic,
                             int partitionId, boolean isConsumed) {
        long updatedOffset;
        String offsetCacheKey = getOffsetCacheKey(topic, partitionId);
        long tmpOffset = getAndResetTmpOffset(group, offsetCacheKey);
        if (!isConsumed) {
            tmpOffset = 0;
        }
        OffsetStorageInfo regInfo =
                loadOrCreateOffset(group, topic, partitionId, offsetCacheKey, 0);
        if ((tmpOffset == 0) && (!regInfo.isFirstCreate())) {
            updatedOffset = regInfo.getOffset();
            return updatedOffset;
        }
        updatedOffset = regInfo.addAndGetOffset(tmpOffset);
        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuilder(512)
                    .append("[Offset Manager] Update offset finished, offset=").append(updatedOffset)
                    .append(",group=").append(group).append(",topic-part=").append(offsetCacheKey).toString());
        }
        return updatedOffset;
    }

    /**
     * Reset offset.
     *
     * @param store          the message store instance
     * @param group          the consume group name
     * @param topic          the consumed topic name
     * @param partitionId    the consumed partition id
     * @param reSetOffset    the reset offset
     * @param modifier       the modifier
     * @return               the current offset
     */
    @Override
    public long resetOffset(final MessageStore store, final String group,
                            final String topic, int partitionId,
                            long reSetOffset, final String modifier) {
        long oldOffset = -1;
        if (store != null) {
            long indexMaxOffset = store.getIndexMaxOffset();
            reSetOffset = MixedUtils.mid(reSetOffset, store.getIndexMinOffset(), indexMaxOffset);
            String offsetCacheKey = getOffsetCacheKey(topic, partitionId);
            getAndResetTmpOffset(group, offsetCacheKey);
            OffsetStorageInfo regInfo =
                    loadOrCreateOffset(group, topic, partitionId, offsetCacheKey, 0);
            oldOffset = regInfo.getAndSetOffset(reSetOffset);
            long currentOffset = regInfo.getOffset();
            long offsetDelta = indexMaxOffset - currentOffset;
            logger.info(new StringBuilder(512)
                    .append("[Offset Manager] Manual update offset by modifier=")
                    .append(modifier).append(",resetOffset=").append(reSetOffset)
                    .append(",loadedOffset=").append(oldOffset)
                    .append(",currentOffset=").append(currentOffset)
                    .append(",maxOffset=").append(indexMaxOffset)
                    .append(",offsetDelta=").append(offsetDelta)
                    .append(",lagLevel=").append(getLagLevel(offsetDelta))
                    .append(",group=").append(group)
                    .append(",topic-part=").append(offsetCacheKey).toString());
        }
        return oldOffset;
    }

    /**
     * Get temp offset.
     *
     * @param group          the consume group name
     * @param topic          the consumed topic name
     * @param partitionId    the consumed partition id
     * @return               the inflight offset
     */
    @Override
    public long getTmpOffset(final String group, final String topic, int partitionId) {
        String offsetCacheKey = getOffsetCacheKey(topic, partitionId);
        ConcurrentHashMap<String, Long> partTmpOffsetMap = tmpOffsetMap.get(group);
        if (partTmpOffsetMap != null) {
            Long tmpOffset = partTmpOffsetMap.get(offsetCacheKey);
            if (tmpOffset == null) {
                return 0;
            } else {
                return tmpOffset - tmpOffset % DataStoreUtils.STORE_INDEX_HEAD_LEN;
            }
        }
        return 0;
    }

    /**
     * Get in-memory and in zk group set
     *
     * @return booked group in memory and in zk
     */
    @Override
    public Set<String> getBookedGroups() {
        Set<String> groupSet =
                new HashSet<>(cfmOffsetMap.keySet());
        Map<String, Set<String>> localGroups =
                zkOffsetStorage.queryZkAllGroupTopicInfos();
        groupSet.addAll(localGroups.keySet());
        return groupSet;
    }

    /**
     * Get in-memory group set
     *
     * @return booked group in memory
     */
    public Set<String> getInMemoryGroups() {
        return new HashSet<>(cfmOffsetMap.keySet());
    }

    /**
     * Get in-zookeeper but not in memory's group set
     *
     * @return booked group in zookeeper
     */
    @Override
    public Set<String> getUnusedGroupInfo() {
        Set<String> unUsedGroups = new HashSet<>();
        Map<String, Set<String>> localGroups =
                zkOffsetStorage.queryZkAllGroupTopicInfos();
        for (String groupName : localGroups.keySet()) {
            if (!cfmOffsetMap.containsKey(groupName)) {
                unUsedGroups.add(groupName);
            }
        }
        return unUsedGroups;
    }

    /**
     * Get the topic set subscribed by the consumer group
     *
     * @param group    the queries group name
     * @return         the topic set subscribed
     */
    @Override
    public Set<String> getGroupSubInfo(String group) {
        Set<String> result = new HashSet<>();
        Map<String, OffsetStorageInfo> topicPartOffsetMap = cfmOffsetMap.get(group);
        if (topicPartOffsetMap == null) {
            List<String> groupLst = new ArrayList<>(1);
            groupLst.add(group);
            Map<String, Set<String>> groupTopicInfo =
                    zkOffsetStorage.queryZKGroupTopicInfo(groupLst);
            result = groupTopicInfo.get(group);
        } else {
            for (OffsetStorageInfo storageInfo : topicPartOffsetMap.values()) {
                result.add(storageInfo.getTopic());
            }
        }
        return result;
    }

    /**
     * Get group's offset by Specified topic-partitions
     *
     * @param group           the consume group that to query
     * @param topicPartMap    the topic partition map that to query
     * @return group offset info in memory or zk
     */
    @Override
    public Map<String, Map<Integer, Tuple2<Long, Long>>> queryGroupOffset(
            String group, Map<String, Set<Integer>> topicPartMap) {
        Map<String, Map<Integer, Tuple2<Long, Long>>> result = new HashMap<>();
        // search group from memory
        Map<String, OffsetStorageInfo> topicPartOffsetMap = cfmOffsetMap.get(group);
        if (topicPartOffsetMap == null) {
            // query from zookeeper
            for (Map.Entry<String, Set<Integer>> entry : topicPartMap.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                Map<Integer, Long> qryResult =
                        zkOffsetStorage.queryGroupOffsetInfo(group,
                                entry.getKey(), entry.getValue());
                Map<Integer, Tuple2<Long, Long>> offsetMap = new HashMap<>();
                for (Map.Entry<Integer, Long> item : qryResult.entrySet()) {
                    if (item == null || item.getKey() == null || item.getValue() == null)  {
                        continue;
                    }
                    offsetMap.put(item.getKey(), new Tuple2<>(item.getValue(), 0L));
                }
                if (!offsetMap.isEmpty()) {
                    result.put(entry.getKey(), offsetMap);
                }
            }
        } else {
            // found in memory, get offset values
            Map<String, Long> tmpPartOffsetMap = tmpOffsetMap.get(group);
            for (Map.Entry<String, Set<Integer>> entry : topicPartMap.entrySet()) {
                Map<Integer, Tuple2<Long, Long>> offsetMap = new HashMap<>();
                for (Integer partitionId : entry.getValue()) {
                    String offsetCacheKey =
                            getOffsetCacheKey(entry.getKey(), partitionId);
                    OffsetStorageInfo offsetInfo = topicPartOffsetMap.get(offsetCacheKey);
                    Long tmpOffset = tmpPartOffsetMap.get(offsetCacheKey);
                    if (tmpOffset == null) {
                        tmpOffset = 0L;
                    }
                    if (offsetInfo != null) {
                        offsetMap.put(partitionId,
                                new Tuple2<>(offsetInfo.getOffset(), tmpOffset));
                    }
                }
                if (!offsetMap.isEmpty()) {
                    result.put(entry.getKey(), offsetMap);
                }
            }
        }
        return result;
    }

    /**
     * Get online groups' offset information
     *
     * @return group offset info in memory or zk
     */
    @Override
    public Map<String, OffsetRecordInfo> getOnlineGroupOffsetInfo() {
        Map<String, OffsetRecordInfo> result = new HashMap<>();
        for (Map.Entry<String,
                ConcurrentHashMap<String, OffsetStorageInfo>> entry : cfmOffsetMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            // read offset map information
            Map<String, OffsetStorageInfo> storeMap = entry.getValue();
            if (storeMap.isEmpty()) {
                continue;
            }
            for (OffsetStorageInfo storageInfo : storeMap.values()) {
                if (storageInfo == null) {
                    continue;
                }
                OffsetRecordInfo recordInfo = result.get(entry.getKey());
                if (recordInfo == null) {
                    recordInfo = new OffsetRecordInfo(
                            brokerConfig.getBrokerId(), entry.getKey());
                    result.put(entry.getKey(), recordInfo);
                }
                recordInfo.addCfmOffsetInfo(storageInfo.getTopic(),
                        storageInfo.getPartitionId(), storageInfo.getOffset());
            }
        }
        return result;
    }

    /**
     * Reset offset.
     *
     * @param groups              the groups to reset offset
     * @param topicPartOffsets    the reset offset information
     * @param modifier            the modifier
     * @return at least one record modified
     */
    @Override
    public boolean modifyGroupOffset(Set<String> groups,
                                     List<Tuple3<String, Integer, Long>> topicPartOffsets,
                                     String modifier) {
        long oldOffset;
        boolean changed = false;
        String offsetCacheKey;
        StringBuilder strBuff = new StringBuilder(512);
        // set offset by group
        for (String group : groups) {
            for (Tuple3<String, Integer, Long> tuple3 : topicPartOffsets) {
                if (tuple3 == null
                        || tuple3.getF0() == null
                        || tuple3.getF1() == null
                        || tuple3.getF2() == null) {
                    continue;
                }
                // set offset value
                offsetCacheKey = getOffsetCacheKey(tuple3.getF0(), tuple3.getF1());
                getAndResetTmpOffset(group, offsetCacheKey);
                OffsetStorageInfo regInfo = loadOrCreateOffset(group,
                        tuple3.getF0(), tuple3.getF1(), offsetCacheKey, 0);
                oldOffset = regInfo.getAndSetOffset(tuple3.getF2());
                changed = true;
                logger.info(strBuff
                        .append("[Offset Manager] Update offset by modifier=")
                        .append(modifier).append(",resetOffset=").append(tuple3.getF2())
                        .append(",loadedOffset=").append(oldOffset)
                        .append(",currentOffset=").append(regInfo.getOffset())
                        .append(",group=").append(group)
                        .append(",topic-part=").append(offsetCacheKey).toString());
                strBuff.delete(0, strBuff.length());
            }
        }
        return changed;
    }

    /**
     * Delete offset.
     *
     * @param onlyMemory          whether only memory cached value
     * @param groupTopicPartMap   need removed group and topic map
     * @param modifier            the modifier
     */
    @Override
    public void deleteGroupOffset(boolean onlyMemory,
                                  Map<String, Map<String, Set<Integer>>> groupTopicPartMap,
                                  String modifier) {
        String printBase;
        StringBuilder strBuff = new StringBuilder(512);
        for (Map.Entry<String, Map<String, Set<Integer>>> entry
                : groupTopicPartMap.entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null
                    || entry.getValue().isEmpty()) {
                continue;
            }
            rmvOffset(entry.getKey(), entry.getValue());
        }
        if (onlyMemory) {
            printBase = strBuff
                    .append("[Offset Manager] delete offset from memory by modifier=")
                    .append(modifier).toString();
        } else {
            zkOffsetStorage.deleteGroupOffsetInfo(groupTopicPartMap);
            printBase = strBuff
                    .append("[Offset Manager] delete offset from memory and zk by modifier=")
                    .append(modifier).toString();
        }
        strBuff.delete(0, strBuff.length());
        // print log
        for (Map.Entry<String, Map<String, Set<Integer>>> entry
                : groupTopicPartMap.entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null
                    || entry.getValue().isEmpty()) {
                continue;
            }
            logger.info(strBuff.append(printBase).append(",group=").append(entry.getKey())
                    .append(",topic-partId-map=").append(entry.getValue()).toString());
            strBuff.delete(0, strBuff.length());
        }
    }

    /**
     * Set temp offset.
     *
     * @param group               the consume group name
     * @param offsetCacheKey      the offset store key
     * @param origOffset          the set value
     * @return                    the current inflight offset
     */
    private long setTmpOffset(final String group, final String offsetCacheKey, long origOffset) {
        long tmpOffset = origOffset - origOffset % DataStoreUtils.STORE_INDEX_HEAD_LEN;
        ConcurrentHashMap<String, Long> partTmpOffsetMap = tmpOffsetMap.get(group);
        if (partTmpOffsetMap == null) {
            ConcurrentHashMap<String, Long> tmpMap = new ConcurrentHashMap<>();
            partTmpOffsetMap = tmpOffsetMap.putIfAbsent(group, tmpMap);
            if (partTmpOffsetMap == null) {
                partTmpOffsetMap = tmpMap;
            }
        }
        Long befOffset = partTmpOffsetMap.put(offsetCacheKey, tmpOffset);
        if (befOffset == null) {
            return 0;
        } else {
            return (befOffset - befOffset % DataStoreUtils.STORE_INDEX_HEAD_LEN);
        }
    }

    private long getAndResetTmpOffset(final String group, final String offsetCacheKey) {
        ConcurrentHashMap<String, Long> partTmpOffsetMap = tmpOffsetMap.get(group);
        if (partTmpOffsetMap == null) {
            ConcurrentHashMap<String, Long> tmpMap = new ConcurrentHashMap<>();
            partTmpOffsetMap = tmpOffsetMap.putIfAbsent(group, tmpMap);
            if (partTmpOffsetMap == null) {
                partTmpOffsetMap = tmpMap;
            }
        }
        Long tmpOffset = partTmpOffsetMap.put(offsetCacheKey, 0L);
        if (tmpOffset == null) {
            return 0;
        } else {
            return (tmpOffset - tmpOffset % DataStoreUtils.STORE_INDEX_HEAD_LEN);
        }
    }

    /**
     * Commit temp offsets.
     */
    private void commitTmpOffsets() {
        for (Map.Entry<String, ConcurrentHashMap<String, Long>> entry : tmpOffsetMap.entrySet()) {
            if (TStringUtils.isBlank(entry.getKey())
                    || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Long> topicEntry : entry.getValue().entrySet()) {
                if (TStringUtils.isBlank(topicEntry.getKey())) {
                    continue;
                }
                String[] topicPartStrs = topicEntry.getKey().split("-");
                String topic = topicPartStrs[0];
                int partitionId = Integer.parseInt(topicPartStrs[1]);
                try {
                    commitOffset(entry.getKey(), topic, partitionId, true);
                } catch (Exception e) {
                    logger.warn("[Offset Manager] Commit tmp offset error!", e);
                }
            }
        }
    }

    private void commitCfmOffsets(boolean retryable) {
        long startTime = System.currentTimeMillis();
        for (Map.Entry<String, ConcurrentHashMap<String, OffsetStorageInfo>> entry : cfmOffsetMap.entrySet()) {
            if (TStringUtils.isBlank(entry.getKey())
                    || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            zkOffsetStorage.commitOffset(entry.getKey(), entry.getValue().values(), retryable);
        }
        BrokerSrvStatsHolder.updZKSyncDataDlt(System.currentTimeMillis() - startTime);
    }

    /**
     * Load or create offset.
     *
     * @param group            the consume group name
     * @param topic            the consumed topic name
     * @param partitionId      the consumed partition id
     * @param offsetCacheKey   the offset store key
     * @param defOffset        the default offset if not found
     * @return                 the stored offset object
     */
    private OffsetStorageInfo loadOrCreateOffset(final String group, final String topic,
                                                 int partitionId, final String offsetCacheKey,
                                                 long defOffset) {
        ConcurrentHashMap<String, OffsetStorageInfo> regInfoMap = cfmOffsetMap.get(group);
        if (regInfoMap == null) {
            ConcurrentHashMap<String, OffsetStorageInfo> tmpRegInfoMap
                    = new ConcurrentHashMap<>();
            regInfoMap = cfmOffsetMap.putIfAbsent(group, tmpRegInfoMap);
            if (regInfoMap == null) {
                regInfoMap = tmpRegInfoMap;
            }
        }
        OffsetStorageInfo regInfo = regInfoMap.get(offsetCacheKey);
        if (regInfo == null) {
            OffsetStorageInfo tmpRegInfo =
                    zkOffsetStorage.loadOffset(group, topic, partitionId);
            if (tmpRegInfo == null) {
                tmpRegInfo = new OffsetStorageInfo(topic,
                        brokerConfig.getBrokerId(), partitionId, defOffset, 0);
            }
            regInfo = regInfoMap.putIfAbsent(offsetCacheKey, tmpRegInfo);
            if (regInfo == null) {
                regInfo = tmpRegInfo;
            }
        }
        return regInfo;
    }

    private void rmvOffset(String group, Map<String, Set<Integer>> topicPartMap) {
        if (group == null
                || topicPartMap == null
                || topicPartMap.isEmpty()) {
            return;
        }
        // remove confirm offset
        ConcurrentHashMap<String, OffsetStorageInfo> regInfoMap = cfmOffsetMap.get(group);
        if (regInfoMap != null) {
            for (Map.Entry<String, Set<Integer>> entry : topicPartMap.entrySet()) {
                if (entry.getKey() == null
                        || entry.getValue() == null
                        || entry.getValue().isEmpty()) {
                    continue;
                }
                for (Integer partitionId : entry.getValue()) {
                    String offsetCacheKey = getOffsetCacheKey(entry.getKey(), partitionId);
                    regInfoMap.remove(offsetCacheKey);
                }
            }
            if (regInfoMap.isEmpty()) {
                cfmOffsetMap.remove(group);
            }
        }
        // remove tmp offset
        ConcurrentHashMap<String, Long> tmpRegInfoMap = tmpOffsetMap.get(group);
        if (tmpRegInfoMap != null) {
            for (Map.Entry<String, Set<Integer>> entry : topicPartMap.entrySet()) {
                if (entry.getKey() == null
                        || entry.getValue() == null
                        || entry.getValue().isEmpty()) {
                    continue;
                }
                for (Integer partitionId : entry.getValue()) {
                    String offsetCacheKey = getOffsetCacheKey(entry.getKey(), partitionId);
                    tmpRegInfoMap.remove(offsetCacheKey);
                }
            }
            if (tmpRegInfoMap.isEmpty()) {
                tmpOffsetMap.remove(group);
            }
        }
    }

    private String getOffsetCacheKey(String topic, int partitionId) {
        return new StringBuilder(256).append(topic)
                .append("-").append(partitionId).toString();
    }

    private int getLagLevel(long lagValue) {
        return (lagValue > TServerConstants.CFG_OFFSET_RESET_MID_ALARM_CHECK)
                ? 2 : (lagValue > TServerConstants.CFG_OFFSET_RESET_MIN_ALARM_CHECK) ? 1 : 0;
    }

}
