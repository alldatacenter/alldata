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

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.client.common.PeerInfo;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.factory.InnerSessionFactory;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of PushMessageConsumer.
 */
public class SimplePushMessageConsumer implements PushMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SimplePushMessageConsumer.class);
    private static final int MAX_FAILURE_LOG_TIMES = 10;
    private final MessageFetchManager fetchManager;
    private final BaseMessageConsumer baseConsumer;
    private AtomicLong lastLogPrintTime = new AtomicLong(0);
    private AtomicLong lastFailureCount = new AtomicLong(0);
    private CountDownLatch consumeSync = new CountDownLatch(0);

    public SimplePushMessageConsumer(final InnerSessionFactory messageSessionFactory,
                                     final ConsumerConfig consumerConfig) throws TubeClientException {
        baseConsumer =
                new BaseMessageConsumer(messageSessionFactory, consumerConfig, false);
        this.fetchManager =
                new MessageFetchManager(baseConsumer.consumerConfig, this);
        this.fetchManager.startFetchWorkers();
    }

    @Override
    public void shutdown() throws Throwable {
        this.pauseConsume();
        this.fetchManager.stopFetchWorkers(true);
        ThreadUtils.sleep(200);
        this.fetchManager.stopFetchWorkers(false);
        baseConsumer.shutdown();
    }

    @Override
    public PushMessageConsumer subscribe(String topic,
                                         TreeSet<String> filterConds,
                                         MessageListener messageListener) throws TubeClientException {
        baseConsumer.subscribe(topic, filterConds, messageListener);
        return this;
    }

    @Override
    public void completeSubscribe() throws TubeClientException {
        baseConsumer.completeSubscribe();
    }

    @Override
    public void completeSubscribe(final String sessionKey,
                                  final int sourceCount,
                                  final boolean isSelectBig,
                                  final Map<String, Long> partOffsetMap) throws TubeClientException {
        baseConsumer.completeSubscribe(sessionKey, sourceCount, isSelectBig, partOffsetMap);
    }

    @Override
    public String getClientVersion() {
        return baseConsumer.getClientVersion();
    }

    @Override
    public String getConsumerId() {
        return baseConsumer.getConsumerId();
    }

    @Override
    public boolean isShutdown() {
        return baseConsumer.isShutdown();
    }

    @Override
    public ConsumerConfig getConsumerConfig() {
        return baseConsumer.getConsumerConfig();
    }

    @Override
    public boolean isFilterConsume(String topic) {
        return baseConsumer.isFilterConsume(topic);
    }

    @Override
    public Map<String, ConsumeOffsetInfo> getCurConsumedPartitions() throws TubeClientException {
        return baseConsumer.getCurConsumedPartitions();
    }

    @Override
    public void freezePartitions(List<String> partitionKeys) throws TubeClientException {
        baseConsumer.freezePartitions(partitionKeys);
    }

    @Override
    public void unfreezePartitions(List<String> partitionKeys) throws TubeClientException {
        baseConsumer.unfreezePartitions(partitionKeys);
    }

    @Override
    public void relAllFrozenPartitions() {
        this.baseConsumer.relAllFrozenPartitions();
    }

    @Override
    public Map<String, Long> getFrozenPartInfo() {
        return baseConsumer.getFrozenPartInfo();
    }

    protected BaseMessageConsumer getBaseConsumer() {
        return this.baseConsumer;
    }

    protected void allowConsumeWait() {
        if (this.consumeSync != null
                && this.consumeSync.getCount() != 0) {
            try {
                this.consumeSync.await();
            } catch (InterruptedException ee) {
                //
            }
        }
    }

    @Override
    public void resumeConsume() {
        this.consumeSync.countDown();
        logger.info(new StringBuilder(256)
                .append("[ResumeConsume] Consume is resume, consumerId :")
                .append(this.baseConsumer.consumerId).toString());
    }

    @Override
    public void pauseConsume() {
        this.consumeSync = new CountDownLatch(1);
        logger.info(new StringBuilder(256)
                .append("[PauseConsume] Consume is paused, consumerId :")
                .append(this.baseConsumer.consumerId).toString());
    }

    @Override
    public boolean isConsumePaused() {
        return (this.consumeSync != null
                && this.consumeSync.getCount() != 0);
    }

    /**
     * Process the selected partition result.
     *
     * @param partSelectResult partition select result
     * @param sBuilder         a string builder
     */
    protected void processRequest(PartitionSelectResult partSelectResult, final StringBuilder sBuilder) {
        final long startTime = System.currentTimeMillis();
        FetchContext taskContext =
                baseConsumer.fetchMessage(partSelectResult, sBuilder);
        if (!taskContext.isSuccess()) {
            if (logger.isDebugEnabled()) {
                logger.debug(sBuilder.append("Fetch message error: partition:")
                        .append(partSelectResult.getPartition().toString()).append(" error is ")
                        .append(taskContext.getErrMsg()).toString());
                sBuilder.delete(0, sBuilder.length());
            }
            return;
        }
        boolean isConsumed = false;
        if (!isShutdown()) {
            if (taskContext.getMessageList() == null
                    || taskContext.getMessageList().isEmpty()) {
                isConsumed = true;
            } else {
                try {
                    final TopicProcessor topicProcessor =
                            baseConsumer.consumeSubInfo.getTopicProcessor(taskContext.getPartition().getTopic());
                    if ((topicProcessor == null) || (topicProcessor.getMessageListener() == null)) {
                        throw new TubeClientException(sBuilder
                                .append("Listener is null for topic ")
                                .append(taskContext.getPartition().getTopic()).toString());
                    }
                    isConsumed = notifyListener(taskContext, topicProcessor, sBuilder);
                } catch (Throwable e) {
                    isConsumed =
                            (!baseConsumer.consumerConfig.isPushListenerThrowedRollBack());
                    logMessageProcessFailed(taskContext, e);
                }
            }
        }
        baseConsumer.rmtDataCache.succRspRelease(taskContext.getPartition().getPartitionKey(),
                taskContext.getPartition().getTopic(), taskContext.getUsedToken(),
                isConsumed, isFilterConsume(taskContext.getPartition().getTopic()),
                taskContext.getCurrOffset(), taskContext.getMaxOffset());

        // Warning if the process time is too long
        long cost = System.currentTimeMillis() - startTime;
        if (cost > 30000) {
            logger.info(sBuilder.append("Consuming Partition; current processing thread ")
                    .append(Thread.currentThread().getName())
                    .append("-->Process[")
                    .append(partSelectResult.getPartition().toString())
                    .append("] cost:").append(cost).append(" Ms").toString());
            sBuilder.delete(0, sBuilder.length());
        }
    }

    private boolean notifyListener(final FetchContext request,
                                   final TopicProcessor topicProcessor,
                                   final StringBuilder sBuilder) throws Exception {
        final MessageListener listener = topicProcessor.getMessageListener();
        if (listener.getExecutor() != null) {
            try {
                listener.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        receiveMessages(request, topicProcessor);
                    }
                });
            } catch (final RejectedExecutionException e) {
                logger.error(new StringBuilder(512)
                        .append("MessageListener thread poll is busy, topic=")
                        .append(request.getPartition().getTopic())
                        .append(",partition=").append(request.getPartition()).toString(), e);
                throw e;
            }
        } else {
            this.receiveMessages(request, topicProcessor);
        }
        baseConsumer.clientStatsInfo.bookReturnDuration(request.getPartitionKey(),
                System.currentTimeMillis() - request.getUsedToken());
        return true;
    }

    private void receiveMessages(final FetchContext request,
                                 final TopicProcessor topicProcessor) {
        if (request != null && request.getMessageList() != null) {
            MessageListener msgListener = topicProcessor.getMessageListener();
            try {
                msgListener.receiveMessages(new PeerInfo(request.getPartition(),
                        request.getCurrOffset(), request.getMaxOffset()), request.getMessageList());
            } catch (InterruptedException e) {
                logger.info(
                        "Call listener to process received messages throw Interrupted Exception!");
            }
        }
    }

    /**
     * A utility method that log message process failure.
     *
     * @param request fetch task context
     * @param e       error cause
     */
    private void logMessageProcessFailed(final FetchContext request, final Throwable e) {
        StringBuilder sBuilder = new StringBuilder(512);
        sBuilder.append("CallBack process message failed: partition=").append(request.getPartition());
        sBuilder.append(", group=").append(baseConsumer.consumerConfig.getConsumerGroup());
        sBuilder.append(", FetchManager.isConsumePaused=").append(isConsumePaused());
        sBuilder.append(", MessageConsumer.shutdown=").append(isShutdown());
        if (!isShutdown()) {
            final long now = System.currentTimeMillis();
            final long lastTime = lastLogPrintTime.get();
            if ((lastFailureCount.incrementAndGet() <= MAX_FAILURE_LOG_TIMES)
                    || (lastTime <= 0 || now - lastTime > 30000)) {
                logger.warn(sBuilder.toString(), e);
                if (now - lastTime > 30000) {
                    lastLogPrintTime.set(now);
                    lastFailureCount.set(0);
                }
            }
        }
    }

}
