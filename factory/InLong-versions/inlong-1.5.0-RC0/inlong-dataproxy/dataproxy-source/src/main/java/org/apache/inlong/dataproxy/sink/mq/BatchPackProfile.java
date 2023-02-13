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

import org.apache.inlong.sdk.commons.protocol.ProxyEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * DispatchProfile
 */
public class BatchPackProfile {

    public static final long MINUTE_MS = 60L * 1000;

    private final String inlongGroupId;
    private final String inlongStreamId;
    private final String uid;
    private List<ProxyEvent> events = new ArrayList<>();
    private long createTime = System.currentTimeMillis();
    private long count = 0;
    private long size = 0;
    private long dispatchTime;
    private BatchPackProfileCallback callback;

    /**
     * Constructor
     * 
     * @param uid
     * @param inlongGroupId
     * @param inlongStreamId
     * @param dispatchTime
     */
    public BatchPackProfile(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
        this.uid = uid;
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        this.dispatchTime = dispatchTime;
    }

    /**
     * addEvent
     * 
     * @param  event
     * @param  maxPackCount
     * @param  maxPackSize
     * @return
     */
    public boolean addEvent(ProxyEvent event, long maxPackCount, long maxPackSize) {
        long eventLength = event.getBody().length;
        if (count >= maxPackCount || (count > 0 && size + eventLength > maxPackSize)) {
            return false;
        }
        this.events.add(event);
        this.count++;
        this.size += eventLength;
        return true;
    }

    /**
     * isTimeout
     * 
     * @param  createThreshold
     * @return
     */
    public boolean isTimeout(long createThreshold) {
        return createThreshold >= createTime;
    }

    /**
     * get uid
     * 
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * get events
     * 
     * @return the events
     */
    public List<ProxyEvent> getEvents() {
        return events;
    }

    /**
     * set events
     * 
     * @param events the events to set
     */
    public void setEvents(List<ProxyEvent> events) {
        this.events = events;
    }

    /**
     * get count
     * 
     * @return the count
     */
    public long getCount() {
        return count;
    }

    /**
     * set count
     * 
     * @param count the count to set
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     * get size
     * 
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * set size
     * 
     * @param size the size to set
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * get inlongGroupId
     * 
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * get inlongStreamId
     * 
     * @return the inlongStreamId
     */
    public String getInlongStreamId() {
        return inlongStreamId;
    }

    /**
     * getDispatchTime
     * 
     * @return
     */
    public long getDispatchTime() {
        return dispatchTime;
    }

    /**
     * ack
     */
    public void ack() {
        if (callback != null) {
            callback.ack(this.events.size());
        }
    }

    /**
     * fail
     * @return
     */
    public void fail() {
        if (callback != null) {
            callback.fail();
        }
    }

    /**
     * isResend
     * @return
     */
    public boolean isResend() {
        return callback == null;
    }

    /**
     * get callback
     * @return the callback
     */
    public BatchPackProfileCallback getCallback() {
        return callback;
    }

    /**
     * set callback
     * @param callback the callback to set
     */
    public void setCallback(BatchPackProfileCallback callback) {
        this.callback = callback;
    }

}
