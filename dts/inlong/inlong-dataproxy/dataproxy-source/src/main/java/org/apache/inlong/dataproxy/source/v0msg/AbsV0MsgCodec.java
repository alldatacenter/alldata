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

package org.apache.inlong.dataproxy.source.v0msg;

import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.enums.DataProxyMsgEncType;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.consts.StatConstants;
import org.apache.inlong.dataproxy.source.BaseSource;
import org.apache.inlong.dataproxy.utils.DateTimeUtils;
import org.apache.inlong.sdk.commons.protocol.EventConstants;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Event;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class AbsV0MsgCodec {

    // string splitter
    protected static final Splitter.MapSplitter mapSplitter = Splitter
            .on(AttributeConstants.SEPARATOR).trimResults()
            .withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);
    // map joiner
    protected static final Joiner.MapJoiner mapJoiner = Joiner.on(AttributeConstants.SEPARATOR)
            .withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);
    protected final Map<String, String> attrMap = new HashMap<>();
    protected DataProxyErrCode errCode = DataProxyErrCode.UNKNOWN_ERROR;
    protected String errMsg = "";
    protected String strRemoteIP;
    protected long msgRcvTime;
    protected int totalDataLen;
    protected byte msgType;
    protected int msgCount;
    protected String origAttr = "";
    protected byte[] bodyData;
    protected byte[] origBody = null;
    protected long dataTimeMs;
    protected String groupId;
    protected String streamId = "";
    protected String topicName;
    protected String msgSeqId = "";
    protected long uniq = -1L;
    protected boolean isOrderOrProxy = false;
    protected String msgProcType = "b2b";
    protected boolean needResp = true;

    public AbsV0MsgCodec(int totalDataLen, int msgTypeValue,
            long msgRcvTime, String strRemoteIP) {
        this.totalDataLen = totalDataLen;
        this.msgType = (byte) (msgTypeValue & 0xFF);
        this.msgRcvTime = msgRcvTime;
        this.strRemoteIP = strRemoteIP;
    }

    public abstract boolean descMsg(BaseSource source, ByteBuf cb) throws Exception;

    public abstract boolean validAndFillFields(BaseSource source, StringBuilder strBuff);

    public abstract Event encEventPackage(BaseSource source, Channel channel);

    public DataProxyErrCode getErrCode() {
        return this.errCode;
    }

    public String getErrMsg() {
        return this.errMsg;
    }

    public boolean isNeedResp() {
        return this.needResp;
    }

    public boolean isOrderOrProxy() {
        return isOrderOrProxy;
    }

    public byte getMsgType() {
        return this.msgType;
    }

    public String getAttr() {
        return this.origAttr;
    }

    public Map<String, String> getAttrMap() {
        return this.attrMap;
    }

    public long getUniq() {
        return this.uniq;
    }

    public long getDataTimeMs() {
        return this.dataTimeMs;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getStreamId() {
        return this.streamId;
    }

    public String getTopicName() {
        return this.topicName;
    }

    public String getMsgProcType() {
        return this.msgProcType;
    }

    public int getBodyLength() {
        return this.bodyData == null ? 0 : this.bodyData.length;
    }

    public int getMsgCount() {
        return this.msgCount;
    }

    public String getStrRemoteIP() {
        return strRemoteIP;
    }

    public byte[] getOrigBody() {
        return origBody;
    }

    public long getMsgRcvTime() {
        return msgRcvTime;
    }

    public void setSuccessInfo() {
        this.errCode = DataProxyErrCode.SUCCESS;
        this.errMsg = "";
    }

    public void setFailureInfo(DataProxyErrCode errCode) {
        setFailureInfo(errCode, "");
    }

    public void setFailureInfo(DataProxyErrCode errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    protected boolean decAttrInfo(BaseSource source, ByteBuf cb,
            int attrLen, int attrPos) throws Exception {
        // get attr bytes
        if (attrLen > 0) {
            byte[] attrData = new byte[attrLen];
            cb.getBytes(attrPos, attrData, 0, attrLen);
            try {
                this.origAttr = new String(attrData, StandardCharsets.UTF_8);
            } catch (Throwable err) {
                //
            }
        }
        // parse attribute field
        if (StringUtils.isNotBlank(this.origAttr)) {
            try {
                this.attrMap.putAll(mapSplitter.split(this.origAttr));
            } catch (Exception e) {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_ATTR_INVALID);
                this.errCode = DataProxyErrCode.SPLIT_ATTR_ERROR;
                this.errMsg = String.format("Parse attr (%s) failure", this.origAttr);
                return false;
            }
        }
        // get whether return request
        if ("false".equalsIgnoreCase(attrMap.get(AttributeConstants.MESSAGE_IS_ACK))) {
            this.needResp = false;
        }
        // get whether sync send
        if ("true".equalsIgnoreCase(attrMap.get(AttributeConstants.MESSAGE_SYNC_SEND))) {
            if (!this.needResp) {
                this.needResp = true;
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_ORDER_ACK_INVALID);
                this.errCode = DataProxyErrCode.ATTR_ORDER_CONTROL_CONFLICT_ERROR;
                return false;
            }
            this.isOrderOrProxy = true;
            this.msgProcType = "order";
        }
        // get whether proxy send
        if ("true".equalsIgnoreCase(attrMap.get(AttributeConstants.MESSAGE_PROXY_SEND))) {
            if (!this.needResp) {
                this.needResp = true;
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_PROXY_ACK_INVALID);
                this.errCode = DataProxyErrCode.ATTR_PROXY_CONTROL_CONFLICT_ERROR;
                return false;
            }
            this.isOrderOrProxy = true;
            this.msgProcType = "proxy";
        }
        return true;
    }

    protected Map<String, String> buildEventHeaders(long pkgTime) {
        // build headers
        Map<String, String> headers = new HashMap<>();
        headers.put(AttributeConstants.GROUP_ID, groupId);
        headers.put(AttributeConstants.STREAM_ID, streamId);
        headers.put(ConfigConstants.TOPIC_KEY, topicName);
        headers.put(AttributeConstants.DATA_TIME, String.valueOf(dataTimeMs));
        headers.put(ConfigConstants.REMOTE_IP_KEY, strRemoteIP);
        headers.put(ConfigConstants.MSG_COUNTER_KEY, String.valueOf(msgCount));
        headers.put(ConfigConstants.MSG_ENCODE_VER,
                DataProxyMsgEncType.MSG_ENCODE_TYPE_INLONGMSG.getStrId());
        headers.put(EventConstants.HEADER_KEY_VERSION,
                DataProxyMsgEncType.MSG_ENCODE_TYPE_INLONGMSG.getStrId());
        headers.put(AttributeConstants.RCV_TIME, String.valueOf(msgRcvTime));
        headers.put(AttributeConstants.UNIQ_ID, String.valueOf(uniq));
        headers.put(ConfigConstants.PKG_TIME_KEY, DateTimeUtils.ms2yyyyMMddHHmm(pkgTime));
        // add extra key-value information
        if (!needResp) {
            headers.put(AttributeConstants.MESSAGE_IS_ACK, "false");
        }
        String syncSend = attrMap.get(AttributeConstants.MESSAGE_SYNC_SEND);
        if (StringUtils.isNotEmpty(syncSend)) {
            headers.put(AttributeConstants.MESSAGE_SYNC_SEND, syncSend);
        }
        String proxySend = attrMap.get(AttributeConstants.MESSAGE_PROXY_SEND);
        if (StringUtils.isNotEmpty(proxySend)) {
            headers.put(AttributeConstants.MESSAGE_PROXY_SEND, proxySend);
        }
        if (isOrderOrProxy) {
            headers.put(ConfigConstants.DECODER_ATTRS, this.origAttr);
        }
        String partitionKey = attrMap.get(AttributeConstants.MESSAGE_PARTITION_KEY);
        if (StringUtils.isNotEmpty(partitionKey)) {
            headers.put(AttributeConstants.MESSAGE_PARTITION_KEY, partitionKey);
        }
        if (StringUtils.isNotEmpty(this.msgSeqId)) {
            headers.put(ConfigConstants.SEQUENCE_ID, this.msgSeqId);
        }
        return headers;
    }

}
