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

package org.apache.inlong.dataproxy.sink.common;

import com.google.protobuf.ByteString;

import org.apache.inlong.dataproxy.config.pojo.IdTopicConfig;
import org.apache.inlong.dataproxy.sink.mq.BatchPackProfile;
import org.apache.inlong.sdk.commons.protocol.EventConstants;
import org.apache.inlong.sdk.commons.protocol.ProxyEvent;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MapFieldEntry;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObjs;
import org.apache.inlong.sdk.commons.utils.GzipUtils;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sdk.commons.protocol.EventConstants.HEADER_CACHE_VERSION_1;
import static org.apache.inlong.sdk.commons.protocol.EventConstants.HEADER_KEY_VERSION;

/**
 * DefaultEventHandler
 * 
 */
public class DefaultEventHandler implements EventHandler {

    /**
     * parseHeader
     */
    @Override
    public Map<String, String> parseHeader(IdTopicConfig idConfig, BatchPackProfile profile, String nodeId,
            INLONG_COMPRESSED_TYPE compressType) {
        Map<String, String> headers = new HashMap<>();
        // version int32 protocol version, the value is 1
        headers.put(HEADER_KEY_VERSION, HEADER_CACHE_VERSION_1);
        // inlongGroupId string inlongGroupId
        headers.put(EventConstants.INLONG_GROUP_ID, profile.getInlongGroupId());
        // inlongStreamId string inlongStreamId
        headers.put(EventConstants.INLONG_STREAM_ID, profile.getInlongStreamId());
        // proxyName string proxy node id, IP or conainer name
        headers.put(EventConstants.HEADER_KEY_PROXY_NAME, nodeId);
        // packTime int64 pack time, milliseconds
        headers.put(EventConstants.HEADER_KEY_PACK_TIME, String.valueOf(System.currentTimeMillis()));
        // msgCount int32 message count
        headers.put(EventConstants.HEADER_KEY_MSG_COUNT, String.valueOf(profile.getEvents().size()));
        // srcLength int32 total length of raw messages body
        headers.put(EventConstants.HEADER_KEY_SRC_LENGTH, String.valueOf(profile.getSize()));
        // compressType int
        // compress type of body data
        // INLONG_NO_COMPRESS = 0,
        // INLONG_GZ = 1,
        // INLONG_SNAPPY = 2
        headers.put(EventConstants.HEADER_KEY_COMPRESS_TYPE,
                String.valueOf(compressType.getNumber()));
        // messageKey string partition hash key, optional
        return headers;
    }

    /**
     * parseBody
     */
    @Override
    public byte[] parseBody(IdTopicConfig idConfig, BatchPackProfile profile, INLONG_COMPRESSED_TYPE compressType)
            throws IOException {
        List<ProxyEvent> events = profile.getEvents();
        // encode
        MessageObjs.Builder objs = MessageObjs.newBuilder();
        for (ProxyEvent event : events) {
            MessageObj.Builder builder = MessageObj.newBuilder();
            builder.setMsgTime(event.getMsgTime());
            builder.setSourceIp(event.getSourceIp());
            event.getHeaders().forEach((key, value) -> {
                builder.addParams(MapFieldEntry.newBuilder().setKey(key).setValue(value));
            });
            builder.setBody(ByteString.copyFrom(event.getBody()));
            objs.addMsgs(builder.build());
        }
        byte[] srcBytes = objs.build().toByteArray();
        // compress
        byte[] compressBytes = null;
        switch (compressType) {
            case INLONG_SNAPPY:
                compressBytes = Snappy.compress(srcBytes);
                break;
            case INLONG_GZ:
                compressBytes = GzipUtils.compress(srcBytes);
                break;
            case INLONG_NO_COMPRESS:
            default:
                compressBytes = srcBytes;
                break;
        }
        return compressBytes;
    }

}
