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

import java.util.ArrayList;
import java.util.List;

import org.apache.flume.Transaction;
import org.apache.inlong.sort.standalone.utils.BufferQueue;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * ProfileTransaction
 */
public class ProfileTransaction implements Transaction {

    public static final Logger LOG = InlongLoggerFactory.getLogger(ProfileTransaction.class);

    private BufferQueue<ProfileEvent> bufferQueue;
    private List<ProfileEvent> takeList = new ArrayList<>();
    private List<ProfileEvent> putList = new ArrayList<>();

    /**
     * Constructor
     * 
     * @param bufferQueue
     */
    public ProfileTransaction(BufferQueue<ProfileEvent> bufferQueue) {
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
        for (ProfileEvent event : takeList) {
            bufferQueue.release(event.getBody().length);
        }
        this.takeList.clear();
        for (ProfileEvent event : putList) {
            this.bufferQueue.offer(event);
        }
        this.putList.clear();
    }

    /**
     * rollback
     */
    @Override
    public void rollback() {
        for (ProfileEvent event : takeList) {
            this.bufferQueue.offer(event);
        }
        this.takeList.clear();
        for (ProfileEvent event : putList) {
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
    public void doTake(ProfileEvent event) {
        this.takeList.add(event);
    }

    /**
     * doPut
     * 
     * @param event
     */
    public void doPut(ProfileEvent event) {
        this.putList.add(event);
    }
}
