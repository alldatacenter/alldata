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

package org.apache.inlong.tubemq.server.broker.msgstore;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.server.broker.BrokerConfig;
import org.apache.inlong.tubemq.server.broker.TubeBroker;
import org.apache.inlong.tubemq.server.broker.exception.StartupException;
import org.apache.inlong.tubemq.server.broker.metadata.MetadataManager;
import org.apache.inlong.tubemq.server.broker.metadata.TopicMetadata;
import org.apache.inlong.tubemq.server.broker.msgstore.disk.GetMessageResult;
import org.apache.inlong.tubemq.server.broker.nodeinfo.ConsumerNodeInfo;
import org.apache.inlong.tubemq.server.broker.offset.OffsetRecordInfo;
import org.apache.inlong.tubemq.server.broker.offset.RecordItem;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;
import org.apache.inlong.tubemq.server.broker.utils.TopicPubStoreInfo;
import org.apache.inlong.tubemq.server.common.TStatusConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message storage management. It contains all topics on broker. In charge of store, expire, and flush operation,
 */
public class MessageStoreManager implements StoreService {
    private static final Logger logger = LoggerFactory.getLogger(MessageStoreManager.class);
    private final BrokerConfig tubeConfig;
    private final TubeBroker tubeBroker;
    // metadata manager, get metadata from master.
    private final MetadataManager metadataManager;
    // storeId to store on each topic.
    private final ConcurrentHashMap<String/* topic */,
            ConcurrentHashMap<Integer/* storeId */, MessageStore>> dataStores =
            new ConcurrentHashMap<>();
    // store service status
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    // data expire operation scheduler.
    private final ScheduledExecutorService logClearScheduler;
    // flush operation scheduler.
    private final ScheduledExecutorService unFlushDiskScheduler;
    // message on memory sink to disk operation scheduler.
    private final ScheduledExecutorService unFlushMemScheduler;
    // max transfer size.
    private final int maxMsgTransferSize;
    // the status that is deleting topic.
    private final AtomicBoolean isRemovingTopic = new AtomicBoolean(false);

    /**
     * Initial the message-store manager.
     *
     * @param tubeBroker      the broker instance
     * @param tubeConfig      the initial configure
     * @throws IOException    the exception during processing
     */
    public MessageStoreManager(final TubeBroker tubeBroker,
                               final BrokerConfig tubeConfig) throws IOException {
        super();
        this.tubeConfig = tubeConfig;
        this.tubeBroker = tubeBroker;
        this.metadataManager = this.tubeBroker.getMetadataManager();
        this.isRemovingTopic.set(false);
        this.maxMsgTransferSize =
                Math.min(tubeConfig.getTransferSize(), DataStoreUtils.MAX_MSG_TRANSFER_SIZE);
        this.metadataManager.addPropertyChangeListener("topicConfigMap", new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                Map<String, TopicMetadata> oldTopicConfigMap
                        = (Map<String, TopicMetadata>) evt.getOldValue();
                Map<String, TopicMetadata> newTopicConfigMap
                        = (Map<String, TopicMetadata>) evt.getNewValue();
                MessageStoreManager.this.refreshMessageStoresHoldVals(oldTopicConfigMap, newTopicConfigMap);

            }
        });
        this.logClearScheduler =
                Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "Broker Log Clear Thread");
                    }
                });
        this.unFlushDiskScheduler =
                Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "Broker Log Disk Flush Thread");
                    }
                });
        this.unFlushMemScheduler =
                Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "Broker Log Mem Flush Thread");
                    }
                });

    }

    @Override
    public void start() {
        try {
            this.loadMessageStores(this.tubeConfig);
        } catch (final IOException e) {
            logger.error("[Store Manager] load message stores failed", e);
            throw new StartupException("Initialize message store manager failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.logClearScheduler.scheduleWithFixedDelay(new LogClearRunner(),
                tubeConfig.getLogClearupDurationMs(),
                tubeConfig.getLogClearupDurationMs(),
                TimeUnit.MILLISECONDS);

        this.unFlushDiskScheduler.scheduleWithFixedDelay(new DiskUnFlushRunner(),
                tubeConfig.getLogFlushDiskDurMs(),
                tubeConfig.getLogFlushDiskDurMs(),
                TimeUnit.MILLISECONDS);

        this.unFlushMemScheduler.scheduleWithFixedDelay(new MemUnFlushRunner(),
                tubeConfig.getLogFlushMemDurMs(),
                tubeConfig.getLogFlushMemDurMs(),
                TimeUnit.MILLISECONDS);

    }

    @Override
    public void close() {
        if (this.stopped.get()) {
            return;
        }
        if (this.stopped.compareAndSet(false, true)) {
            logger.info("[Store Manager] begin close store manager......");
            this.logClearScheduler.shutdownNow();
            this.unFlushDiskScheduler.shutdownNow();
            this.unFlushMemScheduler.shutdownNow();
            for (Map.Entry<String, ConcurrentHashMap<Integer, MessageStore>> entry :
                    this.dataStores.entrySet()) {
                if (entry.getValue() != null) {
                    ConcurrentHashMap<Integer, MessageStore> subMap = entry.getValue();
                    for (Map.Entry<Integer, MessageStore> subEntry : subMap.entrySet()) {
                        if (subEntry.getValue() != null) {
                            try {
                                subEntry.getValue().close();
                            } catch (final Throwable e) {
                                logger.error(new StringBuilder(512)
                                        .append("[Store Manager] Try to run close  ")
                                        .append(subEntry.getValue().getStoreKey()).append(" failed").toString(), e);
                            }
                        }
                    }
                }
            }
            this.dataStores.clear();
            logger.info("[Store Manager] Store Manager stopped!");
        }
    }

    @Override
    public List<String> removeTopicStore() {
        if (isRemovingTopic.get()) {
            return null;
        }
        if (!isRemovingTopic.compareAndSet(false, true)) {
            return null;
        }
        try {
            List<String> removedTopics =
                    new ArrayList<>();
            Map<String, TopicMetadata> removedTopicMap =
                    this.metadataManager.getRemovedTopicConfigMap();
            if (removedTopicMap.isEmpty()) {
                return removedTopics;
            }
            Set<String> targetTopics = new HashSet<>();
            for (Map.Entry<String, TopicMetadata> entry : removedTopicMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                if (entry.getValue().getStatusId() == TStatusConstants.STATUS_TOPIC_SOFT_REMOVE) {
                    targetTopics.add(entry.getKey());
                }
            }
            if (targetTopics.isEmpty()) {
                return removedTopics;
            }
            for (String tmpTopic : targetTopics) {
                ConcurrentHashMap<Integer, MessageStore> topicStores =
                        dataStores.get(tmpTopic);
                if (topicStores != null) {
                    Set<Integer> storeIds = topicStores.keySet();
                    for (Integer storeId : storeIds) {
                        try {
                            MessageStore tmpStore = topicStores.remove(storeId);
                            tmpStore.close();
                            if (topicStores.isEmpty()) {
                                this.dataStores.remove(tmpTopic);
                            }
                        } catch (Throwable ee) {
                            logger.error(new StringBuilder(512)
                                    .append("[Remove Topic] Close removed store failure, storeKey=")
                                    .append(tmpTopic).append("-").append(storeId).toString(), ee);
                        }
                    }
                }
                TopicMetadata tmpTopicConf = removedTopicMap.get(tmpTopic);
                if (tmpTopicConf != null) {
                    StringBuilder sBuilder = new StringBuilder(512);
                    for (int storeId = 0; storeId < tmpTopicConf.getNumTopicStores(); storeId++) {
                        String storeDir = sBuilder.append(tmpTopicConf.getDataPath())
                                .append(File.separator).append(tmpTopic).append("-")
                                .append(storeId).toString();
                        sBuilder.delete(0, sBuilder.length());
                        try {
                            delTopicFiles(storeDir);
                        } catch (Throwable e) {
                            logger.error("[Remove Topic] Remove topic data error : ", e);
                        }
                        ThreadUtils.sleep(50);
                    }
                    tmpTopicConf.setStatusId(TStatusConstants.STATUS_TOPIC_HARD_REMOVE);
                    removedTopics.add(tmpTopic);
                }
                ThreadUtils.sleep(100);
            }
            return removedTopics;
        } finally {
            this.isRemovingTopic.set(false);
        }
    }

    /**
     * Get message store by topic.
     *
     * @param topic  query topic name
     * @return       the queried topic's store list
     */
    @Override
    public Collection<MessageStore> getMessageStoresByTopic(final String topic) {
        final ConcurrentHashMap<Integer, MessageStore> map
                = this.dataStores.get(topic);
        if (map == null) {
            return Collections.emptyList();
        }
        return map.values();
    }

    /**
     * Get or create message store.
     *
     * @param topic           the topic name
     * @param partition       the partition id
     * @return                the message-store instance
     * @throws IOException    the exception during processing
     */
    @Override
    public MessageStore getOrCreateMessageStore(final String topic,
                                                final int partition) throws Throwable {
        StringBuilder sBuilder = new StringBuilder(512);
        final int storeId = partition < TBaseConstants.META_STORE_INS_BASE
                ? 0 : partition / TBaseConstants.META_STORE_INS_BASE;
        int realPartition = partition < TBaseConstants.META_STORE_INS_BASE
                ? partition : partition % TBaseConstants.META_STORE_INS_BASE;
        final String dataStoreToken = sBuilder.append("tube_store_manager_").append(topic).toString();
        sBuilder.delete(0, sBuilder.length());
        if (realPartition < 0 || realPartition >= this.metadataManager.getNumPartitions(topic)) {
            throw new IllegalArgumentException(sBuilder.append("Wrong partition value ")
                    .append(partition).append(",valid partitions in (0,")
                    .append(this.metadataManager.getNumPartitions(topic) - 1)
                    .append(")").toString());
        }
        ConcurrentHashMap<Integer, MessageStore> dataMap = dataStores.get(topic);
        if (dataMap == null) {
            ConcurrentHashMap<Integer, MessageStore> tmpTopicMap =
                    new ConcurrentHashMap<>();
            dataMap = this.dataStores.putIfAbsent(topic, tmpTopicMap);
            if (dataMap == null) {
                dataMap = tmpTopicMap;
            }
        }
        MessageStore messageStore = dataMap.get(storeId);
        if (messageStore == null) {
            synchronized (dataStoreToken.intern()) {
                messageStore = dataMap.get(storeId);
                if (messageStore == null) {
                    TopicMetadata topicMetadata =
                            metadataManager.getTopicMetadata(topic);
                    MessageStore tmpMessageStore =
                            new MessageStore(this, topicMetadata, storeId,
                                    tubeConfig, 0, maxMsgTransferSize);
                    messageStore = dataMap.putIfAbsent(storeId, tmpMessageStore);
                    if (messageStore == null) {
                        messageStore = tmpMessageStore;
                        logger.info(sBuilder
                                .append("[Store Manager] Created a new message storage, storeKey=")
                                .append(topic).append("-").append(storeId).toString());
                    } else {
                        tmpMessageStore.close();
                    }
                }
            }
        }
        return messageStore;
    }

    public TubeBroker getTubeBroker() {
        return this.tubeBroker;
    }

    /**
     * Get message from store.
     *
     * @param msgStore        the message-store
     * @param topic           the topic name
     * @param partitionId     the partition id
     * @param msgCount        the message count to read
     * @param filterCondSet   the filter condition set
     * @return                the query result
     * @throws IOException    the exception during processing
     */
    public GetMessageResult getMessages(final MessageStore msgStore,
                                        final String topic,
                                        final int partitionId,
                                        final int msgCount,
                                        final Set<String> filterCondSet) throws IOException {
        long requestOffset = 0L;
        try {
            final long maxOffset = msgStore.getIndexMaxOffset();
            ConsumerNodeInfo consumerNodeInfo =
                    new ConsumerNodeInfo(tubeBroker.getStoreManager(),
                            "visit", filterCondSet, "", System.currentTimeMillis(), "");
            int maxIndexReadSize = (msgCount + 1)
                    * DataStoreUtils.STORE_INDEX_HEAD_LEN * msgStore.getPartitionNum();
            if (filterCondSet != null && !filterCondSet.isEmpty()) {
                maxIndexReadSize *= 5;
            }
            requestOffset = maxOffset - maxIndexReadSize < 0 ? 0L : maxOffset - maxIndexReadSize;
            return msgStore.getMessages(303, requestOffset, partitionId,
                    consumerNodeInfo, topic, this.maxMsgTransferSize, 0);
        } catch (Throwable e1) {
            return new GetMessageResult(false, TErrCodeConstants.INTERNAL_SERVER_ERROR,
                    requestOffset, 0, "Get message failure, errMsg=" + e1.getMessage());
        }
    }

    public MetadataManager getMetadataManager() {
        return tubeBroker.getMetadataManager();
    }

    public int getMaxMsgTransferSize() {
        return maxMsgTransferSize;
    }

    public Map<String, ConcurrentHashMap<Integer, MessageStore>> getMessageStores() {
        return Collections.unmodifiableMap(this.dataStores);
    }

    /**
     * Query topic's publish info.
     *
     * @param topicSet query's topic set
     *
     * @return the topic's offset info
     */
    @Override
    public Map<String, Map<Integer, TopicPubStoreInfo>> getTopicPublishInfos(
            Set<String> topicSet) {
        MessageStore store = null;
        TopicMetadata topicMetadata = null;
        Set<String> qryTopicSet = new HashSet<>();
        Map<String, Map<Integer, TopicPubStoreInfo>> topicPubStoreInfoMap = new HashMap<>();
        Map<String, TopicMetadata> confTopicInfo = metadataManager.getTopicConfigMap();
        if (topicSet == null || topicSet.isEmpty()) {
            qryTopicSet.addAll(confTopicInfo.keySet());
        } else {
            for (String topic : topicSet) {
                if (confTopicInfo.containsKey(topic)) {
                    qryTopicSet.add(topic);
                }
            }
        }
        if (qryTopicSet.isEmpty()) {
            return topicPubStoreInfoMap;
        }
        for (String topic : qryTopicSet) {
            topicMetadata = confTopicInfo.get(topic);
            if (topicMetadata == null) {
                continue;
            }
            Map<Integer, MessageStore> storeMap = dataStores.get(topic);
            if (storeMap == null) {
                continue;
            }
            Map<Integer, TopicPubStoreInfo> storeInfoMap = new HashMap<>();
            for (Map.Entry<Integer, MessageStore> entry : storeMap.entrySet()) {
                if (entry == null
                        || entry.getKey() == null
                        || entry.getValue() == null) {
                    continue;
                }
                store = entry.getValue();
                for (Integer partitionId : topicMetadata.getPartIdsByStoreId(entry.getKey())) {
                    TopicPubStoreInfo storeInfo =
                            new TopicPubStoreInfo(topic, entry.getKey(), partitionId,
                                    store.getIndexMinOffset(), store.getIndexMaxOffset(),
                                    store.getDataMinOffset(), store.getDataMaxOffset());
                    storeInfoMap.put(partitionId, storeInfo);
                }
            }
            topicPubStoreInfoMap.put(topic, storeInfoMap);
        }
        return topicPubStoreInfoMap;
    }

    /**
     * Query topic's publish info.
     *
     * @param groupOffsetMap query's topic set
     *
     */
    @Override
    public void getTopicPublishInfos(Map<String, OffsetRecordInfo> groupOffsetMap) {
        MessageStore store = null;
        for (Map.Entry<String, OffsetRecordInfo> entry : groupOffsetMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            Map<String, Map<Integer, RecordItem>> topicOffsetMap =
                    entry.getValue().getOffsetMap();
            // Get offset records by topic
            for (Map.Entry<String, Map<Integer, RecordItem>> entryTopic
                    : topicOffsetMap.entrySet()) {
                if (entryTopic == null
                        || entryTopic.getKey() == null
                        || entryTopic.getValue() == null) {
                    continue;
                }
                // Get message store instance
                Map<Integer, MessageStore> storeMap =
                        dataStores.get(entryTopic.getKey());
                if (storeMap == null) {
                    continue;
                }
                for (Map.Entry<Integer, RecordItem> entryRcd
                        : entryTopic.getValue().entrySet()) {
                    store = storeMap.get(entryRcd.getValue().getStoreId());
                    if (store == null) {
                        continue;
                    }
                    // Append current max, min offset
                    entryRcd.getValue().addStoreInfo(store.getIndexMinOffset(),
                            store.getIndexMaxOffset(), store.getDataMinOffset(),
                            store.getDataMaxOffset());
                }
            }
        }
    }

    private Set<File> getLogDirSet(final BrokerConfig tubeConfig) throws IOException {
        TopicMetadata topicMetadata = null;
        final Set<String> paths = new HashSet<>();
        paths.add(tubeConfig.getPrimaryPath());
        for (final String topic : metadataManager.getTopics()) {
            topicMetadata = metadataManager.getTopicMetadata(topic);
            if (topicMetadata != null
                    && TStringUtils.isNotBlank(topicMetadata.getDataPath())) {
                paths.add(topicMetadata.getDataPath());
            }
        }
        final Set<File> fileSet = new HashSet<>();
        for (final String path : paths) {
            final File dir = new File(path);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException(new StringBuilder(512)
                        .append("Could not make Log directory ")
                        .append(dir.getAbsolutePath()).toString());
            }
            if (!dir.isDirectory() || !dir.canRead()) {
                throw new IOException(new StringBuilder(512).append("Log path ")
                        .append(dir.getAbsolutePath())
                        .append(" is not a readable directory").toString());
            }
            fileSet.add(dir);
        }
        return fileSet;
    }

    /**
     * Load stores sequential.
     *
     * @param tubeConfig             the broker's configure
     * @throws IOException           the exception during processing
     * @throws InterruptedException  the exception during processing
     */
    private void loadMessageStores(final BrokerConfig tubeConfig)
            throws IOException, InterruptedException {
        StringBuilder sBuilder = new StringBuilder(512);
        logger.info(sBuilder.append("[Store Manager] Begin to load message stores from path ")
                .append(tubeConfig.getPrimaryPath()).toString());
        sBuilder.delete(0, sBuilder.length());
        final long start = System.currentTimeMillis();
        final AtomicInteger errCnt = new AtomicInteger(0);
        final AtomicInteger finishCnt = new AtomicInteger(0);
        List<Callable<MessageStore>> tasks = new ArrayList<>();
        for (final File dir : this.getLogDirSet(tubeConfig)) {
            if (dir == null) {
                continue;
            }
            final File[] ls = dir.listFiles();
            if (ls == null) {
                continue;
            }
            for (final File subDir : ls) {
                if (subDir == null) {
                    continue;
                }
                if (!subDir.isDirectory()) {
                    continue;
                }
                final String name = subDir.getName();
                final int index = name.lastIndexOf('-');
                if (index < 0) {
                    logger.warn(sBuilder.append("[Store Manager] Ignore invalid directory:")
                            .append(subDir.getAbsolutePath()).toString());
                    sBuilder.delete(0, sBuilder.length());
                    continue;
                }
                final String topic = name.substring(0, index);
                final TopicMetadata topicMetadata = metadataManager.getTopicMetadata(topic);
                if (topicMetadata == null) {
                    logger.warn(sBuilder
                            .append("[Store Manager] No valid topic config for topic data directories:")
                            .append(topic).toString());
                    sBuilder.delete(0, sBuilder.length());
                    continue;
                }
                final int storeId = Integer.parseInt(name.substring(index + 1));
                final MessageStoreManager messageStoreManager = this;
                tasks.add(new Callable<MessageStore>() {
                    @Override
                    public MessageStore call() throws Exception {
                        MessageStore msgStore = null;
                        try {
                            msgStore = new MessageStore(messageStoreManager,
                                    topicMetadata, storeId, tubeConfig, maxMsgTransferSize);
                            ConcurrentHashMap<Integer, MessageStore> map =
                                    dataStores.get(msgStore.getTopic());
                            if (map == null) {
                                map = new ConcurrentHashMap<>();
                                ConcurrentHashMap<Integer, MessageStore> oldmap =
                                        dataStores.putIfAbsent(msgStore.getTopic(), map);
                                if (oldmap != null) {
                                    map = oldmap;
                                }
                            }
                            MessageStore oldMsgStore = map.putIfAbsent(msgStore.getStoreId(), msgStore);
                            if (oldMsgStore != null) {
                                try {
                                    msgStore.close();
                                    logger.info(new StringBuilder(512)
                                            .append("[Store Manager] Close duplicated messageStore ")
                                            .append(msgStore.getStoreKey()).toString());
                                } catch (Throwable e2) {
                                    //
                                    logger.info("[Store Manager] Close duplicated messageStore failure", e2);
                                }
                            }
                        } catch (Throwable e2) {
                            errCnt.incrementAndGet();
                            logger.error(new StringBuilder(512).append("[Store Manager] Loaded ")
                                    .append(subDir.getAbsolutePath())
                                    .append("message store failure:").toString(), e2);
                        } finally {
                            finishCnt.incrementAndGet();
                        }
                        return null;
                    }
                });
            }
        }
        this.loadStoresInParallel(tasks);
        tasks.clear();
        if (errCnt.get() > 0) {
            throw new RuntimeException(
                    "[Store Manager] failure to load message stores, please check load logger and fix first!");
        }
        logger.info(sBuilder.append("[Store Manager] End to load message stores in ")
                .append((System.currentTimeMillis() - start) / 1000).append(" secs").toString());
    }

    /**
     * Load stores in parallel.
     *
     * @param tasks                    the load tasks
     * @throws InterruptedException    the exception during processing
     */
    private void loadStoresInParallel(List<Callable<MessageStore>> tasks) throws InterruptedException {
        ExecutorService executor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        CompletionService<MessageStore> completionService =
                new ExecutorCompletionService<>(executor);
        for (Callable<MessageStore> task : tasks) {
            completionService.submit(task);
        }
        for (int i = 0; i < tasks.size(); i++) {
            try {
                completionService.take().get();
            } catch (Throwable e) {
                //
            }
        }
        executor.shutdown();
    }

    private void delTopicFiles(String filepath) throws IOException {
        File targetFile = new File(filepath);
        if (targetFile.exists()) {
            if (targetFile.isFile()) {
                targetFile.delete();
            } else if (targetFile.isDirectory()) {
                File[] files = targetFile.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        this.delTopicFiles(files[i].getAbsolutePath());
                    }
                }
            }
            targetFile.delete();
        }
    }

    /**
     * Refresh message-store's dynamic configures
     *
     * @param oldTopicConfigMap     the stored topic configure map
     * @param newTopicConfigMap     the newly topic configure map
     */
    public void refreshMessageStoresHoldVals(Map<String, TopicMetadata> oldTopicConfigMap,
                                             Map<String, TopicMetadata> newTopicConfigMap) {
        if (((newTopicConfigMap == null) || newTopicConfigMap.isEmpty())
                || ((oldTopicConfigMap == null) || oldTopicConfigMap.isEmpty())) {
            return;
        }
        StringBuilder sBuilder = new StringBuilder(512);
        for (TopicMetadata newTopicMetadata : newTopicConfigMap.values()) {
            TopicMetadata oldTopicMetadata = oldTopicConfigMap.get(newTopicMetadata.getTopic());
            if ((oldTopicMetadata == null) || oldTopicMetadata.isPropertyEquals(newTopicMetadata)) {
                continue;
            }
            ConcurrentHashMap<Integer, MessageStore> messageStores =
                    MessageStoreManager.this.dataStores.get(newTopicMetadata.getTopic());
            if ((messageStores == null) || messageStores.isEmpty()) {
                continue;
            }
            for (Map.Entry<Integer, MessageStore> entry : messageStores.entrySet()) {
                if (entry.getValue() != null) {
                    try {
                        entry.getValue().refreshUnflushThreshold(newTopicMetadata);
                    } catch (Throwable ee) {
                        logger.error(sBuilder.append("[Store Manager] refresh ")
                                .append(entry.getValue().getStoreKey())
                                .append("'s parameter error,").toString(), ee);
                        sBuilder.delete(0, sBuilder.length());
                    }
                }
            }
        }
    }

    private class LogClearRunner implements Runnable {

        public LogClearRunner() {
            //
        }

        @Override
        public void run() {
            StringBuilder sBuilder = new StringBuilder(256);
            long startTime = System.currentTimeMillis();
            Set<String> expiredTopic = getExpiredTopicSet(sBuilder);
            if (!expiredTopic.isEmpty()) {
                logger.info(sBuilder.append("Found ").append(expiredTopic.size())
                        .append(" files expired, start delete files!").toString());
                sBuilder.delete(0, sBuilder.length());
                for (String topicName : expiredTopic) {
                    if (topicName == null) {
                        continue;
                    }
                    Map<Integer, MessageStore> storeMap = dataStores.get(topicName);
                    if (storeMap == null || storeMap.isEmpty()) {
                        continue;
                    }
                    for (Map.Entry<Integer, MessageStore> entry : storeMap.entrySet()) {
                        if (entry.getValue() == null) {
                            continue;
                        }
                        try {
                            entry.getValue().runClearupPolicy(false);
                        } catch (final Throwable e) {
                            logger.error(sBuilder.append("Try to run delete policy with ")
                                    .append(entry.getValue().getStoreKey())
                                    .append("'s log file  failed").toString(), e);
                            sBuilder.delete(0, sBuilder.length());
                        }
                    }
                }
                logger.info("Log Clear Scheduler finished file delete!");
            }
            long dltTime = System.currentTimeMillis() - startTime;
            if (dltTime >= tubeConfig.getLogClearupDurationMs()) {
                logger.warn(sBuilder.append("Log Clear up task continue over the clearup duration, ")
                        .append("used ").append(dltTime).append(", configure value is ")
                        .append(tubeConfig.getLogClearupDurationMs()).toString());
                sBuilder.delete(0, sBuilder.length());
            }
        }

        private Set<String> getExpiredTopicSet(final StringBuilder sb) {
            Set<String> expiredTopic = new HashSet<>();
            for (Map<Integer, MessageStore> storeMap : dataStores.values()) {
                if (storeMap == null || storeMap.isEmpty()) {
                    continue;
                }
                for (MessageStore msgStore : storeMap.values()) {
                    if (msgStore == null) {
                        continue;
                    }
                    try {
                        if (msgStore.runClearupPolicy(true)) {
                            expiredTopic.add(msgStore.getTopic());
                        }
                    } catch (final Throwable e) {
                        logger.error(sb.append("Try to run delete policy with ")
                                .append(msgStore.getStoreKey())
                                .append("'s log file failed").toString(), e);
                        sb.delete(0, sb.length());
                    }
                }
            }
            return expiredTopic;
        }
    }

    private class DiskUnFlushRunner implements Runnable {

        public DiskUnFlushRunner() {
            //
        }

        @Override
        public void run() {
            StringBuilder sBuilder = new StringBuilder(256);
            for (Map<Integer, MessageStore> storeMap : dataStores.values()) {
                if (storeMap == null || storeMap.isEmpty()) {
                    continue;
                }
                for (MessageStore msgStore : storeMap.values()) {
                    if (msgStore == null) {
                        continue;
                    }
                    try {
                        msgStore.flushFile();
                    } catch (final Throwable e) {
                        logger.error(sBuilder.append("[Store Manager] Try to flush ")
                                .append(msgStore.getStoreKey())
                                .append("'s file-store failed : ").toString(), e);
                        sBuilder.delete(0, sBuilder.length());
                    }
                }
            }
        }
    }

    private class MemUnFlushRunner implements Runnable {

        public MemUnFlushRunner() {
            //
        }

        @Override
        public void run() {
            StringBuilder sBuilder = new StringBuilder(256);
            for (Map<Integer, MessageStore> storeMap : dataStores.values()) {
                if (storeMap == null || storeMap.isEmpty()) {
                    continue;
                }
                for (MessageStore msgStore : storeMap.values()) {
                    if (msgStore == null) {
                        continue;
                    }
                    try {
                        msgStore.flushMemCacheData();
                    } catch (final Throwable e) {
                        logger.error(sBuilder.append("[Store Manager] Try to flush ")
                                .append(msgStore.getStoreKey())
                                .append("'s mem-store failed : ").toString(), e);
                        sBuilder.delete(0, sBuilder.length());
                    }
                }
            }
        }
    }

}
