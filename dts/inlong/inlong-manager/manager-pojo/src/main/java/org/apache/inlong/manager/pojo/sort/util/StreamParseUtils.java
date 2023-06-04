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

package org.apache.inlong.manager.pojo.sort.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.TransformType;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.manager.pojo.stream.StreamPipeline;
import org.apache.inlong.manager.pojo.stream.StreamTransform;
import org.apache.inlong.manager.pojo.transform.TransformDefinition;
import org.apache.inlong.manager.pojo.transform.deduplication.DeDuplicationDefinition;
import org.apache.inlong.manager.pojo.transform.encrypt.EncryptDefinition;
import org.apache.inlong.manager.pojo.transform.filter.FilterDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.IntervalJoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.JoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.LookUpJoinerDefinition;
import org.apache.inlong.manager.pojo.transform.joiner.TemporalJoinerDefinition;
import org.apache.inlong.manager.pojo.transform.replacer.StringReplacerDefinition;
import org.apache.inlong.manager.pojo.transform.splitter.SplitterDefinition;

/**
 * Utils of stream parse.
 */
public class StreamParseUtils {

    public static final String LEFT_NODE = "leftNode";
    public static final String RIGHT_NODE = "rightNode";
    public static final String SOURCE_TYPE = "sourceType";
    public static final String SOURCE_NAME = "sourceName";
    public static final String SINK_TYPE = "sinkType";
    public static final String SINK_NAME = "sinkName";
    public static final String TRANSFORM_TYPE = "transformType";
    public static final String TRANSFORM_NAME = "transformName";

    private static final Gson GSON = new Gson();

    public static TransformDefinition parseTransformDefinition(String transformDefinition,
            TransformType transformType) {
        JsonObject joinerJson = GSON.fromJson(transformDefinition, JsonObject.class);
        switch (transformType) {
            case FILTER:
                return GSON.fromJson(transformDefinition, FilterDefinition.class);
            case JOINER:
                return parseJoinerDefinition(transformDefinition, joinerJson);
            case SPLITTER:
                return GSON.fromJson(transformDefinition, SplitterDefinition.class);
            case DE_DUPLICATION:
                return GSON.fromJson(transformDefinition, DeDuplicationDefinition.class);
            case STRING_REPLACER:
                return GSON.fromJson(transformDefinition, StringReplacerDefinition.class);
            case ENCRYPT:
                return GSON.fromJson(transformDefinition, EncryptDefinition.class);
            case LOOKUP_JOINER:
                return parseLookupJoinerDefinition(transformDefinition, joinerJson);
            case TEMPORAL_JOINER:
                return parseTemporalJoinerDefinition(transformDefinition, joinerJson);
            case INTERVAL_JOINER:
                return parseIntervalJoinerDefinition(transformDefinition, joinerJson);
            default:
                throw new IllegalArgumentException(String.format("Unsupported transformType for %s", transformType));
        }
    }

    public static JoinerDefinition parseJoinerDefinition(String transformDefinition, JsonObject joinerJson) {
        JoinerDefinition joinerDefinition = GSON.fromJson(transformDefinition, JoinerDefinition.class);
        StreamNode leftStreamNode = parseNode(joinerJson.getAsJsonObject(LEFT_NODE));
        joinerDefinition.setLeftNode(leftStreamNode);
        StreamNode rightStreamNode = parseNode(joinerJson.getAsJsonObject(RIGHT_NODE));
        joinerDefinition.setRightNode(rightStreamNode);
        return joinerDefinition;
    }

    public static LookUpJoinerDefinition parseLookupJoinerDefinition(String transformDefinition,
            JsonObject joinerJson) {
        LookUpJoinerDefinition joinerDefinition = GSON.fromJson(transformDefinition, LookUpJoinerDefinition.class);
        StreamNode leftStreamNode = parseNode(joinerJson.getAsJsonObject(LEFT_NODE));
        joinerDefinition.setLeftNode(leftStreamNode);
        StreamNode rightStreamNode = parseNode(joinerJson.getAsJsonObject(RIGHT_NODE));
        joinerDefinition.setRightNode(rightStreamNode);
        return joinerDefinition;
    }

    public static TemporalJoinerDefinition parseTemporalJoinerDefinition(String transformDefinition,
            JsonObject joinerJson) {
        TemporalJoinerDefinition joinerDefinition = GSON.fromJson(transformDefinition, TemporalJoinerDefinition.class);
        StreamNode leftStreamNode = parseNode(joinerJson.getAsJsonObject(LEFT_NODE));
        joinerDefinition.setLeftNode(leftStreamNode);
        StreamNode rightStreamNode = parseNode(joinerJson.getAsJsonObject(RIGHT_NODE));
        joinerDefinition.setRightNode(rightStreamNode);
        return joinerDefinition;
    }

    public static IntervalJoinerDefinition parseIntervalJoinerDefinition(String transformDefinition,
            JsonObject joinerJson) {
        IntervalJoinerDefinition joinerDefinition = GSON.fromJson(transformDefinition, IntervalJoinerDefinition.class);
        StreamNode leftStreamNode = parseNode(joinerJson.getAsJsonObject(LEFT_NODE));
        joinerDefinition.setLeftNode(leftStreamNode);
        StreamNode rightStreamNode = parseNode(joinerJson.getAsJsonObject(RIGHT_NODE));
        joinerDefinition.setRightNode(rightStreamNode);
        return joinerDefinition;
    }

    private static StreamNode parseNode(JsonObject jsonObject) {
        if (jsonObject.has(SOURCE_TYPE)) {
            String sourceName = jsonObject.get(SOURCE_NAME).getAsString();
            StreamSource source = new StreamSource() {
            };
            source.setSourceName(sourceName);
            return source;
        } else if (jsonObject.has(SINK_TYPE)) {
            String sinkName = jsonObject.get(SINK_NAME).getAsString();
            StreamSink sink = new StreamSink() {
            };
            sink.setSinkName(sinkName);
            return sink;
        } else {
            String transformName = jsonObject.get(TRANSFORM_NAME).getAsString();
            StreamTransform transform = new StreamTransform() {

                @Override
                public String getTransformName() {
                    return super.getTransformName();
                }
            };
            transform.setTransformName(transformName);
            return transform;
        }
    }

    public static StreamPipeline parseStreamPipeline(String tempView, String inlongStreamId) {
        Preconditions.expectNotBlank(tempView, ErrorCodeEnum.INVALID_PARAMETER,
                String.format(" should not be null for streamId=%s", inlongStreamId));
        return GSON.fromJson(tempView, StreamPipeline.class);
    }

}
