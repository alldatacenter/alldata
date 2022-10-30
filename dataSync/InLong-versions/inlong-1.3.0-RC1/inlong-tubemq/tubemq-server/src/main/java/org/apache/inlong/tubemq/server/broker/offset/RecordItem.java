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

package org.apache.inlong.tubemq.server.broker.offset;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

/**
 * The offset snapshot of the consumer group on the partition.
 */
public class RecordItem {
    protected int storeId;
    protected int partitionId;
    // consume group confirmed offset
    protected long offsetCfm = 0L;
    // partition min index offset
    protected long offsetMin = 0L;
    // partition max index offset
    protected long offsetMax = 0L;
    // consume lag
    protected long offsetLag = 0L;
    // partition min data offset
    protected long dataMin = 0L;
    // partition max data offset
    protected long dataMax = 0L;
    // consume data offset
    protected long dataLag = -1L;

    public RecordItem(int partitionId, long offsetCfm) {
        this.partitionId = partitionId % TBaseConstants.META_STORE_INS_BASE;
        this.offsetCfm = offsetCfm;
        this.storeId = partitionId / TBaseConstants.META_STORE_INS_BASE;
    }

    public void addStoreInfo(long offsetMin, long offsetMax,
                             long dataMin, long dataMax) {
        this.offsetMin = offsetMin;
        this.offsetMax = offsetMax;
        this.dataMin = dataMin;
        this.dataMax = dataMax;
        if (offsetMax != TBaseConstants.META_VALUE_UNDEFINED
                && offsetCfm != TBaseConstants.META_VALUE_UNDEFINED) {
            offsetLag = offsetMax - offsetCfm;
        }
    }

    public int getStoreId() {
        return storeId;
    }
}
