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

package org.apache.inlong.sdk.sort.entity;

import java.util.List;

public class MessageRecord {

    private final String msgKey;
    private final List<InLongMessage> msgs;
    private final String offset;
    private final long recTime;

    public MessageRecord(String msgKey, List<InLongMessage> msgs, String offset, long recTime) {
        this.msgKey = msgKey;
        this.msgs = msgs;
        this.offset = offset;
        this.recTime = recTime;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public List<InLongMessage> getMsgs() {
        return msgs;
    }

    public String getOffset() {
        return offset;
    }

    public long getRecTime() {
        return recTime;
    }

    @Override
    public String toString() {
        return "MessageRecord{"
                + "msgKey='" + msgKey
                + ", message=" + String.valueOf(msgs)
                + ", offset='" + offset
                + ", recTime=" + recTime
                + '}';
    }
}
