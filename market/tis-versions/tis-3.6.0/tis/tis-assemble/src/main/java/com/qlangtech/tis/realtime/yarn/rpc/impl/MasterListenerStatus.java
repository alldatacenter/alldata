/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.realtime.yarn.rpc.impl;

import com.qlangtech.tis.realtime.transfer.IIncreaseCounter;
import com.qlangtech.tis.realtime.transfer.IOnsListenerStatus;
import com.qlangtech.tis.realtime.transfer.TableSingleDataIndexStatus;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月8日
 */
public class MasterListenerStatus implements IOnsListenerStatus {

    private final IncrStatusUmbilicalProtocolImpl incrStatusUmbilicalProtocol;

    private static final Logger logger = LoggerFactory.getLogger(MasterListenerStatus.class);

    private final String collectionName;

    public MasterListenerStatus(String collectionName, IncrStatusUmbilicalProtocolImpl incrStatusUmbilicalProtocol) {
        super();
        this.collectionName = collectionName;
        this.incrStatusUmbilicalProtocol = incrStatusUmbilicalProtocol;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    // startService(empty);
    @Override
    public long getSolrConsumeIncrease() {
        return 0;
    }

    @Override
    public void cleanLastAccumulator() {
    }

    @Override
    public String getCollectionName() {
        return this.collectionName;
    }

    @Override
    public String getTableUpdateCount() {
        return "0";
    }

    @Override
    public int getBufferQueueUsedSize() {
        return getCounter(new GetterStrategy() {

            @Override
            public int getInt(TableSingleDataIndexStatus dto) {
                return dto.getBufferQueueUsedSize();
            }
        });
    }

    @Override
    public int getBufferQueueRemainingCapacity() {
        return getCounter(new GetterStrategy() {

            @Override
            public int getInt(TableSingleDataIndexStatus dto) {
                return dto.getBufferQueueRemainingCapacity();
            }
        });
    }

    @Override
    public long getConsumeErrorCount() {
        return getCounter(new GetterStrategy() {

            @Override
            public int getInt(TableSingleDataIndexStatus dto) {
                return dto.getConsumeErrorCount();
            }
        });
    }

    protected int getCounter(GetterStrategy getterStrategy) {
        return 0;
        // TableSingleDataIndexStatus indexStatus = incrStatusUmbilicalProtocol
        // .getIndexUpdateCounterStatus(collectionName);
        // if (indexStatus == null) {
        // return 0;
        // }
        // return getterStrategy.getInt(indexStatus);
    }

    private static interface GetterStrategy {

        int getInt(TableSingleDataIndexStatus dto);
    }

    @Override
    public long getConsumeIncreaseCount() {
        return 0;
    }

    @Override
    public long getIgnoreRowsCount() {
        return 0;
        // TableSingleDataIndexStatus indexStatus = incrStatusUmbilicalProtocol
        // .getIndexUpdateCounterStatus(collectionName);
        // if (indexStatus == null) {
        // return 0;
        // }
        // return indexStatus.getIgnoreRowsCount();
    }

    @Override
    public void resumeConsume() {
        incrStatusUmbilicalProtocol.resume(this.getCollectionName());
    }

    @Override
    public void pauseConsume() {
        incrStatusUmbilicalProtocol.stop(this.getCollectionName());
    }

    @Override
    public Set<Entry<String, IIncreaseCounter>> getUpdateStatic() {
        return null;
        // TableSingleDataIndexStatus indexStatus = incrStatusUmbilicalProtocol
        // .getIndexUpdateCounterStatus(collectionName);
        //
        // if (indexStatus == null) {
        // return Collections.emptySet();
        // }
        //
        // Map<String, IIncreaseCounter> result = new HashMap<String,
        // IIncreaseCounter>();
        //
        // for (Map.Entry<String, IncrCounter> entry : indexStatus.entrySet()) {
        // result.put(entry.getKey(), entry.getValue());
        // }
        //
        // return result.entrySet();
    }

    @Override
    public IIncreaseCounter getMetricCount(String metricName) {
        return null;
    }
}
