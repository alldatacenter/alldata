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

package org.apache.inlong.sort.standalone.channel;

import org.apache.inlong.sdk.sort.api.SortClient;
import org.apache.inlong.sdk.sort.entity.MessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * CacheMessageRecord
 */
public class CacheMessageRecord {

    public static final Logger LOG = LoggerFactory.getLogger(CacheMessageRecord.class);
    private final SortClient client;
    private final String msgKey;
    private final String offset;
    private final AtomicInteger ackCount;

    /**
     * Constructor
     * 
     * @param msgRecord
     * @param client
     */
    public CacheMessageRecord(MessageRecord msgRecord, SortClient client) {
        this.msgKey = msgRecord.getMsgKey();
        this.offset = msgRecord.getOffset();
        this.ackCount = new AtomicInteger(msgRecord.getMsgs().size());
        this.client = client;
    }

    /**
     * ackMessage
     */
    public void ackMessage() {
        int result = this.ackCount.decrementAndGet();
        if (result == 0 && client != null) {
            try {
                client.ack(msgKey, offset);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }
}
