/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.model.notification;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Represents a notification message that is associated with a version.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasNotificationMessage<T> extends AtlasNotificationBaseMessage {
    private String  msgSourceIP;
    private String  msgCreatedBy;
    private long    msgCreationTime;
    private boolean spooled;

    /**
     * The actual message.
     */
    private T message;


    // ----- Constructors ----------------------------------------------------
    public AtlasNotificationMessage() {
    }

    public AtlasNotificationMessage(MessageVersion version, T message) {
        this(version, message, null, null, false);
    }

    public AtlasNotificationMessage(MessageVersion version, T message, String msgSourceIP, String createdBy, boolean spooled) {
        this(version, message, msgSourceIP, createdBy, spooled, null);
    }

    public AtlasNotificationMessage(MessageVersion version, T message, String msgSourceIP, String createdBy, boolean spooled, MessageSource source) {
        super(version, source);

        this.msgSourceIP     = msgSourceIP;
        this.msgCreatedBy    = createdBy;
        this.msgCreationTime = (new Date()).getTime();
        this.message         = message;
        this.spooled         = spooled;
    }

    public AtlasNotificationMessage(MessageVersion version, T message, String msgSourceIP, String createdBy) {
        this(version, message, msgSourceIP, createdBy, false, null);
    }

    public String getMsgSourceIP() {
        return msgSourceIP;
    }

    public void setMsgSourceIP(String msgSourceIP) {
        this.msgSourceIP = msgSourceIP;
    }

    public String getMsgCreatedBy() {
        return msgCreatedBy;
    }

    public void setMsgCreatedBy(String msgCreatedBy) {
        this.msgCreatedBy = msgCreatedBy;
    }

    public long getMsgCreationTime() {
        return msgCreationTime;
    }

    public void setMsgCreationTime(long msgCreationTime) {
        this.msgCreationTime = msgCreationTime;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public void setSpooled(boolean val) {
        this.spooled = val;
    }

    public boolean getSpooled() {
        return spooled;
    }
}
