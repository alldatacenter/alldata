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

package org.apache.inlong.sort.standalone.dispatch;

import org.apache.flume.Context;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DispatchManager
 */
public class DispatchManager {

    public static final Logger LOG = InlongLoggerFactory.getLogger(DispatchManager.class);
    public static final String KEY_DISPATCH_TIMEOUT = "dispatchTimeout";
    public static final String KEY_DISPATCH_MAX_PACKCOUNT = "dispatchMaxPackCount";
    public static final String KEY_DISPATCH_MAX_PACKSIZE = "dispatchMaxPackSize";
    public static final String KEY_DISPATCH_AHEAD_TIME = "dispatchAheadTime";
    public static final String KEY_DISPATCH_DELAY_TIME = "dispatchDelayTime";
    public static final long DEFAULT_DISPATCH_TIMEOUT = 2000;
    public static final long DEFAULT_DISPATCH_MAX_PACKCOUNT = 256;
    public static final long DEFAULT_DISPATCH_MAX_PACKSIZE = 327680;
    public static final long MINUTE_MS = 60L * 1000;
    public static final long DEFAULT_DISPATCH_AHEAD_TIME = 60 * 60 * 1000L;
    public static final long DEFAULT_DISPATCH_DELAY_TIME = 60 * 60 * 1000 * 16L;
    private final long dispatchTimeout;
    private final long maxPackCount;
    private final long maxPackSize;
    private final long dispatchAheadTime;
    private final long dispatchDelayTime;
    private LinkedBlockingQueue<DispatchProfile> dispatchQueue;
    private ConcurrentHashMap<String, DispatchProfile> profileCache = new ConcurrentHashMap<>();
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
    public DispatchManager(Context context, LinkedBlockingQueue<DispatchProfile> dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
        this.dispatchTimeout = context.getLong(KEY_DISPATCH_TIMEOUT, DEFAULT_DISPATCH_TIMEOUT);
        this.maxPackCount = context.getLong(KEY_DISPATCH_MAX_PACKCOUNT, DEFAULT_DISPATCH_MAX_PACKCOUNT);
        this.maxPackSize = context.getLong(KEY_DISPATCH_MAX_PACKSIZE, DEFAULT_DISPATCH_MAX_PACKSIZE);
        this.dispatchAheadTime = context.getLong(KEY_DISPATCH_AHEAD_TIME, DEFAULT_DISPATCH_AHEAD_TIME);
        this.dispatchDelayTime = -1 * context.getLong(KEY_DISPATCH_DELAY_TIME, DEFAULT_DISPATCH_DELAY_TIME);
    }

    /**
     * addEvent
     * 
     * @param event
     */
    public void addEvent(ProfileEvent event) {
        // parse
        String eventUid = event.getUid();
        long dispatchTime = event.getRawLogTime() - event.getRawLogTime() % MINUTE_MS;
        String dispatchKey = eventUid + "." + dispatchTime;
        //
        DispatchProfile dispatchProfile = this.profileCache.get(dispatchKey);
        if (dispatchProfile == null) {
            dispatchProfile = new DispatchProfile(eventUid, event.getInlongGroupId(), event.getInlongStreamId(),
                    dispatchTime);
            this.profileCache.put(dispatchKey, dispatchProfile);
        }
        //
        boolean addResult = dispatchProfile.addEvent(event, maxPackCount, maxPackSize);
        if (!addResult) {
            DispatchProfile newDispatchProfile = new DispatchProfile(eventUid, event.getInlongGroupId(),
                    event.getInlongStreamId(), dispatchTime);
            DispatchProfile oldDispatchProfile = this.profileCache.put(dispatchKey, newDispatchProfile);
            long curTime = System.currentTimeMillis();
            this.checkAndResetDispatchTime(dispatchProfile, curTime);
            this.dispatchQueue.offer(oldDispatchProfile);
            outCounter.addAndGet(dispatchProfile.getCount());
            newDispatchProfile.addEvent(event, maxPackCount, maxPackSize);
        }
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
        for (Entry<String, DispatchProfile> entry : this.profileCache.entrySet()) {
            DispatchProfile dispatchProfile = entry.getValue();
            eventCount += dispatchProfile.getCount();
            if (!dispatchProfile.isTimeout(createThreshold)) {
                continue;
            }
            removeKeys.add(entry.getKey());
        }
        // output
        long curTime = System.currentTimeMillis();
        removeKeys.forEach((key) -> {
            DispatchProfile dispatchProfile = this.profileCache.remove(key);
            if (dispatchProfile != null) {
                this.checkAndResetDispatchTime(dispatchProfile, curTime);
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
     * reset dispatch time if the dispatch time is invalid.
     * The default ahead time is 1 hour, and default delay time is 16 hours.
     */
    private void checkAndResetDispatchTime(DispatchProfile dispatchProfile, long cur) {
        long diff = dispatchProfile.getDispatchTime() - cur;
        if (dispatchAheadTime <= diff || diff <= dispatchDelayTime) {
            dispatchProfile.setDispatchTime(cur - cur % MINUTE_MS);
        }
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
