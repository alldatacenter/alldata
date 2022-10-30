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

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class KeyBuilderUtils {

    public static String buildGroupTopicRecKey(String groupName, String topicName) {
        return new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                .append(topicName).append(TokenConstants.ATTR_SEP)
                .append(groupName).toString();
    }

    public static Tuple2<String, String> splitRecKey2GroupTopic(String recordKey) {
        Tuple2<String, String> ret = new Tuple2<>();
        if (recordKey == null) {
            return ret;
        }
        String[] items = recordKey.split(TokenConstants.ATTR_SEP);
        // return [topicName, groupName]
        if (items.length < 2) {
            ret.setF0AndF1(items[0], "");
        } else {
            ret.setF0AndF1(items[0], items[1]);
        }
        return ret;
    }

    public static String buildTopicConfRecKey(int brokerId, String topicName) {
        return new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                .append(brokerId).append(TokenConstants.ATTR_SEP)
                .append(topicName).toString();
    }

    public static String buildAddressInfo(String brokerIp, int brokerPort) {
        return new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                .append(brokerIp).append(TokenConstants.ATTR_SEP)
                .append(brokerPort).toString();
    }

}
