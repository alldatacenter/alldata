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

package org.apache.inlong.tubemq.server.broker.stats;

/**
 * Statistic of message, contains message's count and message's size.
 */
public class TrafficInfo {
    private long msgCnt = 0L;
    private long msgSize = 0L;

    public TrafficInfo() {
        clear();
    }

    public TrafficInfo(long msgCount, long msgSize) {
        this.msgCnt = msgCount;
        this.msgSize = msgSize;
    }

    public long getMsgCount() {
        return msgCnt;
    }

    public long getMsgSize() {
        return msgSize;
    }

    public void addMsgCntAndSize(long msgCount, long msgSize) {
        this.msgCnt += msgCount;
        this.msgSize += msgSize;
    }

    public void clear() {
        this.msgCnt = 0L;
        this.msgSize = 0L;
    }
}
