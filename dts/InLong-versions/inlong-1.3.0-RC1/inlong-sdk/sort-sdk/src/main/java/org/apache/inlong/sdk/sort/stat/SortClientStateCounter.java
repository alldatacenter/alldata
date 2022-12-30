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

package org.apache.inlong.sdk.sort.stat;

import java.util.concurrent.atomic.AtomicLongArray;

public class SortClientStateCounter {

    private final AtomicLongArray count = new AtomicLongArray(21);
    public String sortTaskId;
    public String cacheClusterId;
    public String topic;
    public int partitionId;

    /**
     * SortClientStateCounter Constructor
     *
     * @param sortTaskId String
     * @param cacheClusterId String
     * @param topic String
     * @param partitionId int
     */
    public SortClientStateCounter(String sortTaskId, String cacheClusterId, String topic, int partitionId) {
        this.sortTaskId = sortTaskId;
        this.cacheClusterId = cacheClusterId;
        this.topic = topic;
        this.partitionId = partitionId;
    }

    /**
     * reset Counter
     *
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter reset() {
        SortClientStateCounter counter = new SortClientStateCounter(sortTaskId, cacheClusterId, topic, partitionId);
        for (int i = 0, len = counter.count.length(); i < len; i++) {
            counter.count.set(i, this.count.getAndSet(i, 0));
        }
        return counter;
    }

    /**
     * get double[] values
     *
     * @return double[]
     */
    public long[] getStatvalue() {
        long[] vals = new long[this.count.length()];
        for (int i = 0, len = this.count.length(); i < len; i++) {
            vals[i] = this.count.get(i);
        }
        return vals;
    }

    /**
     * consume byte size
     *
     * @param num long byte
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addConsumeSize(long num) {
        count.getAndAdd(0, num);
        return this;
    }

    /**
     * count receive event num
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addMsgCount(long num) {
        count.getAndAdd(1, num);
        return this;
    }

    /**
     * count callbak times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addCallbackTimes(long num) {
        count.getAndAdd(2, num);
        return this;
    }

    /**
     * count callbak done times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addCallbackDoneTimes(long num) {
        count.getAndAdd(3, num);
        return this;
    }

    /**
     * count callbak time cost
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addCallbackTimeCost(long num) {
        count.getAndAdd(4, num);
        return this;
    }

    /**
     * count callbak error times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addCallbackErrorTimes(long num) {
        count.getAndAdd(5, num);
        return this;
    }

    /**
     * count topic online times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addTopicOnlineTimes(long num) {
        count.getAndAdd(6, num);
        return this;
    }

    /**
     * count topic offline times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addTopicOfflineTimes(long num) {
        count.getAndAdd(7, num);
        return this;
    }

    /**
     * count ack fail times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addAckFailTimes(long num) {
        count.getAndAdd(8, num);
        return this;
    }

    /**
     * count ack succ times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addAckSuccTimes(long num) {
        count.getAndAdd(9, num);
        return this;
    }

    /**
     * count request manager times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addRequestManagerTimes(long num) {
        count.getAndAdd(10, num);
        return this;
    }

    /**
     * count request manager time cost
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addRequestManagerTimeCost(long num) {
        count.getAndAdd(11, num);
        return this;
    }

    /**
     * count request manager fail times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addRequestManagerFailTimes(long num) {
        count.getAndAdd(12, num);
        return this;
    }

    /**
     * count manager conf changed times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addManagerConfChangedTimes(long num) {
        count.getAndAdd(13, num);
        return this;
    }

    /**
     * count manager result code common error times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addRequestManagerCommonErrorTimes(long num) {
        count.getAndAdd(14, num);
        return this;
    }

    /**
     * count manager result param error times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addRequestManagerParamErrorTimes(long num) {
        count.getAndAdd(15, num);
        return this;
    }

    /**
     * count thread fetch times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addFetchTimes(long num) {
        count.getAndAdd(16, num);
        return this;
    }

    /**
     * count fetch error times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addFetchErrorTimes(long num) {
        count.getAndAdd(17, num);
        return this;
    }

    /**
     * count empty fetch times
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addEmptyFetchTimes(long num) {
        count.getAndAdd(18, num);
        return this;
    }

    /**
     * count fetch time cost
     *
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addFetchTimeCost(long num) {
        count.getAndAdd(19, num);
        return this;
    }

    /**
     * count decompression consume size
     * @param num long
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter addDecompressionConsumeSize(long num) {
        count.getAndAdd(20, num);
        return this;
    }
}
