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
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.StatConstants;
import org.apache.inlong.dataproxy.source.BaseSource;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.TXT_MSG_BODYLEN_OFFSET;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.TXT_MSG_BODY_OFFSET;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.TXT_MSG_FORMAT_SIZE;
import static org.apache.inlong.dataproxy.source.v0msg.MsgFieldConsts.TXT_MSG_TOTALLEN_SIZE;

public class CodecTextMsg extends AbsV0MsgCodec {

    public CodecTextMsg(int totalDataLen, int msgTypeValue,
            long msgRcvTime, String strRemoteIP) {
        super(totalDataLen, msgTypeValue, msgRcvTime, strRemoteIP);
    }

    public boolean descMsg(BaseSource source, ByteBuf cb) throws Exception {
        // get body length
        int msgHeadPos = cb.readerIndex() - 5;
        int bodyLen = cb.getInt(msgHeadPos + TXT_MSG_BODYLEN_OFFSET);
        if (bodyLen <= 0) {
            if (bodyLen == 0) {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_ZERO);
                this.errCode = DataProxyErrCode.BODY_LENGTH_ZERO;
            } else {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_NEGATIVE);
                this.errCode = DataProxyErrCode.BODY_LENGTH_LESS_ZERO;
            }
            return false;
        }
        if (bodyLen + TXT_MSG_FORMAT_SIZE > totalDataLen + TXT_MSG_TOTALLEN_SIZE) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_TXT_LEN_MALFORMED);
            this.errCode = DataProxyErrCode.BODY_EXCEED_MAX_LEN;
            this.errMsg = String.format("Error msg, bodyLen(%d) + fixedLength(%d) > totalDataLen(%d) + 4",
                    bodyLen, TXT_MSG_FORMAT_SIZE, totalDataLen);
            return false;
        }
        // extract body bytes
        this.bodyData = new byte[bodyLen];
        cb.getBytes(msgHeadPos + TXT_MSG_BODY_OFFSET, this.bodyData, 0, bodyLen);
        if (MsgType.MSG_ORIGINAL_RETURN.equals(MsgType.valueOf(msgType))) {
            this.origBody = new byte[bodyLen];
            System.arraycopy(this.bodyData, 0, this.origBody, 0, bodyLen);
        }
        // get attribute length
        int attrLen = cb.getInt(msgHeadPos + TXT_MSG_BODY_OFFSET + bodyLen);
        if (attrLen < 0) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_ATTR_NEGATIVE);
            this.errCode = DataProxyErrCode.ATTR_LENGTH_LESS_ZERO;
            return false;
        }
        // check attribute length
        if (totalDataLen + TXT_MSG_TOTALLEN_SIZE != TXT_MSG_FORMAT_SIZE + bodyLen + attrLen) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_TXT_LEN_MALFORMED);
            this.errCode = DataProxyErrCode.BODY_EXCEED_MAX_LEN;
            this.errMsg = String.format(
                    "Error msg, totalDataLen(%d) + 4 != fixedLength(%d) + bodyLen(%d) + attrLen(%d)",
                    totalDataLen, TXT_MSG_FORMAT_SIZE, bodyLen, attrLen);
            return false;
        }
        // extract attr bytes
        if (!decAttrInfo(source, cb, attrLen, msgHeadPos + TXT_MSG_FORMAT_SIZE + bodyLen)) {
            return false;
        }
        // decompress body data
        if (StringUtils.isNotBlank(attrMap.get(AttributeConstants.COMPRESS_TYPE))) {
            byte[] unCompressedData;
            try {
                int uncompressedLen = Snappy.uncompressedLength(bodyData, 0, bodyData.length);
                unCompressedData = new byte[uncompressedLen];
                Snappy.uncompress(bodyData, 0, bodyData.length, unCompressedData, 0);
            } catch (IOException e) {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_UNPRESS_EXP);
                this.errCode = DataProxyErrCode.UNCOMPRESS_DATA_ERROR;
                this.errMsg = String.format("Error to uncompress msg, compress type(%s), attr: (%s), error: (%s)",
                        attrMap.get(AttributeConstants.COMPRESS_TYPE), origAttr, e.getCause());
                return false;
            }
            if (unCompressedData.length == 0) {
                source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_UNPRESS_EXP);
                this.errCode = DataProxyErrCode.UNCOMPRESS_DATA_ERROR;
                this.errMsg = String.format("Error to uncompress msg, compress type(%s), attr: (%s), error: 2",
                        attrMap.get(AttributeConstants.COMPRESS_TYPE), origAttr);
                return false;
            }
            this.bodyData = unCompressedData;
        }
        // check body items
        if (MsgType.MSG_MULTI_BODY.equals(MsgType.valueOf(msgType))) {
            int readPos = 0;
            int singleMsgLen;
            ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyData);
            int remaining = bodyBuffer.remaining();
            while (remaining > 0) {
                singleMsgLen = bodyBuffer.getInt(readPos);
                if (singleMsgLen <= 0 || singleMsgLen > remaining) {
                    source.fileMetricIncSumStats(StatConstants.EVENT_MSG_ITEM_LEN_MALFORMED);
                    this.errCode = DataProxyErrCode.BODY_EXCEED_MAX_LEN;
                    this.errMsg = String.format(
                            "Malformed data len, singleMsgLen(%d), buffer remaining(%d), attr: (%s)",
                            singleMsgLen, remaining, origAttr);
                    return false;
                }
                readPos += 4 + singleMsgLen;
                remaining -= 4 + singleMsgLen;
            }
        }
        return true;
    }

    public boolean validAndFillFields(BaseSource source, StringBuilder strBuff) {
        // process topic field
        String tmpGroupId = attrMap.get(AttributeConstants.GROUP_ID);
        String tmpStreamId = attrMap.get(AttributeConstants.STREAM_ID);
        if (StringUtils.isBlank(tmpGroupId)) {
            source.fileMetricIncSumStats(StatConstants.EVENT_MSG_GROUPID_MISSING);
            this.errCode = DataProxyErrCode.MISS_REQUIRED_GROUPID_ARGUMENT;
            return false;
        }
        // get and check topic configure
        String tmpTopicName = ConfigManager.getInstance().getTopicName(tmpGroupId, tmpStreamId);
        if (StringUtils.isBlank(tmpTopicName)) {
            source.fileMetricIncSumStats(StatConstants.EVENT_CONFIG_TOPIC_MISSING);
            this.errCode = DataProxyErrCode.TOPIC_IS_BLANK;
            this.errMsg = String.format(
                    "Topic not configured for groupId=(%s), streamId=(%s)", tmpGroupId, tmpStreamId);
            return false;
        }
        this.groupId = tmpGroupId;
        this.topicName = tmpTopicName;
        if (StringUtils.isNotBlank(tmpStreamId)) {
            this.streamId = tmpStreamId;
        }
        // process message count
        this.msgCount = 1;
        String cntStr = attrMap.get(AttributeConstants.MESSAGE_COUNT);
        if (StringUtils.isBlank(cntStr)) {
            attrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(this.msgCount));
        } else {
            try {
                this.msgCount = Integer.parseInt(cntStr);
            } catch (Throwable e) {
                attrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(this.msgCount));
            }
        }
        // process data-time
        this.dataTimeMs = msgRcvTime;
        String strDataTime = attrMap.get(AttributeConstants.DATA_TIME);
        if (StringUtils.isBlank(strDataTime)) {
            attrMap.put(AttributeConstants.DATA_TIME, String.valueOf(this.dataTimeMs));
        } else {
            try {
                this.dataTimeMs = Long.parseLong(strDataTime);
            } catch (Throwable e) {
                attrMap.put(AttributeConstants.DATA_TIME, String.valueOf(this.dataTimeMs));
            }
        }
        // process sequence id
        String sequenceId = attrMap.get(AttributeConstants.SEQUENCE_ID);
        if (StringUtils.isNotBlank(sequenceId)) {
            strBuff.append(groupId).append(AttributeConstants.SEPARATOR).append(streamId)
                    .append(AttributeConstants.SEPARATOR).append(sequenceId)
                    .append("#").append(strRemoteIP);
            msgSeqId = strBuff.toString();
            strBuff.delete(0, strBuff.length());
        }
        // append required attributes
        if (StringUtils.isBlank(attrMap.get(AttributeConstants.RCV_TIME))) {
            strBuff.append(AttributeConstants.RCV_TIME)
                    .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(msgRcvTime);
            attrMap.put(AttributeConstants.RCV_TIME, String.valueOf(msgRcvTime));
        }
        if (StringUtils.isBlank(attrMap.get(AttributeConstants.MSG_RPT_TIME))) {
            if (strBuff.length() > 0) {
                strBuff.append(AttributeConstants.SEPARATOR);
            }
            strBuff.append(AttributeConstants.MSG_RPT_TIME)
                    .append(AttributeConstants.KEY_VALUE_SEPARATOR).append(msgRcvTime);
            attrMap.put(AttributeConstants.MSG_RPT_TIME, String.valueOf(msgRcvTime));
        }
        // rebuild attribute string
        if (strBuff.length() > 0) {
            if (StringUtils.isNotBlank(origAttr)) {
                strBuff.append(AttributeConstants.SEPARATOR).append(origAttr);
            }
            totalDataLen += strBuff.length() - origAttr.length();
            origAttr = strBuff.toString();
            strBuff.delete(0, strBuff.length());
        }
        return true;
    }

    public Event encEventPackage(BaseSource source, Channel channel) {
        // build InLongMsg object
        int inLongMsgVer = 1;
        if (MsgType.MSG_MULTI_BODY_ATTR.equals(MsgType.valueOf(msgType))) {
            inLongMsgVer = 3;
        }
        InLongMsg inLongMsg = InLongMsg.newInLongMsg(source.isCompressed(), inLongMsgVer);
        if (MsgType.MSG_MULTI_BODY.equals(MsgType.valueOf(msgType))) {
            int calcCnt = 0;
            int singleMsgLen;
            ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyData);
            attrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(1));
            while (bodyBuffer.remaining() > 0) {
                singleMsgLen = bodyBuffer.getInt();
                if (singleMsgLen <= 0 || singleMsgLen > bodyBuffer.remaining()) {
                    break;
                }
                byte[] record = new byte[singleMsgLen];
                bodyBuffer.get(record);
                inLongMsg.addMsg(mapJoiner.join(attrMap), bodyBuffer);
                calcCnt++;
            }
            attrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(calcCnt));
            this.msgCount = calcCnt;
        } else if (MsgType.MSG_MULTI_BODY_ATTR.equals(MsgType.valueOf(msgType))) {
            attrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(1));
            inLongMsg.addMsg(mapJoiner.join(attrMap), bodyData);
            attrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(this.msgCount));
        } else {
            if (!"pb".equals(attrMap.get(AttributeConstants.MESSAGE_TYPE))) {
                if (bodyData[bodyData.length - 1] == '\n') {
                    int tripDataLen = bodyData.length - 1;
                    if (bodyData[bodyData.length - 2] == '\r') {
                        tripDataLen = bodyData.length - 2;
                    }
                    byte[] tripData = new byte[tripDataLen];
                    System.arraycopy(bodyData, 0, tripData, 0, tripDataLen);
                    bodyData = tripData;
                    source.fileMetricIncSumStats(StatConstants.EVENT_MSG_BODY_TRIP);
                }
            }
            inLongMsg.addMsg(mapJoiner.join(attrMap), bodyData);
        }
        byte[] inlongMsgData = inLongMsg.buildArray();
        Event event = EventBuilder.withBody(inlongMsgData, buildEventHeaders(inLongMsg.getCreatetime()));
        inLongMsg.reset();
        return event;
    }
}
