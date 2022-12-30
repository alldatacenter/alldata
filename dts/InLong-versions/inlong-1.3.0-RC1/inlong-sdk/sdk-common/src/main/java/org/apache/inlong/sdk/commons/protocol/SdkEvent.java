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

package org.apache.inlong.sdk.commons.protocol;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.flume.event.SimpleEvent;

/**
 * 
 * SdkEvent
 */
public class SdkEvent extends SimpleEvent {

    protected String inlongGroupId;
    protected String inlongStreamId;
    protected String uid;

    protected long msgTime;
    protected String sourceIp;

    /**
     * Constructor
     */
    public SdkEvent(){
    }
    
    /**
     * Constructor
     * 
     * @param inlongGroupId
     * @param inlongStreamId
     * @param body
     */
    public SdkEvent(String inlongGroupId, String inlongStreamId, byte[] body) {
        this.inlongGroupId = inlongGroupId;
        this.inlongStreamId = inlongStreamId;
        super.setBody(body);
        this.uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
        this.msgTime = System.currentTimeMillis();
        this.sourceIp = "127.0.0.1";
        Map<String, String> headers = super.getHeaders();
        headers.put(EventConstants.INLONG_GROUP_ID, inlongGroupId);
        headers.put(EventConstants.INLONG_STREAM_ID, inlongStreamId);
        headers.put(EventConstants.HEADER_KEY_MSG_TIME, String.valueOf(msgTime));
    }

    /**
     * Constructor
     * 
     * @param inlongGroupId
     * @param inlongStreamId
     * @param body
     */
    public SdkEvent(String inlongGroupId, String inlongStreamId, String body) {
        this(inlongGroupId, inlongStreamId, body.getBytes(Charset.defaultCharset()));
    }

    /**
     * get sourceIp
     * 
     * @return the sourceIp
     */
    public String getSourceIp() {
        return sourceIp;
    }

    /**
     * set sourceIp
     * 
     * @param sourceIp the sourceIp to set
     */
    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
        this.getHeaders().put(EventConstants.HEADER_KEY_SOURCE_IP, sourceIp);
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
     * get uid
     * 
     * @return the uid
     */
    public String getUid() {
        return uid;
    }

    /**
     * get msgTime
     * 
     * @return the msgTime
     */
    public long getMsgTime() {
        return msgTime;
    }

    /**
     * set inlongGroupId
     * 
     * @param inlongGroupId the inlongGroupId to set
     */
    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    /**
     * set inlongStreamId
     * 
     * @param inlongStreamId the inlongStreamId to set
     */
    public void setInlongStreamId(String inlongStreamId) {
        this.inlongStreamId = inlongStreamId;
    }

    /**
     * set uid
     * 
     * @param uid the uid to set
     */
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     * set msgTime
     * 
     * @param msgTime the msgTime to set
     */
    public void setMsgTime(long msgTime) {
        this.msgTime = msgTime;
    }

}
