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
package com.qlangtech.tis.realtime.yarn.rpc;

import com.qlangtech.tis.realtime.transfer.TableSingleDataIndexStatus;
import org.apache.commons.lang.StringUtils;
import java.util.HashMap;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月7日
 */
public class UpdateCounterMap {

    private HashMap<String /** indexname*/, TableSingleDataIndexStatus>
    data = new HashMap<>();

    // 增量转发节点执行增量的数量
    private long gcCounter;

    // 从哪个地址发送过来的
    private String from;

    private long updateTime;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getGcCounter() {
        return gcCounter;
    }

    public void setGcCounter(long gcCounter) {
        this.gcCounter = gcCounter;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    // private static final long serialVersionUID = 1L;
    public boolean containIndex(String indexName) {
        return data.keySet().contains(indexName);
    }

    public boolean containsIndex(String indexName, String uuid) {
        TableSingleDataIndexStatus tableSingleDataIndexStatus = data.get(indexName);
        if (tableSingleDataIndexStatus == null) {
            return false;
        } else {
            return StringUtils.equals(tableSingleDataIndexStatus.getUUID(), uuid);
        }
    }

    // @Override
    // public void write(DataOutput out) throws IOException {
    // out.writeInt(data.size());
    // for (Map.Entry<String, TableSingleDataIndexStatus> /* indexname */
    // entry : data.entrySet()) {
    // TableSingleDataIndexStatus singleDataIndexStatus = entry.getValue();
    // WritableUtils.writeString(out, entry.getKey());
    // out.writeInt(singleDataIndexStatus.tableSize());
    // for (Map.Entry<String, Long> /* tableName */
    // tabUpdate : singleDataIndexStatus.getTableConsumeData().entrySet()) {
    // WritableUtils.writeString(out, tabUpdate.getKey());
    // out.writeLong(tabUpdate.getValue());
    // }
    // out.writeInt(singleDataIndexStatus.getBufferQueueRemainingCapacity());
    // out.writeInt(singleDataIndexStatus.getBufferQueueUsedSize());
    // out.writeInt(singleDataIndexStatus.getConsumeErrorCount());
    // out.writeInt(singleDataIndexStatus.getIgnoreRowsCount());
    // out.writeLong(singleDataIndexStatus.getTis30sAvgRT());
    // WritableUtils.writeString(out, singleDataIndexStatus.getUUID());
    // }
    // out.writeLong(gcCounter);
    // WritableUtils.writeString(out, from);
    // out.writeLong(updateTime);
    // }
    // @Override
    // public void readFields(DataInput in) throws IOException {
    // int indexSize = in.readInt();
    // int tabsCount;
    // for (int i = 0; i < indexSize; i++) {
    // String indexName = WritableUtils.readString(in);
    // tabsCount = in.readInt();
    // TableSingleDataIndexStatus tableUpdateCounter = new TableSingleDataIndexStatus();
    // for (int j = 0; j < tabsCount; j++) {
    // // tableUpdateCounter.put(WritableUtils.readString(in), new IncrCounter(in.readInt()));
    // tableUpdateCounter.put(WritableUtils.readString(in), in.readLong());
    // }
    // tableUpdateCounter.setBufferQueueRemainingCapacity(in.readInt());
    // tableUpdateCounter.setBufferQueueUsedSize(in.readInt());
    // tableUpdateCounter.setConsumeErrorCount(in.readInt());
    // tableUpdateCounter.setIgnoreRowsCount(in.readInt());
    // tableUpdateCounter.setTis30sAvgRT(in.readLong());
    // tableUpdateCounter.setUUID(WritableUtils.readString(in));
    // data.put(indexName, tableUpdateCounter);
    // }
    // this.gcCounter = in.readLong();
    // this.from = WritableUtils.readString(in);
    // this.updateTime = in.readLong();
    // }
    public void addTableCounter(String indexName, TableSingleDataIndexStatus tableUpdateCounter) {
        this.data.put(indexName, tableUpdateCounter);
    }

    public HashMap<String, TableSingleDataIndexStatus> getData() {
        return this.data;
    }

    public static void main(String[] args) throws Exception {
    // InetSocketAddress address = new InetSocketAddress("0.0.0.0", 1234);
    // IncrStatusUmbilicalProtocol incrStatusUmbilicalProtocol = RPC.getProxy(IncrStatusUmbilicalProtocol.class, IncrStatusUmbilicalProtocol.versionID, address, new Configuration());
    // UpdateCounterMap updateCounterMap = new UpdateCounterMap();
    // TableSingleDataIndexStatus indexStatus = new TableSingleDataIndexStatus();
    // updateCounterMap.addTableCounter("index1", indexStatus);
    // indexStatus.setUUID("uuid");
    // indexStatus.setConsumeErrorCount(1);
    // indexStatus.setBufferQueueRemainingCapacity(1);
    // indexStatus.setBufferQueueUsedSize(1);
    // indexStatus.setIgnoreRowsCount(1);
    // indexStatus.put("tab1", 10L);
    // MasterJob masterJob = incrStatusUmbilicalProtocol.reportStatus(updateCounterMap);
    // System.out.println(masterJob);
    }
    // public static class TableSingleDataIndexStatus extends HashMap<String/* tableName */, ConsumeDataKeeper> {
    // private static final long serialVersionUID = 1L;
    //
    // private int bufferQueueRemainingCapacity;
    // private int bufferQueueUsedSize;
    // private int consumeErrorCount;
    // private int ignoreRowsCount;
    // private UUID uuid;
    // private long createTime;
    //
    // private HashMap<String /*tableName*/, LinkedList<ConsumeDataKeeper>> partitionTableDataKeeper;
    //
    // public int getBufferQueueUsedSize() {
    // return bufferQueueUsedSize;
    // }
    //
    // public void setBufferQueueUsedSize(int bufferQueueUsedSize) {
    // this.bufferQueueUsedSize = bufferQueueUsedSize;
    // }
    //
    // public int getBufferQueueRemainingCapacity() {
    // return bufferQueueRemainingCapacity;
    // }
    //
    // public void setBufferQueueRemainingCapacity(int bufferQueueRemainingCapacity) {
    // this.bufferQueueRemainingCapacity = bufferQueueRemainingCapacity;
    // }
    //
    // public int getConsumeErrorCount() {
    // return consumeErrorCount;
    // }
    //
    // public void setConsumeErrorCount(int consumeErrorCount) {
    // this.consumeErrorCount = consumeErrorCount;
    // }
    //
    // public int getIgnoreRowsCount() {
    // return ignoreRowsCount;
    // }
    //
    // public void setIgnoreRowsCount(int ignoreRowsCount) {
    // this.ignoreRowsCount = ignoreRowsCount;
    // }
    //
    // public UUID getUUID() {
    // return uuid;
    // }
    //
    // public void setUUID(UUID uuid) {
    // this.uuid = uuid;
    // }
    //
    // public long getCreateTime() {
    // return createTime;
    // }
    //
    // public void setCreateTime(long createTime) {
    // this.createTime = createTime;
    // }
    //
    // public HashMap<String, LinkedList<ConsumeDataKeeper>> getPartitionTableDataKeeper() {
    // return partitionTableDataKeeper;
    // }
    //
    // public void setPartitionTableDataKeeper(HashMap<String, LinkedList<ConsumeDataKeeper>> partitionTableDataKeeper) {
    // this.partitionTableDataKeeper = partitionTableDataKeeper;
    // }
    // }
}
