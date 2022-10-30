/*
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

package org.apache.inlong.tubemq.corebase.utils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.inlong.tubemq.corebase.Message;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class MixedUtils {
    // java version cache
    private static String javaVersion = "";

    static {
        javaVersion = System.getProperty("java.version");
    }

    public static String getJavaVersion() {
        if (TStringUtils.isEmpty(javaVersion)) {
            return "";
        } else {
            int maxLen = Math.min(javaVersion.length(), 100);
            return javaVersion.substring(0, maxLen);
        }
    }

    /**
     * parse topic Parameter with format topic_1[,topic_2[:filterCond_2.1[;filterCond_2.2]]]
     *  topicParam->set(filterCond) map
     * @param topicParam - composite string
     * @return - map of topic->set(filterCond)
     */
    public static Map<String, TreeSet<String>> parseTopicParam(String topicParam) {
        Map<String, TreeSet<String>> topicAndFiltersMap = new HashMap<>();
        if (TStringUtils.isBlank(topicParam)) {
            return topicAndFiltersMap;
        }
        String[] topicFilterStrs = topicParam.split(TokenConstants.ARRAY_SEP);
        for (String topicFilterStr : topicFilterStrs) {
            if (TStringUtils.isBlank(topicFilterStr)) {
                continue;
            }
            String[] topicFilters = topicFilterStr.split(TokenConstants.ATTR_SEP);
            if (TStringUtils.isBlank(topicFilters[0])) {
                continue;
            }
            TreeSet<String> filterSet = new TreeSet<>();
            if (topicFilters.length > 1
                    && TStringUtils.isNotBlank(topicFilters[1])) {
                String[] filterItems = topicFilters[1].split(TokenConstants.LOG_SEG_SEP);
                for (String filterItem : filterItems) {
                    if (TStringUtils.isBlank(filterItem)) {
                        continue;
                    }
                    filterSet.add(filterItem.trim());
                }
            }
            topicAndFiltersMap.put(topicFilters[0].trim(), filterSet);
        }
        return topicAndFiltersMap;
    }

    // build the topic and filter item pair carried in the message
    public static List<Tuple2<String, String>> buildTopicFilterTupleList(
            Map<String, TreeSet<String>> topicAndFiltersMap) {
        // initial send target
        List<Tuple2<String, String>> topicFilterTuples = new ArrayList<>();
        // initial topic send round
        for (Map.Entry<String, TreeSet<String>> entry: topicAndFiltersMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                topicFilterTuples.add(new Tuple2<>(entry.getKey()));
            } else {
                for (String filter : entry.getValue()) {
                    topicFilterTuples.add(new Tuple2<>(entry.getKey(), filter));
                }
            }
        }
        return topicFilterTuples;
    }

    // only for demo
    public static byte[] buildTestData(int bodySize) {
        final byte[] transmitData =
                StringUtils.getBytesUtf8("This is a test data!");
        final ByteBuffer dataBuffer = ByteBuffer.allocate(bodySize);
        while (dataBuffer.hasRemaining()) {
            int offset = dataBuffer.arrayOffset();
            dataBuffer.put(transmitData, offset,
                    Math.min(dataBuffer.remaining(), transmitData.length));
        }
        dataBuffer.flip();
        return dataBuffer.array();
    }

    // build message to be sent
    // only for demo
    public static Message buildMessage(String topicName, String filterItem,
                                       byte[] bodyData, long serialId) {
        // build message to be sent
        Message message = new Message(topicName, bodyData);
        long currTimeMillis = System.currentTimeMillis();
        // added a serial number and data generation time to each message
        message.setAttrKeyVal("serialId", String.valueOf(serialId));
        message.setAttrKeyVal("dataTime", String.valueOf(currTimeMillis));
        // add filter attribute information, time require yyyyMMddHHmm format
        message.putSystemHeader(filterItem,
                DateTimeConvertUtils.ms2yyyyMMddHHmm(currTimeMillis));
        return message;
    }

    // only for demo
    public static void coolSending(long msgSentCount) {
        if (msgSentCount % 5000 == 0) {
            ThreadUtils.sleep(3000);
        } else if (msgSentCount % 4000 == 0) {
            ThreadUtils.sleep(2000);
        } else if (msgSentCount % 2000 == 0) {
            ThreadUtils.sleep(800);
        } else if (msgSentCount % 1000 == 0) {
            ThreadUtils.sleep(400);
        }
    }

    // get the middle data between min, max, and data
    public static int mid(int data, int min, int max) {
        return Math.max(min, Math.min(max, data));
    }

    public static long mid(long data, long min, long max) {
        return Math.max(min, Math.min(max, data));
    }

}
