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
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.common.util.StringUtil;
import org.apache.inlong.manager.pojo.consume.BriefMQMessage;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class InlongMsgDeserializeOperator implements DeserializeOperator {

    @Override
    public boolean accept(DataProxyMsgEncType type) {
        return DataProxyMsgEncType.MSG_ENCODE_TYPE_INLONGMSG.equals(type);
    }

    @Override
    public List<BriefMQMessage> decodeMsg(InlongStreamInfo streamInfo,
            byte[] msgBytes, Map<String, String> headers, int index) throws Exception {
        String groupId = headers.get(AttributeConstants.GROUP_ID);
        String streamId = headers.get(AttributeConstants.STREAM_ID);
        List<BriefMQMessage> messageList = new ArrayList<>();
        InLongMsg inLongMsg = InLongMsg.parseFrom(msgBytes);
        for (String attr : inLongMsg.getAttrs()) {
            Map<String, String> attributes = StringUtil.splitKv(attr, INLONGMSG_ATTR_ENTRY_DELIMITER,
                    INLONGMSG_ATTR_KV_DELIMITER, null, null);
            // Extracts time from the attributes
            long msgTime;
            if (attributes.containsKey(INLONGMSG_ATTR_TIME_T)) {
                String date = attributes.get(INLONGMSG_ATTR_TIME_T).trim();
                msgTime = StringUtil.parseDateTime(date);
            } else if (attributes.containsKey(INLONGMSG_ATTR_TIME_DT)) {
                String epoch = attributes.get(INLONGMSG_ATTR_TIME_DT).trim();
                msgTime = Long.parseLong(epoch);
            } else {
                throw new IllegalArgumentException(String.format("PARSE_ATTR_ERROR_STRING%s",
                        INLONGMSG_ATTR_TIME_T + " or " + INLONGMSG_ATTR_TIME_DT));
            }
            Iterator<byte[]> iterator = inLongMsg.getIterator(attr);
            while (iterator.hasNext()) {
                byte[] bodyBytes = iterator.next();
                if (Objects.isNull(bodyBytes)) {
                    continue;
                }
                BriefMQMessage inLongMessage =
                        new BriefMQMessage(index, groupId, streamId, msgTime, attributes.get(CLIENT_IP),
                                new String(bodyBytes, Charset.forName(streamInfo.getDataEncoding())));
                messageList.add(inLongMessage);
            }
        }
        return messageList;
    }
}
