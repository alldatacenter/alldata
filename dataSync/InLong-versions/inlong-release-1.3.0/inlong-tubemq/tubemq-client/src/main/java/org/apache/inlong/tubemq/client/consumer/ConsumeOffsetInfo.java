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

package org.apache.inlong.tubemq.client.consumer;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class ConsumeOffsetInfo {
    private String partitionKey;
    private long currOffset = TBaseConstants.META_VALUE_UNDEFINED;
    private long maxOffset = TBaseConstants.META_VALUE_UNDEFINED;
    private long updateTime = TBaseConstants.META_VALUE_UNDEFINED;

    public ConsumeOffsetInfo(String partitionKey, long currOffset, long maxOffset) {
        this.partitionKey = partitionKey;
        this.currOffset = currOffset;
        this.maxOffset = maxOffset;
        this.updateTime = System.currentTimeMillis();
    }

    public ConsumeOffsetInfo(String partitionKey,
                             long currOffset,
                             long maxOffset,
                             long updateTime) {
        this.partitionKey = partitionKey;
        this.currOffset = currOffset;
        this.maxOffset = maxOffset;
        this.updateTime = updateTime;
    }

    public void updateOffsetInfo(long currOffset, long maxOffset) {
        boolean updated = false;
        if (currOffset >= 0) {
            this.currOffset = currOffset;
            updated = true;
        }
        if (maxOffset >= 0) {
            this.maxOffset = maxOffset;
            updated = true;
        }
        if (updated) {
            this.updateTime = System.currentTimeMillis();
        }
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public long getCurrOffset() {
        return currOffset;
    }

    public long getMaxOffset() {
        return maxOffset;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    @Override
    public String toString() {
        return this.partitionKey
            + TokenConstants.SEGMENT_SEP + this.currOffset
            + TokenConstants.ATTR_SEP + this.maxOffset;
    }
}
