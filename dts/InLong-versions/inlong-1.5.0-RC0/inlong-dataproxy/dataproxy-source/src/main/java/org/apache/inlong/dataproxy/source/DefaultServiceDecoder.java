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

package org.apache.inlong.dataproxy.source;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.msg.MsgType;
import org.apache.inlong.dataproxy.base.ProxyMessage;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.exception.ErrorCode;
import org.apache.inlong.dataproxy.exception.MessageIDException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

public class DefaultServiceDecoder implements ServiceDecoder {

    private static final int BIN_MSG_TOTALLEN_OFFSET = 0;
    private static final int BIN_MSG_TOTALLEN_SIZE = 4;
    private static final int BIN_MSG_MSGTYPE_OFFSET = 4;
    private static final int BIN_MSG_EXTEND_OFFSET = 9;
    private static final int BIN_MSG_EXTEND_SIZE = 2;
    private static final int BIN_MSG_SET_SNAPPY = (1 << 5);
    private static final int BIN_MSG_BODYLEN_SIZE = 4;
    private static final int BIN_MSG_BODYLEN_OFFSET = 21;
    private static final int BIN_MSG_BODY_OFFSET = BIN_MSG_BODYLEN_SIZE + BIN_MSG_BODYLEN_OFFSET;
    private static final int BIN_MSG_ATTRLEN_SIZE = 2;

    private static final int BIN_MSG_FORMAT_SIZE = 29;
    private static final int BIN_MSG_MAGIC_SIZE = 2;
    private static final int BIN_MSG_MAGIC = 0xEE01;

    private static final int BIN_HB_TOTALLEN_SIZE = 4;
    private static final int BIN_HB_BODYLEN_OFFSET = 10;
    private static final int BIN_HB_BODYLEN_SIZE = 4;
    private static final int BIN_HB_BODY_OFFSET = BIN_HB_BODYLEN_SIZE + BIN_HB_BODYLEN_OFFSET;
    private static final int BIN_HB_ATTRLEN_SIZE = 2;
    private static final int BIN_HB_FORMAT_SIZE = 17;

    private static final Logger LOG = LoggerFactory
            .getLogger(DefaultServiceDecoder.class);

    private static final Splitter.MapSplitter mapSplitter = Splitter
            .on(AttributeConstants.SEPARATOR).trimResults()
            .withKeyValueSeparator(AttributeConstants.KEY_VALUE_SEPARATOR);

    /**
     * extract bin heart beat data, message type is 8
     */
    private Map<String, Object> extractNewBinHB(Map<String, Object> resultMap,
            ByteBuf cb, Channel channel, int totalDataLen) throws Exception {
        int msgHeadPos = cb.readerIndex() - 5;

        // check validation
        int bodyLen = cb.getInt(msgHeadPos + BIN_HB_BODYLEN_OFFSET);
        int attrLen = cb.getShort(msgHeadPos + BIN_HB_BODY_OFFSET + bodyLen);
        int msgMagic = cb.getUnsignedShort(msgHeadPos + BIN_HB_BODY_OFFSET
                + bodyLen + BIN_HB_ATTRLEN_SIZE + attrLen);

        if ((totalDataLen + BIN_HB_TOTALLEN_SIZE < (bodyLen
                + attrLen + BIN_HB_FORMAT_SIZE)) || (msgMagic != BIN_MSG_MAGIC)) {

            LOG.error("err msg, bodyLen + attrLen > totalDataLen, "
                    + "and bodyLen={},attrLen={},totalDataLen={},magic={};Connection "
                    + "info:{}",
                    bodyLen, attrLen, totalDataLen, Integer.toHexString(msgMagic), channel.toString());

            return resultMap;
        }
        cb.skipBytes(9 + bodyLen + BIN_HB_ATTRLEN_SIZE);
        // extract common attr
        String strAttr = null;
        if (attrLen != 0) {
            byte[] attrData = new byte[attrLen];
            cb.readBytes(attrData, 0, attrLen);
            strAttr = new String(attrData, StandardCharsets.UTF_8);
            resultMap.put(ConfigConstants.DECODER_ATTRS, strAttr);
        }
        byte version = cb.getByte(msgHeadPos + 9);
        resultMap.put(ConfigConstants.VERSION_TYPE, version);

        return resultMap;
    }

    private void handleDateTime(Map<String, String> commonAttrMap, long uniq,
            long dataTime, int msgCount, String strRemoteIP,
            long msgRcvTime) {
        commonAttrMap.put(AttributeConstants.UNIQ_ID, String.valueOf(uniq));
        String time = String.valueOf(dataTime);
        commonAttrMap.put(AttributeConstants.SEQUENCE_ID,
                new StringBuilder(256).append(strRemoteIP)
                        .append("#").append(time).append("#").append(uniq).toString());
        // dt from sdk
        commonAttrMap.put(AttributeConstants.DATA_TIME, String.valueOf(dataTime));
        commonAttrMap
                .put(AttributeConstants.RCV_TIME, String.valueOf(msgRcvTime));
        commonAttrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(msgCount));
    }

    private boolean handleExtMap(Map<String, String> commonAttrMap, ByteBuf cb,
            Map<String, Object> resultMap, int extendField, int msgHeadPos) {
        boolean index = false;
        if ((extendField & 0x8) == 0x8) {
            index = true;
            // if message is file metric
            int dataLen = cb.getInt(msgHeadPos + BIN_MSG_BODYLEN_OFFSET + 4);
            byte[] data = new byte[dataLen];
            cb.getBytes(msgHeadPos + BIN_MSG_BODY_OFFSET + 4, data, 0,
                    dataLen);
            resultMap.put(ConfigConstants.FILE_BODY, data);
            commonAttrMap.put(ConfigConstants.FILE_CHECK_DATA, "true");
        } else if ((extendField & 0x10) == 0x10) {
            index = true;
            // if message is verification metric message
            int dataLen = cb.getInt(msgHeadPos + BIN_MSG_BODYLEN_OFFSET + 4);
            byte[] data = new byte[dataLen];
            // remove body len
            cb.getBytes(msgHeadPos + BIN_MSG_BODY_OFFSET + 4, data, 0,
                    dataLen);
            resultMap.put(ConfigConstants.FILE_BODY, data);
            commonAttrMap.put(ConfigConstants.MINUTE_CHECK_DATA, "true");
        }
        return index;
    }

    private ByteBuffer handleExtraAppendAttrInfo(Map<String, String> commonAttrMap,
            Channel channel, ByteBuf cb, int extendField,
            int msgHeadPos, int totalDataLen, int attrLen,
            String strAttr, int bodyLen, long msgRcvTime) {
        // get and check report time from report node
        boolean needRebuild = false;
        String rtMs = "";
        if (StringUtils.isBlank(commonAttrMap.get(AttributeConstants.MSG_RPT_TIME))) {
            needRebuild = true;
            rtMs = AttributeConstants.MSG_RPT_TIME
                    + AttributeConstants.KEY_VALUE_SEPARATOR
                    + System.currentTimeMillis();
        }
        // get trace requirement
        String traceInfo = "";
        boolean enableTrace = (((extendField & 0x2) >> 1) == 0x1);
        if (enableTrace) {
            needRebuild = true;
            // get local address
            String strNode2Ip = null;
            SocketAddress loacalSockAddr = channel.localAddress();
            if (null != loacalSockAddr) {
                strNode2Ip = loacalSockAddr.toString();
                try {
                    strNode2Ip = strNode2Ip.substring(1, strNode2Ip.indexOf(':'));
                } catch (Exception ee) {
                    LOG.warn("fail to get the local IP, and strIP={},localSocketAddress={}",
                            strNode2Ip, loacalSockAddr);
                }
            }
            traceInfo = AttributeConstants.DATAPROXY_NODE_IP
                    + AttributeConstants.KEY_VALUE_SEPARATOR + strNode2Ip
                    + AttributeConstants.SEPARATOR
                    + AttributeConstants.DATAPROXY_RCVTIME
                    + AttributeConstants.KEY_VALUE_SEPARATOR + msgRcvTime;
        }
        // rebuild msg attribute
        ByteBuffer dataBuf;
        if (needRebuild) {
            int newTotalLen = totalDataLen;
            // add rtms attribute
            if (StringUtils.isNotEmpty(rtMs)) {
                if (StringUtils.isEmpty(strAttr)) {
                    newTotalLen += rtMs.length();
                    strAttr = rtMs;
                } else {
                    newTotalLen += AttributeConstants.SEPARATOR.length() + rtMs.length();
                    strAttr = strAttr + AttributeConstants.SEPARATOR + rtMs;
                }
            }
            // add trace attribute
            if (StringUtils.isNotEmpty(traceInfo)) {
                if (StringUtils.isEmpty(strAttr)) {
                    newTotalLen += traceInfo.length();
                    strAttr = traceInfo;
                } else {
                    newTotalLen += AttributeConstants.SEPARATOR.length() + traceInfo.length();
                    strAttr = strAttr + AttributeConstants.SEPARATOR + traceInfo;
                }
            }
            // build message buffer
            dataBuf = ByteBuffer.allocate(newTotalLen + BIN_MSG_TOTALLEN_SIZE);
            cb.getBytes(msgHeadPos, dataBuf.array(), 0,
                    bodyLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_ATTRLEN_SIZE
                            - BIN_MSG_MAGIC_SIZE));
            dataBuf.putShort(
                    bodyLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_ATTRLEN_SIZE - BIN_MSG_MAGIC_SIZE),
                    (short) strAttr.length());
            // copy all attributes
            System.arraycopy(strAttr.getBytes(StandardCharsets.UTF_8), 0, dataBuf.array(),
                    bodyLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_MAGIC_SIZE),
                    strAttr.length());
            dataBuf.putInt(0, newTotalLen);
            dataBuf.putShort(newTotalLen + BIN_MSG_TOTALLEN_SIZE - BIN_MSG_MAGIC_SIZE,
                    (short) 0xee01);
        } else {
            dataBuf = ByteBuffer.allocate(totalDataLen + BIN_MSG_TOTALLEN_SIZE);
            cb.getBytes(msgHeadPos, dataBuf.array(), 0, totalDataLen + BIN_MSG_TOTALLEN_SIZE);
        }
        return dataBuf;
    }

    /**
     * extract bin data, message type is 7
     */
    private Map<String, Object> extractNewBinData(Map<String, Object> resultMap,
            ByteBuf cb, Channel channel,
            int totalDataLen, MsgType msgType,
            String strRemoteIP,
            long msgRcvTime) throws Exception {
        int msgHeadPos = cb.readerIndex() - 5;
        // get body length
        int bodyLen = cb.getInt(msgHeadPos + BIN_MSG_BODYLEN_OFFSET);
        if (bodyLen == 0) {
            throw new Exception(
                    "Error msg,  bodyLen is empty; connection info:" + strRemoteIP);
        }
        // get attribute length
        int attrLen = cb.getShort(msgHeadPos + BIN_MSG_BODY_OFFSET + bodyLen);
        // get msg magic
        int msgMagic = cb.getUnsignedShort(msgHeadPos + BIN_MSG_BODY_OFFSET
                + bodyLen + BIN_MSG_ATTRLEN_SIZE + attrLen);
        if ((msgMagic != BIN_MSG_MAGIC)
                || (totalDataLen + BIN_MSG_TOTALLEN_SIZE < (bodyLen + attrLen + BIN_MSG_FORMAT_SIZE))) {
            throw new Exception(
                    "Error msg, bodyLen + attrLen > totalDataLen,or msgMagic is valid! and bodyLen="
                            + bodyLen + ",attrLen=" + attrLen + ",totalDataLen=" + totalDataLen
                            + ";magic=" + Integer.toHexString(msgMagic)
                            + "; connection info:" + strRemoteIP);
        }
        // read data from ByteBuf
        int groupIdNum = cb.readUnsignedShort();
        int streamIdNum = cb.readUnsignedShort();
        final int extendField = cb.readUnsignedShort();
        long dataTime = cb.readUnsignedInt();
        dataTime = dataTime * 1000;
        int msgCount = cb.readUnsignedShort();
        msgCount = (msgCount != 0) ? msgCount : 1;
        long uniq = cb.readUnsignedInt();
        cb.skipBytes(BIN_MSG_BODYLEN_SIZE + bodyLen + BIN_MSG_ATTRLEN_SIZE);
        // read body data
        byte[] bodyData = new byte[bodyLen];
        cb.getBytes(msgHeadPos + BIN_MSG_BODY_OFFSET, bodyData, 0, bodyLen);
        // read attr and write to map.
        String strAttr = null;
        Map<String, String> commonAttrMap = new HashMap<>();
        if (attrLen != 0) {
            byte[] attrData = new byte[attrLen];
            cb.readBytes(attrData, 0, attrLen);
            strAttr = new String(attrData, StandardCharsets.UTF_8);
            resultMap.put(ConfigConstants.DECODER_ATTRS, strAttr);
            try {
                commonAttrMap.putAll(mapSplitter.split(strAttr));
            } catch (Exception e) {
                cb.clear();
                throw new MessageIDException(uniq,
                        ErrorCode.ATTR_ERROR,
                        new Throwable("[Parse Error]new six segment protocol ,attr is "
                                + strAttr + " , channel info:" + strRemoteIP));
            }
        }
        // build attributes
        resultMap.put(ConfigConstants.COMMON_ATTR_MAP, commonAttrMap);
        resultMap.put(ConfigConstants.EXTRA_ATTR, ((extendField & 0x1) == 0x1) ? "true" : "false");
        resultMap.put(ConfigConstants.DECODER_BODY, bodyData);
        try {
            // handle common attribute information
            handleDateTime(commonAttrMap, uniq, dataTime, msgCount, strRemoteIP, msgRcvTime);
            final boolean isIndexMsg =
                    handleExtMap(commonAttrMap, cb, resultMap, extendField, msgHeadPos);
            ByteBuffer dataBuf = handleExtraAppendAttrInfo(commonAttrMap, channel, cb,
                    extendField, msgHeadPos, totalDataLen, attrLen, strAttr, bodyLen, msgRcvTime);
            // Check if groupId and streamId are number-to-name
            String groupId = commonAttrMap.get(AttributeConstants.GROUP_ID);
            String streamId = commonAttrMap.get(AttributeConstants.STREAM_ID);
            if ((groupId != null) && (streamId != null)) {
                commonAttrMap.put(AttrConstants.NUM2NAME, "FALSE");
                dataBuf.putShort(BIN_MSG_EXTEND_OFFSET, (short) (extendField | 0x4));
            } else {
                boolean hasNumGroupId = (((extendField & 0x4) >> 2) == 0x0);
                if (hasNumGroupId && (0 != groupIdNum) && (0 != streamIdNum)) {
                    commonAttrMap.put(AttrConstants.NUM2NAME, "TRUE");
                    commonAttrMap.put(AttrConstants.GROUPID_NUM, String.valueOf(groupIdNum));
                    commonAttrMap.put(AttrConstants.STREAMID_NUM, String.valueOf(streamIdNum));
                }
            }
            // build ProxyMessage
            if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                List<ProxyMessage> msgList = new ArrayList<>(1);
                if (isIndexMsg) {
                    msgList.add(new ProxyMessage(groupId, streamId, commonAttrMap,
                            (byte[]) resultMap.get(ConfigConstants.FILE_BODY)));
                } else {
                    msgList.add(new ProxyMessage(groupId,
                            streamId, commonAttrMap, dataBuf.array()));
                }
                resultMap.put(ConfigConstants.MSG_LIST, msgList);
            }
        } catch (Exception ex) {
            LOG.error("extractNewBinData has error: ", ex);
            cb.clear();
            throw new MessageIDException(uniq, ErrorCode.OTHER_ERROR, ex.getCause());
        }
        return resultMap;
    }

    /**
     * extract bin data, message type less than 7
     */
    private Map<String, Object> extractDefaultData(Map<String, Object> resultMap,
            ByteBuf cb, int totalDataLen,
            MsgType msgType, String strRemoteIP,
            long msgRcvTime) throws Exception {
        int bodyLen = cb.readInt();
        if (bodyLen == 0) {
            throw new Exception("Error msg: bodyLen is empty, connection info:" + strRemoteIP);
        }
        // if body len is bigger than totalDataLen - 5(bodyLen bytes + message type bytes),
        // that means an invalid message, reject it.
        if (bodyLen > totalDataLen - 5) {
            throw new Exception("Error msg, firstLen > totalDataLen, and bodyLen="
                    + bodyLen + ",totalDataLen=" + totalDataLen
                    + ", connection info:" + strRemoteIP);
        }
        // extract body bytes
        byte[] bodyData = new byte[bodyLen];
        cb.readBytes(bodyData, 0, bodyLen);
        resultMap.put(ConfigConstants.DECODER_BODY, bodyData);
        // extract attribute
        int attrLen = cb.readInt();
        // 9 means bodyLen bytes(4) + message type bytes(1) + attrLen bytes(4)
        if (totalDataLen != 9 + attrLen + bodyLen) {
            throw new Exception(
                    "Error msg, totalDataLen != 9 + bodyLen + attrLen,and bodyLen=" + bodyLen
                            + ",totalDataLen=" + totalDataLen + ",attrDataLen=" + attrLen
                            + ", connection info:" + strRemoteIP);
        }
        // extract attr bytes
        byte[] attrData = new byte[attrLen];
        cb.readBytes(attrData, 0, attrLen);
        // convert attr bytes to map
        Map<String, String> commonAttrMap;
        String strAttr = new String(attrData, StandardCharsets.UTF_8);
        try {
            commonAttrMap = new HashMap<>(mapSplitter.split(strAttr));
        } catch (Exception e) {
            throw new Exception("Parse commonAttrMap error.commonAttrString is: "
                    + strAttr + " , connection info:" + strRemoteIP);
        }
        resultMap.put(ConfigConstants.DECODER_ATTRS, strAttr);
        resultMap.put(ConfigConstants.COMMON_ATTR_MAP, commonAttrMap);
        // decompress body data if compress type exists.
        String compressType = commonAttrMap.get(AttributeConstants.COMPRESS_TYPE);
        if (StringUtils.isNotBlank(compressType)) {
            resultMap.put(ConfigConstants.COMPRESS_TYPE, compressType);
            byte[] unCompressedData = processUnCompress(bodyData, compressType);
            if (unCompressedData == null || unCompressedData.length == 0) {
                throw new Exception("Uncompressed data error! compress type:"
                        + compressType + ";attr:" + strAttr
                        + " , connection info:" + strRemoteIP);
            }
            bodyData = unCompressedData;
        }
        // fill up attr map with some keys.
        String groupId = commonAttrMap.get(AttributeConstants.GROUP_ID);
        String streamId = commonAttrMap.get(AttributeConstants.STREAM_ID);
        String strDataTime = commonAttrMap.get(AttributeConstants.DATA_TIME);
        long longDataTime = NumberUtils.toLong(strDataTime, msgRcvTime);
        commonAttrMap.put(AttributeConstants.DATA_TIME, String.valueOf(longDataTime));
        // add message report time field
        if (StringUtils.isBlank(commonAttrMap.get(AttributeConstants.MSG_RPT_TIME))) {
            commonAttrMap.put(AttributeConstants.MSG_RPT_TIME,
                    String.valueOf(msgRcvTime));
        }
        commonAttrMap.put(AttributeConstants.RCV_TIME, String.valueOf(msgRcvTime));
        // check message count attr
        String strMsgCnt = commonAttrMap.get(AttributeConstants.MESSAGE_COUNT);
        int intMsgCnt = NumberUtils.toInt(strMsgCnt, 1);
        commonAttrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(intMsgCnt));
        // extract data from bodyData and if message type is 5, convert data into list.
        int calCnt = 0;
        List<ProxyMessage> msgList = null;
        ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyData);
        if (MsgType.MSG_MULTI_BODY.equals(msgType)) {
            msgList = new ArrayList<>(intMsgCnt);
            while (bodyBuffer.remaining() > 0) {
                int singleMsgLen = bodyBuffer.getInt();
                if (singleMsgLen <= 0 || singleMsgLen > bodyBuffer.remaining()) {
                    throw new Exception(
                            "[Malformed Data]Invalid data len! channel is " + strRemoteIP);
                }
                byte[] record = new byte[singleMsgLen];
                bodyBuffer.get(record);
                ProxyMessage message = new ProxyMessage(groupId, streamId, commonAttrMap, record);
                msgList.add(message);
                calCnt++;
            }
        } else {
            msgList = new ArrayList<>(1);
            msgList.add(new ProxyMessage(groupId, streamId, commonAttrMap, bodyData));
            calCnt++;
        }
        if (calCnt != intMsgCnt) {
            commonAttrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(calCnt));
        }
        resultMap.put(ConfigConstants.MSG_LIST, msgList);
        return resultMap;
    }

    private byte[] processUnCompress(byte[] input, String compressType) {
        byte[] result;
        try {
            int uncompressedLen = Snappy.uncompressedLength(input, 0, input.length);
            result = new byte[uncompressedLen];
            Snappy.uncompress(input, 0, input.length, result, 0);
        } catch (IOException e) {
            LOG.error("Uncompressed data error: ", e);
            return null;
        }
        return result;
    }

    /**
     * BEFORE                                                                   AFTER
     * +--------+--------+--------+----------------+--------+----------------+------------------------+
     * | Length | Msgtype| Length | Actual Content1| Length | Actual Content2|----- >| Actual
     * Content1| Time    postfix              | | 0x000C | 0x02   | 0x000C | "HELLO, WORLD" | 0x000C
     * | "view video? " |       | "HELLO, WORLD" | ",recvtimestamp:12345674321" |
     * +--------+--------+--------+----------------+--------+----------------+------------------------+
     */
    @Override
    public Map<String, Object> extractData(ByteBuf cb, String strRemoteIP,
            long msgRcvTime, Channel channel) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        if (null == cb) {
            LOG.error("cb == null");
            return resultMap;
        }
        int totalLen = cb.readableBytes();
        if (ConfigConstants.MSG_MAX_LENGTH_BYTES < totalLen) {
            throw new Exception("Error msg, ConfigConstants.MSG_MAX_LENGTH_BYTES "
                    + "< totalLen, and  totalLen=" + totalLen);
        }
        // save index, reset it if buffer is not satisfied.
        cb.markReaderIndex();
        int totalDataLen = cb.readInt();
        if (totalDataLen + HEAD_LENGTH <= totalLen) {
            int msgTypeInt = cb.readByte();
            int compressType = ((msgTypeInt & 0xE0) >> 5);
            MsgType msgType = MsgType.valueOf(msgTypeInt);
            resultMap.put(ConfigConstants.MSG_TYPE, msgType);

            // if it's heart beat or unknown message, just return without handling it.
            if (MsgType.MSG_HEARTBEAT.equals(msgType)
                    || MsgType.MSG_UNKNOWN.equals(msgType)) {
                return resultMap;
            }
            // if it's bin heart beat.
            if (MsgType.MSG_BIN_HEARTBEAT.equals(msgType)) {
                return extractNewBinHB(resultMap, cb, channel, totalDataLen);
            }
            // process data message
            if (msgType.getValue() >= MsgType.MSG_BIN_MULTI_BODY.getValue()) {
                resultMap.put(ConfigConstants.COMPRESS_TYPE, (compressType != 0) ? "snappy" : "");
                return extractNewBinData(resultMap, cb,
                        channel, totalDataLen, msgType,
                        strRemoteIP, msgRcvTime);
            } else {
                return extractDefaultData(resultMap, cb,
                        totalDataLen, msgType, strRemoteIP, msgRcvTime);
            }
        } else {
            // reset index.
            cb.resetReaderIndex();
            return null;
        }
    }
}
