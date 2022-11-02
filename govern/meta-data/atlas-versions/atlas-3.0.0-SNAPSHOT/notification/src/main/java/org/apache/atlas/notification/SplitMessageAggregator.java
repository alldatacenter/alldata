/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification;


import org.apache.atlas.model.notification.AtlasNotificationStringMessage;

public class SplitMessageAggregator {
    private final String                           msgId;
    private final AtlasNotificationStringMessage[] splitMessagesBuffer;
    private final long                             firstSplitTimestamp;

    public SplitMessageAggregator(AtlasNotificationStringMessage message) {
        msgId               = message.getMsgId();
        splitMessagesBuffer = new AtlasNotificationStringMessage[message.getMsgSplitCount()];
        firstSplitTimestamp = System.currentTimeMillis();

        add(message);
    }

    public String getMsgId() {
        return msgId;
    }

    public long getTotalSplitCount() {
        return splitMessagesBuffer.length;
    }

    public long getReceivedSplitCount() {
        long ret = 0;

        for (AtlasNotificationStringMessage split : splitMessagesBuffer) {
            if (split != null) {
                ret++;
            }
        }

        return ret;
    }

    public long getFirstSplitTimestamp() {
        return firstSplitTimestamp;
    }

    public boolean add(AtlasNotificationStringMessage message) {
        if (message.getMsgSplitIdx() < splitMessagesBuffer.length) {
            splitMessagesBuffer[message.getMsgSplitIdx()] = message;
        }

        return message.getMsgSplitIdx() == (message.getMsgSplitCount() - 1);
    }

    public AtlasNotificationStringMessage get(int i) {
        return splitMessagesBuffer[i];
    }
}
