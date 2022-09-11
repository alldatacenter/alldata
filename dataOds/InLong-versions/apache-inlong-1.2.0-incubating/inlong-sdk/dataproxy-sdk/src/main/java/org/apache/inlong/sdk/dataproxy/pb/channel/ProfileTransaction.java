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

package org.apache.inlong.sdk.dataproxy.pb.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.flume.Transaction;
import org.apache.inlong.sdk.commons.protocol.SdkEvent;
import org.apache.inlong.sdk.dataproxy.pb.context.CallbackProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProfileTransaction
 */
public class ProfileTransaction implements Transaction {

    public static final Logger LOG = LoggerFactory.getLogger(ProfileTransaction.class);

    private BufferQueue<CallbackProfile> bufferQueue;
    private List<CallbackProfile> takeList = new ArrayList<>();
    private AtomicLong takeCounter = new AtomicLong(0);
    private List<CallbackProfile> putList = new ArrayList<>();

    /**
     * Constructor
     * 
     * @param bufferQueue
     */
    public ProfileTransaction(BufferQueue<CallbackProfile> bufferQueue) {
        this.bufferQueue = bufferQueue;
    }

    /**
     * begin
     */
    @Override
    public void begin() {
    }

    /**
     * commit
     */
    @Override
    public void commit() {
        for (CallbackProfile profile : takeList) {
            SdkEvent event = profile.getEvent();
            bufferQueue.release(event.getBody().length);
        }
        this.takeList.clear();
        for (CallbackProfile event : putList) {
            this.bufferQueue.offer(event);
        }
        this.putList.clear();
    }

    /**
     * rollback
     */
    @Override
    public void rollback() {
        for (CallbackProfile event : takeList) {
            this.bufferQueue.offer(event);
        }
        this.takeList.clear();
        for (CallbackProfile profile : putList) {
            SdkEvent event = profile.getEvent();
            bufferQueue.release(event.getBody().length);
        }
        this.putList.clear();
    }

    /**
     * close
     */
    @Override
    public void close() {
    }

    /**
     * doTake
     * 
     * @param event
     */
    public void doTake(CallbackProfile event) {
        this.takeList.add(event);
        takeCounter.incrementAndGet();
    }

    /**
     * doPut
     * 
     * @param event
     */
    public void doPut(CallbackProfile event) {
        this.putList.add(event);
    }

    /**
     * commitTakeEvent
     */
    public void commitTakeEvent() {
        long current = this.takeCounter.decrementAndGet();
        if (current == 0) {
            this.commit();
        }
    }
}
