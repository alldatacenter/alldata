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

package org.apache.inlong.sdk.dataproxy.pb.channel;

import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.flume.ChannelException;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.AbstractChannel;
import org.apache.inlong.sdk.commons.protocol.SdkEvent;
import org.apache.inlong.sdk.dataproxy.pb.context.CallbackProfile;
import org.apache.inlong.sdk.dataproxy.pb.context.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * BufferQueueChannel
 */
public class BufferQueueChannel extends AbstractChannel {

    public static final Logger LOG = LoggerFactory.getLogger(BufferQueueChannel.class);

    public static final String KEY_MAX_BUFFERQUEUE_SIZE_KB = "channel.maxBufferQueueSizeKb";
    public static final int DEFAULT_MAX_BUFFERQUEUE_SIZE_KB = 128 * 1024;

    private Context context;
    // global buffer size
    private static SizeSemaphore globalBufferQueueSizeKb;
    private BufferQueue<CallbackProfile> bufferQueue;
    private ThreadLocal<ProfileTransaction> currentTransaction = new ThreadLocal<ProfileTransaction>();
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
        putCounter.incrementAndGet();
        int eventSize = event.getBody().length;
        boolean tryAcquire = this.bufferQueue.tryAcquire(eventSize);
        if (!tryAcquire) {
            throw new ChannelException("The buffer is full, please create more SDK object.");
        }
        ProfileTransaction transaction = currentTransaction.get();
        Preconditions.checkState(transaction != null, "No transaction exists for this thread");
        if (event instanceof CallbackProfile) {
            CallbackProfile profile = (CallbackProfile) event;
            transaction.doPut(profile);
        } else {
            SdkEvent sdkEvent = new SdkEvent();
            Map<String, String> headers = event.getHeaders();
            sdkEvent.setInlongGroupId(headers.get(Constants.INLONG_GROUP_ID));
            sdkEvent.setInlongStreamId(headers.get(Constants.INLONG_STREAM_ID));
            sdkEvent.setBody(event.getBody());
            sdkEvent.setHeaders(event.getHeaders());
            CallbackProfile profile = new CallbackProfile(sdkEvent, null);
            transaction.doPut(profile);
        }
    }

    /**
     * take
     * 
     * @return                  Event
     * @throws ChannelException
     */
    @Override
    public Event take() throws ChannelException {
        CallbackProfile event = this.bufferQueue.pollRecord();
        if (event != null) {
            ProfileTransaction transaction = currentTransaction.get();
            Preconditions.checkState(transaction != null, "No transaction exists for this thread");
            transaction.doTake(event);
            takeCounter.incrementAndGet();
        }
        return event;
    }

    /**
     * getTransaction
     * 
     * @return
     */
    @Override
    public Transaction getTransaction() {
        ProfileTransaction newTransaction = new ProfileTransaction(this.bufferQueue);
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
        long reloadInterval = context.getLong(Constants.RELOAD_INTERVAL, 60000L);
        TimerTask channelTask = new TimerTask() {

            public void run() {
                LOG.info("queueSize:{},availablePermits:{},put:{},take:{}",
                        bufferQueue.size(),
                        bufferQueue.availablePermits(),
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
        SizeSemaphore globalSize = getGlobalBufferQueueSizeKb(context);
        this.bufferQueue = new BufferQueue<>(globalSize.maxSize() / 3, globalSize);
    }

    /**
     * getGlobalBufferQueueSizeKb
     *
     * @return
     */
    public static SizeSemaphore getGlobalBufferQueueSizeKb(Context context) {
        if (globalBufferQueueSizeKb != null) {
            return globalBufferQueueSizeKb;
        }
        synchronized (LOG) {
            if (globalBufferQueueSizeKb != null) {
                return globalBufferQueueSizeKb;
            }
            int maxBufferQueueSizeKb = context.getInteger(KEY_MAX_BUFFERQUEUE_SIZE_KB, DEFAULT_MAX_BUFFERQUEUE_SIZE_KB);
            globalBufferQueueSizeKb = new SizeSemaphore(maxBufferQueueSizeKb, SizeSemaphore.ONEKB);
            return globalBufferQueueSizeKb;
        }
    }
}
