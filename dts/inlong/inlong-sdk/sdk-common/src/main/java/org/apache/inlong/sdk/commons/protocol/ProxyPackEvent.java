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

package org.apache.inlong.sdk.commons.protocol;

import org.apache.inlong.sdk.commons.protocol.ProxySdk.ResultCode;

import java.util.List;

/**
 * ProxyPackEvent
 */
public class ProxyPackEvent extends SdkEvent {

    protected long sourceTime;
    private List<ProxyEvent> events;
    private SourceCallback callback;

    /**
     * Constructor
     * @param inlongGroupId
     * @param inlongStreamId
     * @param events
     * @param callback
     */
    public ProxyPackEvent(String inlongGroupId, String inlongStreamId, List<ProxyEvent> events,
            SourceCallback callback) {
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        this.uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        this.sourceTime = System.currentTimeMillis();
        this.events = events;
        this.callback = callback;
    }

    /**
     * acknowledge
     * @param resultCode
     */
    public void acknowledge(ResultCode resultCode) {
        callback.callback(resultCode);
    }

    /**
     * get sourceTime
     * @return the sourceTime
     */
    public long getSourceTime() {
        return sourceTime;
    }

    /**
     * set sourceTime
     * @param sourceTime the sourceTime to set
     */
    public void setSourceTime(long sourceTime) {
        this.sourceTime = sourceTime;
    }

    /**
     * get events
     * @return the events
     */
    public List<ProxyEvent> getEvents() {
        return events;
    }

    /**
     * set events
     * @param events the events to set
     */
    public void setEvents(List<ProxyEvent> events) {
        this.events = events;
    }

    /**
     * get callback
     * @return the callback
     */
    public SourceCallback getCallback() {
        return callback;
    }

    /**
     * set callback
     * @param callback the callback to set
     */
    public void setCallback(SourceCallback callback) {
        this.callback = callback;
    }

}
