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

package org.apache.inlong.sdk.dataproxy.pb.dispatch;

import java.util.ArrayList;
import java.util.List;

import org.apache.inlong.sdk.dataproxy.pb.context.ProfileEvent;

/**
 * 
 * DispatchProfile
 */
public class DispatchProfile {

    private final String inlongGroupId;
    private final String inlongStreamId;
    private final String uid;
    private List<ProfileEvent> events = new ArrayList<>();
    private long createTime = System.currentTimeMillis();
    private long count = 0;
    private long size = 0;
    private long dispatchTime;
    private long sendTime;

    /**
     * Constructor
     * 
     * @param uid
     * @param inlongGroupId
     * @param inlongStreamId
     * @param dispatchTime
     */
    public DispatchProfile(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
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
    public boolean addEvent(ProfileEvent event, long maxPackCount, long maxPackSize) {
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
    public List<ProfileEvent> getEvents() {
        return events;
    }

    /**
     * set events
     * 
     * @param events the events to set
     */
    public void setEvents(List<ProfileEvent> events) {
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
     * get sendTime
     * 
     * @return the sendTime
     */
    public long getSendTime() {
        return sendTime;
    }

    /**
     * set sendTime
     * 
     * @param sendTime the sendTime to set
     */
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

}
