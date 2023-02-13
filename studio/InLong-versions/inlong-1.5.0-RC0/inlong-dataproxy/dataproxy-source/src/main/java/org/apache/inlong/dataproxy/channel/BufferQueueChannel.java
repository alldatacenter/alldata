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

package org.apache.inlong.dataproxy.channel;

import com.google.common.base.Preconditions;

import org.apache.flume.ChannelException;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.AbstractChannel;
import org.apache.inlong.dataproxy.utils.BufferQueue;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BufferQueueChannel
 */
public class BufferQueueChannel extends AbstractChannel {

    public static final Logger LOG = LoggerFactory.getLogger(BufferQueueChannel.class);

    public static final String KEY_MAX_BUFFERQUEUE_COUNT = "maxBufferQueueCount";
    // average message size is 1KB.
    public static final int DEFAULT_MAX_BUFFERQUEUE_COUNT = 128 * 1024;
    public static final String KEY_MAX_BUFFERQUEUE_SIZE_KB = "maxBufferQueueSizeKb";
    public static final int DEFAULT_MAX_BUFFERQUEUE_SIZE_KB = 128 * 1024;
    public static final String KEY_RELOADINTERVAL = "reloadInterval";

    private Context context;
    private int maxBufferQueueCount;
    private Semaphore countSemaphore;
    private int maxBufferQueueSizeKb;
    private BufferQueue<ProxyEvent> bufferQueue;
    private ThreadLocal<ProxyTransaction> currentTransaction = new ThreadLocal<ProxyTransaction>();
    protected Timer channelTimer;
    private AtomicLong takeCounter = new AtomicLong(0);
    private AtomicLong putCounter = new AtomicLong(0);

    /**
     * Constructor
     */
    public BufferQueueChannel() {
    }

    /**
     * put
     *
     * @param  event
     * @throws ChannelException
     */
    @Override
    public void put(Event event) throws ChannelException {
        if (event instanceof ProxyEvent) {
            putCounter.incrementAndGet();
            int eventSize = event.getBody().length;
            this.countSemaphore.acquireUninterruptibly();
            this.bufferQueue.acquire(eventSize);
            ProxyTransaction transaction = currentTransaction.get();
            Preconditions.checkState(transaction != null, "No transaction exists for this thread");
            ProxyEvent profile = (ProxyEvent) event;
            transaction.doPut(profile);
        }
    }

    /**
     * take
     *
     * @return Event
     * @throws ChannelException
     */
    @Override
    public Event take() throws ChannelException {
        ProxyEvent event = this.bufferQueue.pollRecord();
        if (event != null) {
            ProxyTransaction transaction = currentTransaction.get();
            Preconditions.checkState(transaction != null, "No transaction exists for this thread");
            transaction.doTake(event);
            takeCounter.incrementAndGet();
        }
        return event;
    }

    /**
     * getTransaction
     *
     * @return new transaction
     */
    @Override
    public Transaction getTransaction() {
        ProxyTransaction newTransaction = new ProxyTransaction(this.countSemaphore, this.bufferQueue);
        this.currentTransaction.set(newTransaction);
        return newTransaction;
    }

    /**
     * start
     */
    @Override
    public void start() {
        super.start();
        try {
            this.setReloadTimer();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * setReloadTimer
     */
    protected void setReloadTimer() {
        channelTimer = new Timer(true);
        long reloadInterval = context.getLong(KEY_RELOADINTERVAL, 60000L);
        TimerTask channelTask = new TimerTask() {

            public void run() {
                LOG.info("queueSize:{},availablePermits:{},maxBufferQueueCount:{},availablePermits:{},put:{},take:{}",
                        bufferQueue.size(),
                        bufferQueue.availablePermits(),
                        maxBufferQueueCount,
                        countSemaphore.availablePermits(),
                        putCounter.getAndSet(0),
                        takeCounter.getAndSet(0));
            }
        };
        channelTimer.schedule(channelTask,
                new Date(System.currentTimeMillis() + reloadInterval),
                reloadInterval);
    }

    /**
     * configure
     *
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
        this.maxBufferQueueCount = context.getInteger(KEY_MAX_BUFFERQUEUE_COUNT, DEFAULT_MAX_BUFFERQUEUE_COUNT);
        this.countSemaphore = new Semaphore(maxBufferQueueCount, true);
        this.maxBufferQueueSizeKb = context.getInteger(KEY_MAX_BUFFERQUEUE_SIZE_KB, DEFAULT_MAX_BUFFERQUEUE_SIZE_KB);
        this.bufferQueue = new BufferQueue<>(maxBufferQueueSizeKb);
    }
}
