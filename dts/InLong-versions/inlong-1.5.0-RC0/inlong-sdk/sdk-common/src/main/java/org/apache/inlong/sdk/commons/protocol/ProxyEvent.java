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

import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;

import java.util.Map;

/**
 * ProxyEvent
 */
public class ProxyEvent extends SdkEvent {

    protected long sourceTime;
    protected String topic;

    /**
     * Constructor
     */
    public ProxyEvent() {

    }

    /**
     * Constructor
     * 
     * @param inlongGroupId
     * @param inlongStreamId
     * @param body
     * @param msgTime
     * @param sourceIp
     */
    public ProxyEvent(String inlongGroupId, String inlongStreamId, byte[] body, long msgTime, String sourceIp) {
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        super.setBody(body);
        this.uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        this.msgTime = msgTime;
        this.sourceIp = sourceIp;
        Map<String, String> headers = super.getHeaders();
        headers.put(EventConstants.INLONG_GROUP_ID, inlongGroupId);
        headers.put(EventConstants.INLONG_STREAM_ID, inlongStreamId);
        headers.put(EventConstants.HEADER_KEY_MSG_TIME, String.valueOf(msgTime));
        headers.put(EventConstants.HEADER_KEY_SOURCE_IP, sourceIp);

        this.sourceTime = System.currentTimeMillis();
        this.getHeaders().put(EventConstants.HEADER_KEY_SOURCE_TIME, String.valueOf(sourceTime));
    }

    /**
     * Constructor
     * 
     * @param inlongGroupId
     * @param inlongStreamId
     * @param obj
     */
    public ProxyEvent(String inlongGroupId, String inlongStreamId, MessageObj obj) {
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        super.setBody(obj.getBody().toByteArray());
        this.uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        this.msgTime = obj.getMsgTime();
        this.sourceIp = obj.getSourceIp();
        Map<String, String> headers = super.getHeaders();
        headers.put(EventConstants.INLONG_GROUP_ID, inlongGroupId);
        headers.put(EventConstants.INLONG_STREAM_ID, inlongStreamId);
        headers.put(EventConstants.HEADER_KEY_MSG_TIME, String.valueOf(msgTime));
        headers.put(EventConstants.HEADER_KEY_SOURCE_IP, sourceIp);

        this.sourceTime = System.currentTimeMillis();
        this.getHeaders().put(EventConstants.HEADER_KEY_SOURCE_TIME, String.valueOf(sourceTime));
    }

    /**
     * get sourceTime
     * 
     * @return the sourceTime
     */
    public long getSourceTime() {
        return sourceTime;
    }

    /**
     * get topic
     * 
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * set topic
     * 
     * @param topic the topic to set
     */
    public void setTopic(String topic) {
        this.topic = topic;
        this.getHeaders().put(EventConstants.TOPIC, topic);
    }

    /**
     * set sourceTime
     * 
     * @param sourceTime the sourceTime to set
     */
    public void setSourceTime(long sourceTime) {
        this.sourceTime = sourceTime;
    }

}
