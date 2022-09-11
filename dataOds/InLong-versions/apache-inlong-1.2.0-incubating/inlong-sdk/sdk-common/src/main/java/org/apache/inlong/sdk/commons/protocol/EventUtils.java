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

package org.apache.inlong.sdk.commons.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.inlong.sdk.commons.protocol.ProxySdk.INLONG_COMPRESSED_TYPE;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MapFieldEntry;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObjs;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePack;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessagePackHeader;
import org.apache.inlong.sdk.commons.utils.GzipUtils;
import org.xerial.snappy.Snappy;

import com.google.protobuf.ByteString;

/**
 * EventUtils
 */
public class EventUtils {

    /**
     * encode
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @param  compressedType
     * @param  events
     * @return MessagePack
     * @throws IOException
     */
    public static MessagePack encodeSdkEvents(String inlongGroupId, String inlongStreamId,
            INLONG_COMPRESSED_TYPE compressedType, List<SdkEvent> events) throws IOException {
        // MessageObjs
        MessageObjs.Builder objsBuilder = MessageObjs.newBuilder();
        for (SdkEvent event : events) {
            MessageObj.Builder objBuilder = MessageObj.newBuilder();
            objBuilder.setMsgTime(event.getMsgTime());
            objBuilder.setSourceIp(event.getSourceIp());
            objBuilder.setBody(ByteString.copyFrom(event.getBody()));
            objsBuilder.addMsgs(objBuilder.build());
        }
        MessageObjs objs = objsBuilder.build();
        byte[] srcBytes = objs.toByteArray();
        byte[] compressedBytes = null;
        switch (compressedType) {
            case INLONG_SNAPPY :
                compressedBytes = Snappy.compress(srcBytes);
                break;
            case INLONG_GZ :
                compressedBytes = GzipUtils.compress(srcBytes);
                break;
            case INLONG_NO_COMPRESS :
            default :
                compressedBytes = srcBytes;
                break;
        }
        // MessagePack
        MessagePack.Builder packBuilder = MessagePack.newBuilder();
        packBuilder.setCompressBytes(ByteString.copyFrom(compressedBytes));
        // MessagePackHeader
        MessagePackHeader.Builder headerBuilder = MessagePackHeader.newBuilder();
        // string inlongGroupId = 1; //inlongGroupId
        headerBuilder.setInlongGroupId(inlongGroupId);
        // string inlongStreamId = 2; //inlongStreamId
        headerBuilder.setInlongStreamId(inlongStreamId);
        // int64 packId = 3; //pack id
        headerBuilder.setPackId(0);
        // int64 packTime = 4; //pack time, milliseconds
        headerBuilder.setPackTime(System.currentTimeMillis());
        // int32 msgCount = 5; //message count
        headerBuilder.setMsgCount(events.size());
        // int32 srcLength = 6; //total length of raw messages body
        headerBuilder.setSrcLength(srcBytes.length);
        // int32 compressLen = 7; //compress length of messages
        headerBuilder.setCompressLen(compressedBytes.length);
        // INLONG_COMPRESSED_TYPE compressType = 8; //compress type
        headerBuilder.setCompressType(compressedType);
        // map<string, string> params = 9; //additional parameters
        packBuilder.setHeader(headerBuilder.build());
        return packBuilder.build();
    }

    /**
     * decodeSdkPack
     * 
     * @param  packObject
     * @return List,ProxyEvent
     * @throws IOException
     */
    public static List<ProxyEvent> decodeSdkPack(MessagePack packObject) throws IOException {
        MessagePackHeader header = packObject.getHeader();
        // decompress
        byte[] compressBytes = packObject.getCompressBytes().toByteArray();
        byte[] srcBytes = null;
        switch (header.getCompressType()) {
            case INLONG_SNAPPY :
                srcBytes = Snappy.uncompress(compressBytes);
                break;
            case INLONG_GZ :
                srcBytes = GzipUtils.decompress(compressBytes);
                break;
            case INLONG_NO_COMPRESS :
            default :
                srcBytes = compressBytes;
                break;
        }
        // decode
        MessageObjs msgObjs = MessageObjs.parseFrom(srcBytes);
        List<ProxyEvent> events = new ArrayList<>(msgObjs.getMsgsList().size());
        String inlongGroupId = header.getInlongGroupId();
        String inlongStreamId = header.getInlongStreamId();
        for (MessageObj msgObj : msgObjs.getMsgsList()) {
            ProxyEvent event = new ProxyEvent(inlongGroupId, inlongStreamId, msgObj);
            events.add(event);
        }
        return events;
    }

    /**
     * encodeCacheMessageBody
     * 
     * @param  compressedType
     * @param  events
     * @return byte array
     * @throws IOException
     */
    public static byte[] encodeCacheMessageBody(INLONG_COMPRESSED_TYPE compressedType, List<ProxyEvent> events)
            throws IOException {
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
        switch (compressedType) {
            case INLONG_SNAPPY :
                compressBytes = Snappy.compress(srcBytes);
                break;
            case INLONG_GZ :
                compressBytes = GzipUtils.compress(srcBytes);
                break;
            case INLONG_NO_COMPRESS :
            default :
                compressBytes = srcBytes;
                break;
        }
        return compressBytes;
    }

    /**
     * decodeCacheMessageBody
     * 
     * @param  inlongGroupId
     * @param  inlongStreamId
     * @param  compressedType
     * @param  msgBody
     * @return List,SortEvent
     * @throws IOException
     */
    public static List<SortEvent> decodeCacheMessageBody(String inlongGroupId, String inlongStreamId,
            INLONG_COMPRESSED_TYPE compressedType, byte[] msgBody) throws IOException {
        // uncompress
        byte[] srcBytes = null;
        switch (compressedType) {
            case INLONG_SNAPPY :
                srcBytes = Snappy.uncompress(msgBody);
                break;
            case INLONG_GZ :
                srcBytes = GzipUtils.decompress(msgBody);
                break;
            case INLONG_NO_COMPRESS :
            default :
                srcBytes = msgBody;
                break;
        }
        // decode
        MessageObjs msgObjs = MessageObjs.parseFrom(srcBytes);
        List<SortEvent> events = new ArrayList<>(msgObjs.getMsgsList().size());
        for (MessageObj msgObj : msgObjs.getMsgsList()) {
            SortEvent event = new SortEvent(inlongGroupId, inlongStreamId, msgObj);
            events.add(event);
        }
        return events;
    }
}
