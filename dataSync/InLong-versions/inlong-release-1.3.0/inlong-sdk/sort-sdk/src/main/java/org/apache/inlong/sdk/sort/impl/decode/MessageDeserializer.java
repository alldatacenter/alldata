/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sdk.sort.impl.decode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MapFieldEntry;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObjs;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.Deserializer;
import org.apache.inlong.sdk.sort.entity.InLongMessage;
import org.apache.inlong.sdk.sort.entity.InLongTopic;
import org.apache.inlong.sdk.sort.util.Utils;

public class MessageDeserializer implements Deserializer {

    private static final int MESSAGE_VERSION_NONE = 0;
    private static final int MESSAGE_VERSION_PB = 1;
    private static final int COMPRESS_TYPE_NONE = 0;
    private static final int COMPRESS_TYPE_GZIP = 1;
    private static final int COMPRESS_TYPE_SNAPPY = 2;
    private static final String VERSION_KEY = "version";
    private static final String COMPRESS_TYPE_KEY = "compressType";
    private static final String MSG_TIME_KEY = "msgTime";
    private static final String SOURCE_IP_KEY = "sourceIp";
    private static final String INLONG_GROUPID_KEY = "inlongGroupId";
    private static final String INLONG_STREAMID_KEY = "inlongStreamId";

    public MessageDeserializer() {
    }

    @Override
    public List<InLongMessage> deserialize(ClientContext context, InLongTopic inLongTopic, Map<String, String> headers,
            byte[] data) throws Exception {

        //1. version
        int version = Integer.parseInt(headers.getOrDefault(VERSION_KEY, "0"));
        switch (version) {
            case MESSAGE_VERSION_NONE: {
                return decode(context, inLongTopic, data, headers);
            }
            case MESSAGE_VERSION_PB: {
                return decodePB(context, inLongTopic, data, headers);
            }
            default:
                throw new IllegalArgumentException("Unknown version type:" + version);
        }
    }

    private List<InLongMessage> decode(ClientContext context, InLongTopic inLongTopic, byte[] msgBytes,
            Map<String, String> headers) {
        long msgTime = Long.parseLong(headers.getOrDefault(MSG_TIME_KEY, "0"));
        String sourceIp = headers.getOrDefault(SOURCE_IP_KEY, "");
        String inlongGroupId = headers.getOrDefault(INLONG_GROUPID_KEY, "");
        String inlongStreamId = headers.getOrDefault(INLONG_STREAMID_KEY, "");
        context.getStatManager()
                .getStatistics(context.getConfig().getSortTaskId(),
                        inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                .addDecompressionConsumeSize(msgBytes.length);
        return Collections
                .singletonList(new InLongMessage(inlongGroupId, inlongStreamId, msgTime, sourceIp, msgBytes, headers));
    }

    /**
     * uncompress and decode byte[]
     *
     * @param msgBytes byte[]
     * @return {@link MessageObjs}
     */
    private List<InLongMessage> decodePB(ClientContext context, InLongTopic inLongTopic, byte[] msgBytes,
            Map<String, String> headers) throws IOException {
        int compressType = Integer.parseInt(headers.getOrDefault(COMPRESS_TYPE_KEY, "0"));
        String inlongGroupId = headers.getOrDefault(INLONG_GROUPID_KEY, "");
        String inlongStreamId = headers.getOrDefault(INLONG_STREAMID_KEY, "");
        switch (compressType) {
            case COMPRESS_TYPE_NONE: {
                return transformMessageObjs(context, inLongTopic, MessageObjs.parseFrom(msgBytes), inlongGroupId,
                        inlongStreamId);
            }
            case COMPRESS_TYPE_SNAPPY: {
                byte[] values = Utils.snappyDecompress(msgBytes, 0, msgBytes.length);
                return transformMessageObjs(context, inLongTopic, MessageObjs.parseFrom(values), inlongGroupId,
                        inlongStreamId);
            }
            case COMPRESS_TYPE_GZIP: {
                byte[] values = Utils.gzipDecompress(msgBytes, 0, msgBytes.length);
                return transformMessageObjs(context, inLongTopic, MessageObjs.parseFrom(values), inlongGroupId,
                        inlongStreamId);
            }
            default:
                throw new IllegalArgumentException("Unknown compress type:" + compressType);
        }
    }

    /**
     * transform MessageObjs to SortSdkMessage
     *
     * @param messageObjs {@link MessageObjs}
     * @return {@link List}
     */
    private List<InLongMessage> transformMessageObjs(ClientContext context, InLongTopic inLongTopic,
            MessageObjs messageObjs, String inlongGroupId,
            String inlongStreamId) {
        if (null == messageObjs) {
            return null;
        }
        List<InLongMessage> inLongMessages = new ArrayList<>();
        for (MessageObj messageObj : messageObjs.getMsgsList()) {
            List<MapFieldEntry> mapFieldEntries = messageObj.getParamsList();
            Map<String, String> headers = new HashMap<>();
            for (MapFieldEntry mapFieldEntry : mapFieldEntries) {
                headers.put(mapFieldEntry.getKey(), mapFieldEntry.getValue());
            }
            InLongMessage inLongMessage = new InLongMessage(inlongGroupId, inlongStreamId, messageObj.getMsgTime(),
                    messageObj.getSourceIp(),
                    messageObj.getBody().toByteArray(), headers);
            inLongMessages.add(inLongMessage);
            context.getStatManager()
                    .getStatistics(context.getConfig().getSortTaskId(),
                            inLongTopic.getInLongCluster().getClusterId(), inLongTopic.getTopic())
                    .addDecompressionConsumeSize(inLongMessage.getBody().length);
        }
        return inLongMessages;
    }
}
