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

package org.apache.inlong.tubemq.corebase;

import java.io.Serializable;
import java.util.Arrays;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * Message is a message object class passed in the Tube. The data of the
 * service setting is passed from the production end to the message receiving
 * end as it is. The attribute content is a field shared with the Tube system.
 * The content filled in the service is not lost or rewritten, but the field
 * has The contents of the Tube system may be added, and in subsequent versions,
 * the contents of the newly added Tube system may be removed without being
 * notified. This part needs to pay attention to the
 * Message.putSystemHeader(final String msgType, final String msgTime) interface,
 * which is used to set the message type and message sending time of the message,
 * msgType is used for consumer filtering, and msgTime is used as the pipe to send
 * and receive statistics. When the message time statistics dimension is used.
 */
public class Message implements Serializable {
    static final long serialVersionUID = -1L;
    protected int flag;
    private long indexId;
    // tube topic
    private String topic;
    // data body
    private byte[] data;
    private String attribute;
    // message type
    private transient String msgType;
    // message time
    private transient String msgTime;
    private transient String sysAttributes;

    /**
     * init with topic and data body
     *
     * @param topic  topic name
     * @param data   message content
     */
    public Message(final String topic, final byte[] data) {
        super();
        this.topic = topic;
        this.data = data;
    }

    /**
     * init with indexId topic data attribute flag
     *
     * @param indexId    index id
     * @param topic      topic name
     * @param data       message content
     * @param attribute  message attribute
     * @param flag       message flag
     */
    protected Message(long indexId, String topic, byte[] data, String attribute, int flag) {
        this.indexId = indexId;
        this.topic = topic;
        this.data = data;
        this.attribute = attribute;
        this.flag = flag;
    }

    public int getFlag() {
        return this.flag;
    }

    public String getMsgType() {
        if (TStringUtils.isBlank(this.msgType)) {
            if (TStringUtils.isNotBlank(this.attribute)
                    && attribute.contains(TokenConstants.TOKEN_MSG_TYPE)) {
                parseSystemHeader();
            }
        }
        return this.msgType;
    }

    public String getMsgTime() {
        if (TStringUtils.isBlank(this.msgTime)) {
            if (TStringUtils.isNotBlank(this.attribute)
                    && attribute.contains(TokenConstants.TOKEN_MSG_TIME)) {
                parseSystemHeader();
            }
        }
        return msgTime;
    }

    /**
     * Set the filtering items that need to be filtered and the message forwarding time
     *
     * @param msgType   the ID of the stream that needs to be filtered,
     *                 if not required, set null
     * @param msgTime   the time when the message was sent, the format is yyyyMMddHHmm,
     *                  if not required, set null
     */
    public void putSystemHeader(final String msgType, final String msgTime) {
        this.msgType = null;
        this.msgTime = null;
        this.sysAttributes = null;
        StringBuilder strBuff = new StringBuilder(512);
        // valid parameters
        if (TStringUtils.isNotBlank(msgType)) {
            this.msgType = msgType.trim();
            this.sysAttributes = strBuff.append(TokenConstants.TOKEN_MSG_TYPE)
                    .append(TokenConstants.EQ).append(this.msgType).toString();
            strBuff.delete(0, strBuff.length());
        }
        if (TStringUtils.isNotBlank(msgTime)) {
            String tmpMsgTime = msgTime.trim();
            if (tmpMsgTime.length() != DateTimeConvertUtils.LENGTH_YYYYMMDDHHMM) {
                throw new IllegalArgumentException(strBuff
                        .append("Illegal parameter: msgTime's value must '")
                        .append(DateTimeConvertUtils.PAT_YYYYMMDDHHMM)
                        .append("' format and length must equal ")
                        .append(DateTimeConvertUtils.LENGTH_YYYYMMDDHHMM).toString());
            }
            try {
                DateTimeConvertUtils.yyyyMMddHHmm2ms(tmpMsgTime);
            } catch (Throwable ex) {
                throw new IllegalArgumentException(strBuff
                        .append("Illegal parameter: msgTime's value parse failure: ")
                        .append(ex.getMessage()).toString());
            }
            this.msgTime = tmpMsgTime;
            if (TStringUtils.isNotEmpty(this.sysAttributes)) {
                strBuff.append(sysAttributes).append(TokenConstants.ARRAY_SEP);
            }
            this.sysAttributes = strBuff.append(TokenConstants.TOKEN_MSG_TIME)
                    .append(TokenConstants.EQ).append(this.msgTime).toString();
            strBuff.delete(0, strBuff.length());
        }
        // rebuild attributes
        if (TStringUtils.isNotEmpty(this.sysAttributes)) {
            strBuff.append(this.sysAttributes);
        }
        if (TStringUtils.isNotBlank(this.attribute)) {
            String[] strAttrs = this.attribute.split(TokenConstants.ARRAY_SEP);
            for (String strAttrItem : strAttrs) {
                if (strAttrItem != null && !(strAttrItem.contains(TokenConstants.TOKEN_MSG_TYPE)
                        || strAttrItem.contains(TokenConstants.TOKEN_MSG_TIME))) {
                    if (strBuff.length() == 0) {
                        strBuff.append(strAttrItem);
                    } else {
                        strBuff.append(TokenConstants.ARRAY_SEP).append(strAttrItem);
                    }
                }
            }
        }
        this.attribute = strBuff.toString();
    }

    public boolean hasAttribute() {
        return this.attribute != null;
    }

    public long getIndexId() {
        return this.indexId;
    }

    public String getTopic() {
        return this.topic;
    }

    /**
     * Set the message's topic
     */
    public void setTopic(String topicName) {
        this.topic = topicName;
    }

    public byte[] getData() {
        return this.data;
    }

    /**
     * Set the message's payload
     */
    public void setData(final byte[] data) {
        this.data = data;
    }

    public void clearAttribute() {
        this.attribute = "";
    }

    public String getAttribute() {
        return this.attribute;
    }

    /**
     * Get key's value from attribute by key name
     *
     * @param keyVal   the key name
     * @return  the key's value of the key name stored in attribute
     */
    public String getAttrValue(final String keyVal) {
        if (TStringUtils.isBlank(keyVal)) {
            throw new IllegalArgumentException("keyVal's value is blank!");
        }
        if (TStringUtils.isBlank(this.attribute)) {
            return null;
        }
        String[] strAttrs = this.attribute.split(TokenConstants.ARRAY_SEP);
        for (String strAttrItem : strAttrs) {
            if (TStringUtils.isNotBlank(strAttrItem)) {
                String[] strItems = strAttrItem.split(TokenConstants.EQ);
                if (strItems.length > 1 && !TStringUtils.isBlank(strItems[0])) {
                    if (keyVal.equals(strItems[0])) {
                        return strItems[1];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Store key and value in attribute
     *
     * @param keyVal    the key name
     * @param valueVal  the key value
     */
    public void setAttrKeyVal(final String keyVal, final String valueVal) {
        if (TStringUtils.isBlank(keyVal)) {
            throw new IllegalArgumentException("keyVal's value is blank!");
        }
        if (TStringUtils.isBlank(valueVal)) {
            throw new IllegalArgumentException("valueVal's value is blank!");
        }
        if (keyVal.contains(TokenConstants.TOKEN_MSG_TYPE)
                || keyVal.contains(TokenConstants.TOKEN_MSG_TIME)
                || valueVal.contains(TokenConstants.TOKEN_MSG_TYPE)
                || valueVal.contains(TokenConstants.TOKEN_MSG_TIME)) {
            throw new IllegalArgumentException(new StringBuilder(512).append("System Headers(")
                    .append(TokenConstants.TOKEN_MSG_TYPE).append(",")
                    .append(TokenConstants.TOKEN_MSG_TIME)
                    .append(") are reserved tokens, can't include in keyVal or valueVal!").toString());
        }
        if ((keyVal.contains(TokenConstants.ARRAY_SEP)
                || keyVal.contains(TokenConstants.EQ))
                || (valueVal.contains(TokenConstants.ARRAY_SEP)
                || valueVal.contains(TokenConstants.EQ))) {
            throw new IllegalArgumentException(new StringBuilder(512).append("(")
                    .append(TokenConstants.ARRAY_SEP).append(",")
                    .append(TokenConstants.EQ).append(
                            ") are reserved tokens, can't include in keyVal or valueVal!").toString());
        }
        if (TStringUtils.isBlank(this.attribute)) {
            this.attribute = keyVal + TokenConstants.EQ + valueVal;
            return;
        }
        int count = 0;
        StringBuilder sBuilder = new StringBuilder(512);
        String[] strAttrs = this.attribute.split(TokenConstants.ARRAY_SEP);
        for (String strAttrItem : strAttrs) {
            if (TStringUtils.isNotBlank(strAttrItem)) {
                String[] strItems = strAttrItem.split(TokenConstants.EQ);
                if (strItems.length > 1 && !TStringUtils.isBlank(strItems[0])) {
                    if (!keyVal.equals(strItems[0])) {
                        if (count++ > 0) {
                            sBuilder.append(TokenConstants.ARRAY_SEP);
                        }
                        sBuilder.append(strAttrItem);
                    }
                }
            }
        }
        if (count > 0) {
            sBuilder.append(TokenConstants.ARRAY_SEP);
        }
        sBuilder.append(keyVal).append(TokenConstants.EQ).append(valueVal);
        this.attribute = sBuilder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.attribute == null ? 0 : this.attribute.hashCode());
        result = prime * result + Arrays.hashCode(this.data);
        result = prime * result + (int) (this.indexId ^ this.indexId >>> 32);
        result = prime * result + (this.topic == null ? 0 : this.topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        if (this.attribute == null) {
            if (other.attribute != null) {
                return false;
            }
        } else if (!this.attribute.equals(other.attribute)) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if (this.indexId != other.indexId) {
            return false;
        }
        if (this.topic == null) {
            if (other.topic != null) {
                return false;
            }
        } else if (!this.topic.equals(other.topic)) {
            return false;
        }
        return true;
    }

    private void parseSystemHeader() {
        if (TStringUtils.isBlank(this.attribute)) {
            return;
        }
        if (attribute.contains(TokenConstants.TOKEN_MSG_TYPE)
                || attribute.contains(TokenConstants.TOKEN_MSG_TIME)) {
            String[] strAttrs = this.attribute.split(TokenConstants.ARRAY_SEP);
            for (String strAttrItem : strAttrs) {
                if (TStringUtils.isNotBlank(strAttrItem)) {
                    if (strAttrItem.contains(TokenConstants.TOKEN_MSG_TYPE)) {
                        String[] strItems = strAttrItem.split(TokenConstants.EQ);
                        if (strItems.length > 1) {
                            this.msgType = strItems[1];
                        }
                    } else if (strAttrItem.contains(TokenConstants.TOKEN_MSG_TIME)) {
                        String[] strItems = strAttrItem.split(TokenConstants.EQ);
                        if (strItems.length > 1) {
                            try {
                                DateTimeConvertUtils.yyyyMMddHHmm2ms(strItems[1]);
                                this.msgTime = strItems[1];
                            } catch (Throwable ex) {
                                this.msgTime = TStringUtils.EMPTY;
                            }
                        }
                    }
                }
            }
            if (TStringUtils.isNotBlank(this.msgType)) {
                this.sysAttributes = TokenConstants.TOKEN_MSG_TYPE + TokenConstants.EQ + this.msgType;
            }
            if (TStringUtils.isNotBlank(this.msgTime)) {
                if (TStringUtils.isBlank(this.sysAttributes)) {
                    this.sysAttributes = TokenConstants.TOKEN_MSG_TIME + TokenConstants.EQ + this.msgTime;
                } else {
                    this.sysAttributes += TokenConstants.ARRAY_SEP + TokenConstants.TOKEN_MSG_TIME
                            + TokenConstants.EQ + this.msgTime;
                }
            }
        }
    }
}
