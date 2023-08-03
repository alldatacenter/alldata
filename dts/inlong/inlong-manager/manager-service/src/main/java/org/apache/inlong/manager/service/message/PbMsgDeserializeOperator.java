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

package org.apache.inlong.manager.service.message;

import org.apache.inlong.common.enums.DataProxyMsgEncType;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.common.util.Utils;
import org.apache.inlong.manager.pojo.consume.BriefMQMessage;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MapFieldEntry;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObjs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PbMsgDeserializeOperator implements DeserializeOperator {

    @Override
    public boolean accept(DataProxyMsgEncType type) {
        return DataProxyMsgEncType.MSG_ENCODE_TYPE_PB.equals(type);
    }

    @Override
    public List<BriefMQMessage> decodeMsg(InlongStreamInfo streamInfo,
            byte[] msgBytes, Map<String, String> headers, int index) throws Exception {
        List<BriefMQMessage> messageList = new ArrayList<>();
        int compressType = Integer.parseInt(headers.getOrDefault(COMPRESS_TYPE_KEY, "0"));
        byte[] values = msgBytes;
        switch (compressType) {
            case INLONG_COMPRESSED_TYPE.INLONG_NO_COMPRESS_VALUE:
                break;
            case INLONG_COMPRESSED_TYPE.INLONG_GZ_VALUE:
                values = Utils.gzipDecompress(msgBytes, 0, msgBytes.length);
                break;
            case INLONG_COMPRESSED_TYPE.INLONG_SNAPPY_VALUE:
                values = Utils.snappyDecompress(msgBytes, 0, msgBytes.length);
                break;
            default:
                throw new IllegalArgumentException("Unknown compress type:" + compressType);
        }
        messageList = transformMessageObjs(MessageObjs.parseFrom(values), streamInfo, index);
        return messageList;
    }

    private List<BriefMQMessage> transformMessageObjs(MessageObjs messageObjs, InlongStreamInfo streamInfo, int index) {
        if (null == messageObjs) {
            return null;
        }
        List<BriefMQMessage> briefMQMessages = new ArrayList<>();
        for (MessageObj messageObj : messageObjs.getMsgsList()) {
            List<MapFieldEntry> mapFieldEntries = messageObj.getParamsList();
            Map<String, String> headers = new HashMap<>();
            for (MapFieldEntry mapFieldEntry : mapFieldEntries) {
                headers.put(mapFieldEntry.getKey(), mapFieldEntry.getValue());
            }
            BriefMQMessage briefMQMessage = new BriefMQMessage(index, headers.get(AttributeConstants.GROUP_ID),
                    headers.get(AttributeConstants.STREAM_ID), messageObj.getMsgTime(),
                    headers.get(CLIENT_IP),
                    new String(messageObj.getBody().toByteArray(), Charset.forName(streamInfo.getDataEncoding())));
            briefMQMessages.add(briefMQMessage);
        }
        return briefMQMessages;
    }
}
