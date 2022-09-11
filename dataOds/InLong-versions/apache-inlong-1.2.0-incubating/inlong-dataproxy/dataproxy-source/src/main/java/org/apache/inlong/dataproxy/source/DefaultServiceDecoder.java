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
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.apache.commons.lang.StringUtils;
import org.apache.inlong.dataproxy.base.ProxyMessage;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.apache.inlong.dataproxy.exception.ErrorCode;
import org.apache.inlong.dataproxy.exception.MessageIDException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private void handleDateTime(Map<String, String> commonAttrMap, Channel channel,
            long uniq, long dataTime, int msgCount) {
        commonAttrMap.put(AttributeConstants.UNIQ_ID, String.valueOf(uniq));
        String time = "";
        if (commonAttrMap.containsKey(ConfigConstants.PKG_TIME_KEY)) {
            time = commonAttrMap
                    .get(ConfigConstants.PKG_TIME_KEY);
        } else {
            time = String.valueOf(dataTime);
        }
        StringBuilder sidBuilder = new StringBuilder();
        /*
         * udp need use msgEvent get remote address
         */
        String remoteAddress = "";
        if (channel != null && channel.remoteAddress() != null) {
            remoteAddress = channel.remoteAddress().toString();
        }
        sidBuilder.append(remoteAddress).append("#").append(time)
                .append("#").append(uniq);
        commonAttrMap.put(AttributeConstants.SEQUENCE_ID, new String(sidBuilder));

        // datetime from sdk
        commonAttrMap.put(AttributeConstants.DATA_TIME, String.valueOf(dataTime));
        commonAttrMap
                .put(AttributeConstants.RCV_TIME, String.valueOf(System.currentTimeMillis()));
        commonAttrMap.put(AttributeConstants.MESSAGE_COUNT,
                String.valueOf(msgCount != 0 ? msgCount : 1));
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

    private ByteBuffer handleTrace(Channel channel, ByteBuf cb, int extendField,
            int msgHeadPos, int totalDataLen, int attrLen, String strAttr, int bodyLen) {
        // whether enable trace
        boolean enableTrace = (((extendField & 0x2) >> 1) == 0x1);
        ByteBuffer dataBuf;
        if (!enableTrace) {
            dataBuf = ByteBuffer.allocate(totalDataLen + BIN_MSG_TOTALLEN_SIZE);
            cb.getBytes(msgHeadPos, dataBuf.array(), 0,
                    totalDataLen + BIN_MSG_TOTALLEN_SIZE);
        } else {
            String traceInfo;
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

            traceInfo = "node2ip=" + strNode2Ip + "&rtime2=" + System.currentTimeMillis();

            int newTotalLen = 0;

            if (attrLen != 0) {
                newTotalLen = totalDataLen + traceInfo.length() + "&".length();
                strAttr = strAttr + "&" + traceInfo;
            } else {
                newTotalLen = totalDataLen + traceInfo.length();
                strAttr = traceInfo;
            }

            dataBuf = ByteBuffer.allocate(newTotalLen + BIN_MSG_TOTALLEN_SIZE);
            cb.getBytes(msgHeadPos, dataBuf.array(), 0,
                    bodyLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_ATTRLEN_SIZE
                            - BIN_MSG_MAGIC_SIZE));
            dataBuf.putShort(
                    bodyLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_ATTRLEN_SIZE - BIN_MSG_MAGIC_SIZE),
                    (short) strAttr.length());

            System.arraycopy(strAttr.getBytes(StandardCharsets.UTF_8), 0, dataBuf.array(),
                    bodyLen + (BIN_MSG_FORMAT_SIZE - BIN_MSG_MAGIC_SIZE),
                    strAttr.length());

            dataBuf.putInt(0, newTotalLen);
            dataBuf.putShort(newTotalLen + BIN_MSG_TOTALLEN_SIZE - BIN_MSG_MAGIC_SIZE,
                    (short) 0xee01);
        }
        return dataBuf;
    }

    /**
     * extract bin data, message type is 7
     */
    private Map<String, Object> extractNewBinData(Map<String, Object> resultMap,
            ByteBuf cb, Channel channel,
            int totalDataLen, MsgType msgType) throws Exception {
        int msgHeadPos = cb.readerIndex() - 5;

        int bodyLen = cb.getInt(msgHeadPos + BIN_MSG_BODYLEN_OFFSET);
        int attrLen = cb.getShort(msgHeadPos + BIN_MSG_BODY_OFFSET + bodyLen);
        int msgMagic = cb.getUnsignedShort(msgHeadPos + BIN_MSG_BODY_OFFSET
                + bodyLen + BIN_MSG_ATTRLEN_SIZE + attrLen);

        if (bodyLen == 0) {
            throw new Exception(new Throwable("err msg,  bodyLen is empty"
                    + ";Connection info:" + channel.toString()));
        }

        if ((totalDataLen + BIN_MSG_TOTALLEN_SIZE < (bodyLen + attrLen + BIN_MSG_FORMAT_SIZE))
                || (msgMagic != BIN_MSG_MAGIC)) {
            throw new Exception(new Throwable(
                    "err msg, bodyLen + attrLen > totalDataLen,or msgMagic is valid! and bodyLen="
                            + bodyLen + ",totalDataLen=" + totalDataLen + ",attrLen=" + attrLen
                            + ";magic=" + Integer.toHexString(msgMagic)
                            + ";Connection info:" + channel.toString()));
        }

        int groupIdNum = cb.readUnsignedShort();
        int streamIdNum = cb.readUnsignedShort();
        final int extendField = cb.readUnsignedShort();
        long dataTime = cb.readUnsignedInt();
        int msgCount = cb.readUnsignedShort();
        long uniq = cb.readUnsignedInt();

        dataTime = dataTime * 1000;
        Map<String, String> commonAttrMap = new HashMap<String, String>();
        cb.skipBytes(BIN_MSG_BODYLEN_SIZE + bodyLen + BIN_MSG_ATTRLEN_SIZE);
        resultMap.put(ConfigConstants.COMMON_ATTR_MAP, commonAttrMap);

        resultMap.put(ConfigConstants.EXTRA_ATTR, ((extendField & 0x1) == 0x1) ? "true" : "false");

        // read body data
        byte[] bodyData = new byte[bodyLen];
        cb.getBytes(msgHeadPos + BIN_MSG_BODY_OFFSET, bodyData, 0, bodyLen);
        resultMap.put(ConfigConstants.DECODER_BODY, bodyData);

        // read attr and write to map.
        String strAttr = null;
        if (attrLen != 0) {
            byte[] attrData = new byte[attrLen];
            cb.readBytes(attrData, 0, attrLen);
            strAttr = new String(attrData, StandardCharsets.UTF_8);
            LOG.debug("strAttr = {}, length = {}", strAttr, strAttr.length());
            resultMap.put(ConfigConstants.DECODER_ATTRS, strAttr);

            try {
                commonAttrMap.putAll(mapSplitter.split(strAttr));
            } catch (Exception e) {
                cb.clear();
                throw new MessageIDException(uniq,
                        ErrorCode.ATTR_ERROR,
                        new Throwable("[Parse Error]new six segment protocol ,attr is "
                                + strAttr + " , channel info:" + channel.toString()));
            }
        }

        try {
            handleDateTime(commonAttrMap, channel, uniq, dataTime, msgCount);
            final boolean index = handleExtMap(commonAttrMap, cb, resultMap, extendField, msgHeadPos);
            ByteBuffer dataBuf = handleTrace(channel, cb, extendField, msgHeadPos,
                    totalDataLen, attrLen, strAttr, bodyLen);

            String groupId = null;
            String streamId = null;

            if (commonAttrMap.containsKey(AttributeConstants.GROUP_ID)) {
                groupId = commonAttrMap.get(AttributeConstants.GROUP_ID);
            }
            if (commonAttrMap.containsKey(AttributeConstants.STREAM_ID)) {
                streamId = commonAttrMap.get(AttributeConstants.STREAM_ID);
            }

            if ((groupId != null) && (streamId != null)) {
                commonAttrMap.put(AttributeConstants.NUM2NAME, "FALSE");
                dataBuf.putShort(BIN_MSG_EXTEND_OFFSET, (short) (extendField | 0x4));
            } else {
                boolean hasNumGroupId = (((extendField & 0x4) >> 2) == 0x0);
                if (hasNumGroupId && (0 != groupIdNum) && (0 != streamIdNum)) {
                    commonAttrMap.put(AttributeConstants.NUM2NAME, "TRUE");
                    commonAttrMap.put(AttributeConstants.GROUPID_NUM, String.valueOf(groupIdNum));
                    commonAttrMap.put(AttributeConstants.STREAMID_NUM, String.valueOf(streamIdNum));
                }
            }

            if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType) && !index) {
                List<ProxyMessage> msgList = new ArrayList<>(1);
                msgList.add(new ProxyMessage(groupId, streamId, commonAttrMap, dataBuf.array()));
                resultMap.put(ConfigConstants.MSG_LIST, msgList);
            } else if (MsgType.MSG_BIN_MULTI_BODY.equals(msgType)) {
                List<ProxyMessage> msgList = new ArrayList<>(1);
                msgList.add(new ProxyMessage(groupId, streamId, commonAttrMap,
                        (byte[]) resultMap.get(ConfigConstants.FILE_BODY)));
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
            ByteBuf cb, Channel channel,
            int totalDataLen, MsgType msgType) throws Exception {
        int bodyLen = cb.readInt();
        if (bodyLen == 0) {
            throw new Exception(new Throwable("err msg,  bodyLen is empty" + ";"
                    + "Connection info:" + channel.toString()));
        }
        // if body len is bigger than totalDataLen - 5(bodyLen bytes + message type bytes),
        // that means an invalid message, reject it.
        if (bodyLen > totalDataLen - 5) {
            throw new Exception(new Throwable("err msg, firstLen > totalDataLen, and bodyLen="
                    + bodyLen + ",totalDataLen=" + totalDataLen
                    + ";Connection info:" + channel.toString()));
        }

        // extract body bytes
        byte[] bodyData = new byte[bodyLen];
        cb.readBytes(bodyData, 0, bodyLen);
        resultMap.put(ConfigConstants.DECODER_BODY, bodyData);

        int attrLen = cb.readInt();
        // 9 means bodyLen bytes(4) + message type bytes(1) + attrLen bytes(4)
        if (totalDataLen != 9 + attrLen + bodyLen) {
            throw new Exception(new Throwable(
                    "err msg, totalDataLen != 9 + bodyLen + attrLen,and bodyLen=" + bodyLen
                            + ",totalDataLen=" + totalDataLen + ",attrDataLen=" + attrLen
                            + ";Connection info:" + channel.toString()));
        }

        // extract attr bytes
        byte[] attrData = new byte[attrLen];
        cb.readBytes(attrData, 0, attrLen);
        String strAttr = new String(attrData, StandardCharsets.UTF_8);
        resultMap.put(ConfigConstants.DECODER_ATTRS, strAttr);

        // convert attr bytes to map
        Map<String, String> commonAttrMap;
        try {
            commonAttrMap = new HashMap<>(mapSplitter.split(strAttr));
        } catch (Exception e) {
            throw new Exception(new Throwable("Parse commonAttrMap error.commonAttrString is: "
                    + strAttr + " ,channel is :" + channel.toString()));
        }
        resultMap.put(ConfigConstants.COMMON_ATTR_MAP, commonAttrMap);

        // decompress body data if compress type exists.
        String compressType = commonAttrMap.get(AttributeConstants.COMPRESS_TYPE);
        resultMap.put(ConfigConstants.COMPRESS_TYPE, compressType);
        if (StringUtils.isNotBlank(compressType)) {
            byte[] unCompressedData = processUnCompress(bodyData, compressType);
            if (unCompressedData == null || unCompressedData.length == 0) {
                throw new Exception(new Throwable("Uncompressed data error!compress type:"
                        + compressType + ";data:" + new String(bodyData, StandardCharsets.UTF_8)
                        + ";attr:" + strAttr + ";channel:" + channel.toString()));
            }
            bodyData = unCompressedData;
        }

        // fill up attr map with some keys.
        commonAttrMap.put(AttributeConstants.RCV_TIME, String.valueOf(System.currentTimeMillis()));
        String groupId = commonAttrMap.get(AttributeConstants.GROUP_ID);
        String streamId = commonAttrMap.get(AttributeConstants.STREAM_ID);

        // add message count attr
        String cntStr = commonAttrMap.get(AttributeConstants.MESSAGE_COUNT);
        int msgCnt = cntStr != null ? Integer.parseInt(cntStr) : 1;
        commonAttrMap.put(AttributeConstants.MESSAGE_COUNT, String.valueOf(msgCnt));

        // extract data from bodyData and if message type is 5, convert data into list.
        List<ProxyMessage> msgList = null;
        ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyData);
        if (MsgType.MSG_MULTI_BODY.equals(msgType)) {
            msgList = new ArrayList<>(msgCnt);
            while (bodyBuffer.remaining() > 0) {
                int singleMsgLen = bodyBuffer.getInt();
                if (singleMsgLen <= 0 || singleMsgLen > bodyBuffer.remaining()) {
                    throw new Exception(new Throwable("[Malformed Data]Invalid data len!channel is "
                            + channel.toString()));
                }
                byte[] record = new byte[singleMsgLen];
                bodyBuffer.get(record);

                ProxyMessage message = new ProxyMessage(groupId, streamId, commonAttrMap, record);
                msgList.add(message);
            }
        } else {
            msgList = new ArrayList<>(1);
            msgList.add(new ProxyMessage(groupId, streamId, commonAttrMap, bodyData));
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
    public Map<String, Object> extractData(ByteBuf cb, Channel channel) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        if (null == cb) {
            LOG.error("cb == null");
            return resultMap;
        }
        int totalLen = cb.readableBytes();
        if (ConfigConstants.MSG_MAX_LENGTH_BYTES < totalLen) {
            throw new Exception(new Throwable("err msg, ConfigConstants.MSG_MAX_LENGTH_BYTES "
                    + "< totalLen, and  totalLen=" + totalLen));
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

            if (msgType.getValue() >= MsgType.MSG_BIN_MULTI_BODY.getValue()) {
                resultMap.put(ConfigConstants.COMPRESS_TYPE, (compressType != 0) ? "snappy" : "");
                return extractNewBinData(resultMap, cb, channel, totalDataLen, msgType);
            } else {
                return extractDefaultData(resultMap, cb, channel, totalDataLen, msgType);
            }

        } else {
            // reset index.
            cb.resetReaderIndex();
            return null;
        }
    }
}
