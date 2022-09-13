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

import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;

/**
 * 
 * SortEvent
 */
public class SortEvent extends ProxyEvent {

    protected String messageKey;
    protected String messageOffset;
    protected long sendTime;

    /**
     * Constructor
     */
    public SortEvent() {

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
    public SortEvent(String inlongGroupId, String inlongStreamId, byte[] body, long msgTime, String sourceIp) {
        super(inlongGroupId, inlongStreamId, body, msgTime, sourceIp);
    }

    /**
     * Constructor
     * 
     * @param inlongGroupId
     * @param inlongStreamId
     * @param obj
     */
    public SortEvent(String inlongGroupId, String inlongStreamId, MessageObj obj) {
        super(inlongGroupId, inlongStreamId, obj);
    }

    /**
     * get messageKey
     * 
     * @return the messageKey
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * set messageKey
     * 
     * @param messageKey the messageKey to set
     */
    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * get messageOffset
     * 
     * @return the messageOffset
     */
    public String getMessageOffset() {
        return messageOffset;
    }

    /**
     * set messageOffset
     * 
     * @param messageOffset the messageOffset to set
     */
    public void setMessageOffset(String messageOffset) {
        this.messageOffset = messageOffset;
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
