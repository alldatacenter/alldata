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

package org.apache.inlong.dataproxy.sink.mq;

import org.apache.flume.Context;
import org.apache.flume.event.SimpleEvent;
import org.apache.inlong.dataproxy.utils.BufferQueue;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxyPackEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BatchPackManager
 */
public class BatchPackManager {

    public static final Logger LOG = LoggerFactory.getLogger(BatchPackManager.class);
    public static final String KEY_DISPATCH_TIMEOUT = "dispatchTimeout";
    public static final String KEY_DISPATCH_MAX_PACKCOUNT = "dispatchMaxPackCount";
    public static final String KEY_DISPATCH_MAX_PACKSIZE = "dispatchMaxPackSize";
    public static final long DEFAULT_DISPATCH_TIMEOUT = 2000;
    public static final long DEFAULT_DISPATCH_MAX_PACKCOUNT = 256;
    public static final long DEFAULT_DISPATCH_MAX_PACKSIZE = 327680;
    public static final long MINUTE_MS = 60L * 1000;

    private final long dispatchTimeout;
    private final long maxPackCount;
    private final long maxPackSize;
    private BufferQueue<BatchPackProfile> dispatchQueue;
    private ConcurrentHashMap<String, BatchPackProfile> profileCache = new ConcurrentHashMap<>();
    // flag that manager need to output overtime data.
    private AtomicBoolean needOutputOvertimeData = new AtomicBoolean(false);
    private AtomicLong inCounter = new AtomicLong(0);
    private AtomicLong outCounter = new AtomicLong(0);

    /**
     * Constructor
     * 
     * @param context
     * @param dispatchQueue
     */
    public BatchPackManager(Context context, BufferQueue<BatchPackProfile> dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
        this.dispatchTimeout = context.getLong(KEY_DISPATCH_TIMEOUT, DEFAULT_DISPATCH_TIMEOUT);
        this.maxPackCount = context.getLong(KEY_DISPATCH_MAX_PACKCOUNT, DEFAULT_DISPATCH_MAX_PACKCOUNT);
        this.maxPackSize = context.getLong(KEY_DISPATCH_MAX_PACKSIZE, DEFAULT_DISPATCH_MAX_PACKSIZE);
    }

    /**
     * addEvent
     * @param event
     */
    public void addEvent(ProxyEvent event) {
        // parse
        String eventUid = event.getUid();
        long dispatchTime = event.getMsgTime() - event.getMsgTime() % MINUTE_MS;
        String dispatchKey = eventUid + "." + dispatchTime;
        // find dispatch profile
        BatchPackProfile dispatchProfile = this.profileCache.get(dispatchKey);
        if (dispatchProfile == null) {
            dispatchProfile = new BatchPackProfile(eventUid, event.getInlongGroupId(), event.getInlongStreamId(),
                    dispatchTime);
            this.profileCache.put(dispatchKey, dispatchProfile);
        }
        // add event
        boolean addResult = dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
        if (!addResult) {
            BatchPackProfile newDispatchProfile = new BatchPackProfile(eventUid, event.getInlongGroupId(),
                    event.getInlongStreamId(), dispatchTime);
            BatchPackProfile oldDispatchProfile = this.profileCache.put(dispatchKey, newDispatchProfile);
            this.dispatchQueue.acquire(oldDispatchProfile.getSize());
            this.dispatchQueue.offer(oldDispatchProfile);
            outCounter.addAndGet(dispatchProfile.getCount());
            newDispatchProfile.addEvent(event, maxPackCount, maxPackSize);
        }
        inCounter.incrementAndGet();
    }

    /**
     * addPackEvent
     * @param packEvent
     */
    public void addPackEvent(ProxyPackEvent packEvent) {
        String eventUid = packEvent.getUid();
        long dispatchTime = packEvent.getMsgTime() - packEvent.getMsgTime() % MINUTE_MS;
        BatchPackProfile dispatchProfile = new BatchPackProfile(eventUid, packEvent.getInlongGroupId(),
                packEvent.getInlongStreamId(), dispatchTime);
        // callback
        BatchPackProfileCallback callback = new BatchPackProfileCallback(packEvent.getEvents().size(),
                packEvent.getCallback());
        dispatchProfile.setCallback(callback);
        // offer queue
        for (ProxyEvent event : packEvent.getEvents()) {
            inCounter.incrementAndGet();
            boolean addResult = dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
            // dispatch profile is full
            if (!addResult) {
                outCounter.addAndGet(dispatchProfile.getCount());
                this.dispatchQueue.acquire(dispatchProfile.getSize());
                this.dispatchQueue.offer(dispatchProfile);
                dispatchProfile = new BatchPackProfile(eventUid, event.getInlongGroupId(), event.getInlongStreamId(),
                        dispatchTime);
                dispatchProfile.setCallback(callback);
                dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
            }
        }
        // last dispatch profile
        if (dispatchProfile.getEvents().size() > 0) {
            outCounter.addAndGet(dispatchProfile.getCount());
            this.dispatchQueue.acquire(dispatchProfile.getSize());
            this.dispatchQueue.offer(dispatchProfile);
        }
    }

    /**
     * addSimpleEvent
     * @param event
     */
    public void addSimpleEvent(SimpleEvent event) {
        BatchPackProfile dispatchProfile = SimpleBatchPackProfileV0.create(event);
        this.dispatchQueue.acquire(dispatchProfile.getSize());
        this.dispatchQueue.offer(dispatchProfile);
        outCounter.addAndGet(dispatchProfile.getCount());
        inCounter.incrementAndGet();
    }

    /**
     * outputOvertimeData
     * 
     * @return
     */
    public void outputOvertimeData() {
        if (!needOutputOvertimeData.getAndSet(false)) {
            return;
        }
        LOG.debug("start to outputOvertimeData profileCacheSize:{},dispatchQueueSize:{}",
                profileCache.size(), dispatchQueue.size());
        long currentTime = System.currentTimeMillis();
        long createThreshold = currentTime - dispatchTimeout;
        List<String> removeKeys = new ArrayList<>();
        long eventCount = 0;
        for (Entry<String, BatchPackProfile> entry : this.profileCache.entrySet()) {
            BatchPackProfile dispatchProfile = entry.getValue();
            eventCount += dispatchProfile.getCount();
            if (!dispatchProfile.isTimeout(createThreshold)) {
                continue;
            }
            removeKeys.add(entry.getKey());
        }
        // output
        removeKeys.forEach((key) -> {
            BatchPackProfile dispatchProfile = this.profileCache.remove(key);
            if (dispatchProfile != null) {
                this.dispatchQueue.acquire(dispatchProfile.getSize());
                dispatchQueue.offer(dispatchProfile);
                outCounter.addAndGet(dispatchProfile.getCount());
            }
        });
        LOG.debug("end to outputOvertimeData profileCacheSize:{},dispatchQueueSize:{},eventCount:{},"
                + "inCounter:{},outCounter:{}",
                profileCache.size(), dispatchQueue.size(), eventCount,
                inCounter.getAndSet(0), outCounter.getAndSet(0));
    }

    /**
     * get dispatchTimeout
     * 
     * @return the dispatchTimeout
     */
    public long getDispatchTimeout() {
        return dispatchTimeout;
    }

    /**
     * get maxPackCount
     * 
     * @return the maxPackCount
     */
    public long getMaxPackCount() {
        return maxPackCount;
    }

    /**
     * get maxPackSize
     * 
     * @return the maxPackSize
     */
    public long getMaxPackSize() {
        return maxPackSize;
    }

    /**
     * setNeedOutputOvertimeData
     */
    public void setNeedOutputOvertimeData() {
        this.needOutputOvertimeData.getAndSet(true);
    }
}
