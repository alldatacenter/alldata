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

package org.apache.inlong.tubemq.server.broker.offset;

import java.util.HashMap;
import java.util.Map;

/**
 * The offset snapshot of the consumer group on the partition.
 */
public class OffsetCsmRecord {

    protected int storeId;
    // store min index offset
    protected long offsetMin = 0L;
    // store max index offset
    protected long offsetMax = 0L;
    // store min data offset
    protected long dataMin = 0L;
    // store max data offset
    protected long dataMax = 0L;
    // partition consume record
    protected final Map<Integer, OffsetCsmItem> partitionCsmMap = new HashMap<>();

    public OffsetCsmRecord(int storeId) {
        this.storeId = storeId;
    }

    public void addOffsetCfmInfo(int partitionId, long offsetCfm) {
        OffsetCsmItem offsetCsmItem = partitionCsmMap.get(partitionId);
        if (offsetCsmItem == null) {
            OffsetCsmItem tmpItem = new OffsetCsmItem(partitionId);
            offsetCsmItem = partitionCsmMap.putIfAbsent(partitionId, tmpItem);
            if (offsetCsmItem == null) {
                offsetCsmItem = tmpItem;
            }
        }
        offsetCsmItem.addCfmOffset(offsetCfm);
    }

    public void addOffsetFetchInfo(int partitionId, long offsetFetch) {
        OffsetCsmItem offsetCsmItem = partitionCsmMap.get(partitionId);
        if (offsetCsmItem == null) {
            OffsetCsmItem tmpItem = new OffsetCsmItem(partitionId);
            offsetCsmItem = partitionCsmMap.putIfAbsent(partitionId, tmpItem);
            if (offsetCsmItem == null) {
                offsetCsmItem = tmpItem;
            }
        }
        offsetCsmItem.addFetchOffset(offsetFetch);
    }

    public void addStoreInfo(long offsetMin, long offsetMax,
            long dataMin, long dataMax) {
        this.offsetMin = offsetMin;
        this.offsetMax = offsetMax;
        this.dataMin = dataMin;
        this.dataMax = dataMax;
    }

    public int getStoreId() {
        return storeId;
    }
}
