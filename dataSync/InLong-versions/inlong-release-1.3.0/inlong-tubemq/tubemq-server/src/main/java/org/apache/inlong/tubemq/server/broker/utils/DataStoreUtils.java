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

package org.apache.inlong.tubemq.server.broker.utils;

import com.google.protobuf.ByteString;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.HashMap;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.utils.MessageFlagUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.stats.TrafficInfo;

/**
 * Storage util. Used for data and index file storage format.
 */
public class DataStoreUtils {
    // Data storage format definition

    // Message storage structure
    // message length       4
    // + dataType           4
    // + checksum           4
    // + queueId            4
    // + queueLogicOffset   8
    // + receivedTime       8
    // + reportAddr         4
    // + keyCode            4
    // + msgId              8
    // + flag               4
    // + data               0
    //
    public static final int MAX_MSG_TRANSFER_SIZE = 1024 * 1024;

    public static final int STORE_DATA_PREFX_LEN = 48;
    public static final int STORE_DATA_HEADER_LEN = STORE_DATA_PREFX_LEN + 4;
    public static final int STORE_HEADER_POS_LENGTH = 0;
    public static final int STORE_HEADER_POS_DATATYPE = 4;
    public static final int STORE_HEADER_POS_CHECKSUM = 8;
    public static final int STORE_HEADER_POS_QUEUEID = 12;
    public static final int STORE_HEADER_POS_QUEUE_LOGICOFF = 16;
    public static final int STORE_HEADER_POS_RECEIVEDTIME = 24;
    public static final int STORE_HEADER_POS_REPORTADDR = 32;
    public static final int STORE_HEADER_POS_KEYCODE = 36;
    public static final int STORE_HEADER_POS_MSGID = 40;
    public static final int STORE_HEADER_POS_MSGFLAG = 48;
    public static final int STORE_HEADER_POS_MSGDATA = 52;
    public static final int STORE_DATA_TOKER_BEGIN_VALUE = 0x2C998B8;
    public static final int STORE_DATA_TOKER_BLANK_VALUE = 0x2C99B5E;
    public static final long MAX_FILE_ROLL_CHECK_DURATION = 1 * 3600 * 1000;
    public static final long MAX_FILE_NO_WRITE_DURATION = 2 * 24 * 3600 * 1000;
    public static final long MAX_FILE_VALID_DURATION = 168 * 3600L * 1000L;

    // Index storage structure
    // partitionId         4
    // offset              8
    // + getCachedSize     4
    // + keyCode           4
    // + timeInMillSec     8
    public static final int STORE_INDEX_HEAD_LEN = 28;
    public static final int INDEX_POS_PARTITIONID = 0;
    public static final int INDEX_POS_DATAOFFSET = 4;
    public static final int INDEX_POS_MSG_SIZE = 12;
    public static final int INDEX_POS_KEY_CODE = 16;
    public static final int INDEX_POS_TIME_RECV = 20;

    public static final int MAX_MSG_DATA_STORE_SIZE =
            TBaseConstants.META_MAX_MESSAGE_DATA_SIZE_UPPER_LIMIT
                    + TBaseConstants.META_MB_UNIT_SIZE * 8;
    public static final int STORE_MAX_MESSAGE_STORE_LEN
            = STORE_DATA_HEADER_LEN + MAX_MSG_DATA_STORE_SIZE;

    public static final String DATA_FILE_SUFFIX = ".tube";
    public static final String INDEX_FILE_SUFFIX = ".index";

    public static int getInt(final int offset, final byte[] data) {
        return ByteBuffer.wrap(data, offset, 4).getInt();
    }

    public static String nameFromOffset(final long offset, final String fileSuffix) {
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(20);
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(false);
        return nf.format(offset) + fileSuffix;
    }

    /**
     * Convert inner message to protobuf format, then reply to client.
     *
     * @param dataBuffer      the raw stored data
     * @param dataTotalSize   the data size
     * @param countMap        the statistics map
     * @param statisKeyBase   the statistics key prefix
     * @param sBuilder        the string buffer
     * @return                the converted messages
     */
    public static ClientBroker.TransferedMessage getTransferMsg(ByteBuffer dataBuffer, int dataTotalSize,
                                                                HashMap<String, TrafficInfo> countMap,
                                                                String statisKeyBase,
                                                                StringBuilder sBuilder) {
        if (dataBuffer.array().length < dataTotalSize) {
            return null;
        }
        final int msgLen =
                dataBuffer.getInt(DataStoreUtils.STORE_HEADER_POS_LENGTH);
        final int msgToken =
                dataBuffer.getInt(DataStoreUtils.STORE_HEADER_POS_DATATYPE);
        final int checkSum =
                dataBuffer.getInt(DataStoreUtils.STORE_HEADER_POS_CHECKSUM);
        int payLoadLen = msgLen - DataStoreUtils.STORE_DATA_PREFX_LEN;
        int payLoadOffset = DataStoreUtils.STORE_DATA_HEADER_LEN;
        if ((msgToken != DataStoreUtils.STORE_DATA_TOKER_BEGIN_VALUE)
                || (payLoadLen <= 0)
                || (payLoadLen > dataTotalSize - DataStoreUtils.STORE_DATA_HEADER_LEN)) {
            return null;
        }
        final long msgId = dataBuffer.getLong(DataStoreUtils.STORE_HEADER_POS_MSGID);
        final int flag = dataBuffer.getInt(DataStoreUtils.STORE_HEADER_POS_MSGFLAG);
        final int payLoadLen2 = payLoadLen;
        final byte[] payLoadData = new byte[payLoadLen];
        System.arraycopy(dataBuffer.array(), payLoadOffset, payLoadData, 0, payLoadLen);
        ClientBroker.TransferedMessage.Builder dataBuilder =
                ClientBroker.TransferedMessage.newBuilder();
        dataBuilder.setMessageId(msgId);
        dataBuilder.setCheckSum(checkSum);
        dataBuilder.setFlag(flag);
        dataBuilder.setPayLoadData(ByteString.copyFrom(payLoadData));
        // get statistic data
        int attrLen = 0;
        String attribute = null;
        if (MessageFlagUtils.hasAttribute(flag)) {
            if (payLoadLen < 4) {
                return null;
            }
            attrLen = dataBuffer.getInt(DataStoreUtils.STORE_DATA_HEADER_LEN);
            payLoadOffset += 4;
            payLoadLen -= 4;
            if (attrLen > payLoadLen) {
                return null;
            }
            if (attrLen > 0) {
                final byte[] attrData = new byte[attrLen];
                System.arraycopy(dataBuffer.array(), payLoadOffset, attrData, 0, attrLen);
                try {
                    attribute = new String(attrData, TBaseConstants.META_DEFAULT_CHARSET_NAME);
                } catch (final UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        String messageTime = "";
        if (TStringUtils.isNotBlank(attribute)) {
            if (attribute.contains(TokenConstants.TOKEN_MSG_TIME)) {

                String[] strAttrs = attribute.split(TokenConstants.ARRAY_SEP);
                for (String strAttrItem : strAttrs) {
                    if (TStringUtils.isNotBlank(strAttrItem)) {
                        if (strAttrItem.contains(TokenConstants.TOKEN_MSG_TIME)) {
                            String[] strItems = strAttrItem.split(TokenConstants.EQ);
                            if (strItems.length > 1) {
                                messageTime = strItems[1];
                            }
                        }
                    }
                }
            }
        }
        String baseKey = sBuilder.append(statisKeyBase)
                .append("#").append(messageTime).toString();
        sBuilder.delete(0, sBuilder.length());
        TrafficInfo getCount = countMap.get(baseKey);
        if (getCount == null) {
            countMap.put(baseKey, new TrafficInfo(1L, payLoadLen2));
        } else {
            getCount.addMsgCntAndSize(1L, payLoadLen2);
        }
        ClientBroker.TransferedMessage transferedMessage = dataBuilder.build();
        dataBuilder.clear();
        return transferedMessage;
    }
}
