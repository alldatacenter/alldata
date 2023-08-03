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

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;

import org.apache.flume.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * DispatchProfile
 */
public class BatchPackProfile extends PackProfile {

    private List<ProxyEvent> events = new ArrayList<>();
    private BatchPackProfileCallback callback;

    /**
     * Constructor
     *
     * @param uid   the inlong id
     * @param inlongGroupId   the group id
     * @param inlongStreamId  the stream id
     * @param dispatchTime    the dispatch time
     */
    public BatchPackProfile(String uid, String inlongGroupId, String inlongStreamId, long dispatchTime) {
        super(uid, inlongGroupId, inlongStreamId, dispatchTime);
    }

    /**
     * addEvent
     * 
     * @param  event   the event to added
     * @param  maxPackCount   the max package count to cached
     * @param  maxPackSize    the max package size to cached
     * @return  whether added the event
     */
    public boolean addEvent(Event event, long maxPackCount, long maxPackSize) {
        long eventLength = event.getBody().length;
        if (count >= maxPackCount || (count > 0 && size + eventLength > maxPackSize)) {
            return false;
        }
        this.events.add((ProxyEvent) event);
        this.count++;
        this.size += eventLength;
        return true;
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
     * ack
     */
    public void ack() {
        if (callback != null) {
            callback.ack(this.events.size());
        }
    }

    /**
     * fail
     */
    public void fail(DataProxyErrCode errCode, String errMsg) {
        if (callback != null) {
            callback.fail();
        }
    }

    /**
     * isResend
     * @return  whether resend message
     */
    public boolean isResend() {
        return callback == null
                && enableRetryAfterFailure
                && (maxRetries < 0 || ++retries <= maxRetries);
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
