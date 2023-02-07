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

package org.apache.inlong.sdk.dataproxy.codec;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.apache.inlong.sdk.dataproxy.config.EncryptConfigEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EncodeObject {

    private static final Splitter.MapSplitter MAP_SPLITTER = Splitter.on(AttributeConstants.SEPARATOR).trimResults()
            .withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);

    private byte[] bodyBytes;
    private String attributes;
    private String messageId;
    private int msgtype;
    private List<byte[]> bodylist;
    private String commonattr = "";
    private String messageKey = "data";
    private String proxyIp = "";
    // private long seqId
    private long dt;
    // package time
    private long packageTime = System.currentTimeMillis();
    private int cnt = -1;
    private boolean isReport = false;
    private boolean isGroupIdTransfer = false;
    private boolean isSupportLF = false;
    private boolean isAuth = false;
    private boolean isEncrypt = false;
    private boolean isCompress = true;
    private int groupIdNum;
    private int streamIdNum;
    private String groupId;
    private String streamId;
    private short load;
    private String userName = "";
    private String secretKey = "";
    private String msgUUID = null;
    private EncryptConfigEntry encryptEntry = null;

    private SendResult sendResult = SendResult.OK;
    private String errMsg;
    private String dpIp;

    /* Used by de_serialization. msgtype=7/8 */
    public EncodeObject(String attributes) {
        handleAttr(attributes);
    }

    /* Used by de_serialization. */
    public EncodeObject(byte[] bodyBytes, String attributes) {
        this.bodyBytes = bodyBytes;
        this.attributes = attributes;
        handleAttr(attributes);
    }

    /* Used by serialization.But never used */
    // old version:we need add message id by attr
    public EncodeObject(byte[] bodyBytes, String attributes, String messageId) {
        this.bodyBytes = bodyBytes;
        this.messageId = messageId;
        this.attributes = attributes + "&messageId=" + messageId;
        addRTMS(MsgType.MSG_COMMON_SERVICE.getValue());
    }

    // used for bytes initializtion,msgtype=3/5
    public EncodeObject(byte[] bodyBytes, String attributes, String messageId,
            int msgtype, boolean isCompress, final String groupId) {
        this.bodyBytes = bodyBytes;
        this.messageId = messageId;
        this.attributes = attributes + "&messageId=" + messageId;
        this.msgtype = msgtype;
        this.groupId = groupId;
        this.isCompress = isCompress;
        addRTMS(msgtype);
    }

    // used for bodylist initializtion,msgtype=3/5
    public EncodeObject(List<byte[]> bodyList, String attributes, String messageId,
            int msgtype, boolean isCompress, final String groupId) {
        this.bodylist = bodyList;
        this.messageId = messageId;
        this.attributes = attributes + "&messageId=" + messageId;
        this.msgtype = msgtype;
        this.groupId = groupId;
        this.isCompress = isCompress;
        addRTMS(msgtype);
    }

    // used for bytes initializtion,msgtype=7/8
    public EncodeObject(byte[] bodyBytes, int msgtype, boolean isCompress, boolean isReport,
            boolean isGroupIdTransfer, long dt, long seqId, String groupId,
            String streamId, String commonattr) {
        this.bodyBytes = bodyBytes;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isGroupIdTransfer = isGroupIdTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.groupId = groupId;
        this.streamId = streamId;
        addRTMS(msgtype);
    }

    // used for bodylist initializtion,msgtype=7/8
    public EncodeObject(List<byte[]> bodyList, int msgtype, boolean isCompress,
            boolean isReport, boolean isGroupIdTransfer, long dt,
            long seqId, String groupId, String streamId, String commonattr) {
        this.bodylist = bodyList;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isGroupIdTransfer = isGroupIdTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.groupId = groupId;
        this.streamId = streamId;
        addRTMS(msgtype);
    }

    // file agent, used for bytes initializtion,msgtype=7/8
    public EncodeObject(byte[] bodyBytes, int msgtype, boolean isCompress,
            boolean isReport, boolean isGroupIdTransfer, long dt,
            long seqId, String groupId, String streamId, String commonattr,
            String messageKey, String proxyIp) {
        this.bodyBytes = bodyBytes;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isGroupIdTransfer = isGroupIdTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.groupId = groupId;
        this.streamId = streamId;
        this.messageKey = messageKey;
        this.proxyIp = proxyIp;
        addRTMS(msgtype);
    }

    // file agent, used for bodylist initializtion,msgtype=7/8
    public EncodeObject(List<byte[]> bodyList, int msgtype, boolean isCompress,
            boolean isReport, boolean isGroupIdTransfer, long dt,
            long seqId, String groupId, String streamId, String commonattr,
            String messageKey, String proxyIp) {
        this.bodylist = bodyList;
        this.msgtype = msgtype;
        this.isCompress = isCompress;
        this.isReport = isReport;
        this.dt = dt;
        this.isGroupIdTransfer = isGroupIdTransfer;
        this.commonattr = commonattr;
        this.messageId = String.valueOf(seqId);
        this.groupId = groupId;
        this.streamId = streamId;
        this.messageKey = messageKey;
        this.proxyIp = proxyIp;
        addRTMS(msgtype);
    }

    private void handleAttr(String attributes) {
        if (StringUtils.isBlank(attributes)) {
            return;
        }
        Map<String, String> backAttrs = new HashMap<>(MAP_SPLITTER.split(attributes));
        if (backAttrs.containsKey(AttributeConstants.MESSAGE_ID)) {
            this.messageId = backAttrs.get(AttributeConstants.MESSAGE_ID);
        }
        dpIp = backAttrs.get(AttributeConstants.MESSAGE_DP_IP);

        String errCode = backAttrs.get(AttributeConstants.MESSAGE_PROCESS_ERRCODE);
        // errCode is empty or equals 0 -> success
        if (StringUtils.isBlank(errCode) || Integer.parseInt(errCode) == 0) {
            this.sendResult = SendResult.OK;
        } else {
            // get errMsg
            this.errMsg = backAttrs.get(AttributeConstants.MESSAGE_PROCESS_ERRMSG);
            if (StringUtils.isBlank(errMsg)) {
                this.errMsg = DataProxyErrCode.valueOf(Integer.parseInt(errCode)).getErrMsg();
            }
            // sendResult
            this.sendResult = convertToSendResult(Integer.parseInt(errCode));
        }
    }

    private void addRTMS(int msgtype) {
        if (msgtype == MsgType.MSG_BIN_MULTI_BODY.getValue() || msgtype == MsgType.MSG_BIN_HEARTBEAT.getValue()) {
            if (StringUtils.isBlank(commonattr)) {
                this.commonattr = AttributeConstants.MSG_RPT_TIME + "=" + System.currentTimeMillis();
            } else {
                this.commonattr += "&" + AttributeConstants.MSG_RPT_TIME + "=" + System.currentTimeMillis();
            }
        } else {
            if (StringUtils.isBlank(attributes)) {
                this.attributes = AttributeConstants.MSG_RPT_TIME + "=" + System.currentTimeMillis();
            } else {
                this.attributes += "&" + AttributeConstants.MSG_RPT_TIME + "=" + System.currentTimeMillis();
            }
        }
    }

    private SendResult convertToSendResult(int errCode) {
        DataProxyErrCode dpErrCode = DataProxyErrCode.valueOf(errCode);
        switch (dpErrCode) {
            case SINK_SERVICE_UNREADY:
                return SendResult.SINK_SERVICE_UNREADY;
            case MISS_REQUIRED_GROUPID_ARGUMENT:
            case MISS_REQUIRED_STREAMID_ARGUMENT:
            case MISS_REQUIRED_DT_ARGUMENT:
            case UNSUPPORTED_EXTEND_FIELD_VALUE:
                return SendResult.INVALID_ATTRIBUTES;
            case MISS_REQUIRED_BODY_ARGUMENT:
            case EMPTY_MSG:
                return SendResult.INVALID_DATA;
            case BODY_EXCEED_MAX_LEN:
                return SendResult.BODY_EXCEED_MAX_LEN;
            case UNCONFIGURED_GROUPID_OR_STREAMID:
                return SendResult.UNCONFIGURED_GROUPID_OR_STREAMID;
            case PUT_EVENT_TO_CHANNEL_FAILURE:
            case NO_AVAILABLE_PRODUCER:
            case PRODUCER_IS_NULL:
            case SEND_REQUEST_TO_MQ_FAILURE:
            case MQ_RETURN_ERROR:
            case DUPLICATED_MESSAGE:
                return SendResult.DATAPROXY_FAIL_TO_RECEIVE;
            default:
                return SendResult.UNKOWN_ERROR;
        }
    }

    public String getMsgUUID() {
        return msgUUID;
    }

    public void setMsgUUID(String msgUUID) {
        this.msgUUID = msgUUID;
    }

    public boolean isGroupIdTransfer() {
        return isGroupIdTransfer;
    }

    public void setGroupIdTransfer(boolean isGroupIdTransfer) {
        this.isGroupIdTransfer = isGroupIdTransfer;
    }

    public short getLoad() {
        return load;
    }

    public void setLoad(short load) {
        this.load = load;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public boolean isReport() {
        return isReport;
    }

    public void setReport(boolean isReport) {
        this.isReport = isReport;
    }

    public boolean isAuth() {
        return isAuth;
    }

    public void setAuth(boolean auth, final String userName, final String secretKey) {
        this.isAuth = auth;
        this.userName = userName;
        this.secretKey = secretKey;
    }

    public String getUserName() {
        return userName;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean isEncrypt() {
        return isEncrypt;
    }

    public EncryptConfigEntry getEncryptEntry() {
        return encryptEntry;
    }

    public void setEncryptEntry(boolean isEncrypt, String userName, EncryptConfigEntry encryptEntry) {
        this.isEncrypt = isEncrypt;
        if (userName != null) {
            this.userName = userName;
        }
        this.encryptEntry = encryptEntry;
    }

    public int getGroupIdNum() {
        return groupIdNum;
    }

    public void setGroupIdNum(int groupIdNum) {
        this.groupIdNum = groupIdNum;
    }

    public int getStreamIdNum() {
        return streamIdNum;
    }

    public void setStreamIdNum(int streamIdNum) {
        this.streamIdNum = streamIdNum;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getPackageTime() {
        return packageTime;
    }

    public void setPackageTime(long packageTime) {
        this.packageTime = packageTime;
    }

    public String getCommonattr() {
        return commonattr;
    }

    public void setCommonattr(String commonattr) {
        this.commonattr = commonattr;
    }

    public boolean isCompress() {
        return isCompress;
    }

    public List<byte[]> getBodylist() {
        return bodylist;
    }

    public int getMsgtype() {
        return msgtype;
    }

    public void setMsgtype(int msgtype) {
        this.msgtype = msgtype;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public void setBodyBytes(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
    }

    public String getAttributes() {
        return attributes;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getProxyIp() {
        return proxyIp;
    }

    public void setProxyIp(String proxyIp) {
        this.proxyIp = proxyIp;
    }

    public boolean isSupportLF() {
        return isSupportLF;
    }

    public void setSupportLF(boolean supportLF) {
        isSupportLF = supportLF;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public int getRealCnt() {
        if (bodylist != null) {
            return bodylist.size();
        }
        return 1;
    }

    public String getDpIp() {
        return dpIp;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public SendResult getSendResult() {
        return sendResult;
    }
}
