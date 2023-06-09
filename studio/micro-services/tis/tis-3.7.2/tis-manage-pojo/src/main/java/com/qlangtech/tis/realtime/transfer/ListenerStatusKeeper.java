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
package com.qlangtech.tis.realtime.transfer;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class ListenerStatusKeeper {

    private int bufferQueueRemainingCapacity;

    private int bufferQueueUsedSize;

    private int consumeErrorCount;

    private int ignoreRowsCount;

    private String uuid;

    // 增量任务是否暂停
    private boolean incrProcessPaused;

    private long tis30sAvgRT;

    public boolean isIncrProcessPaused() {
        return incrProcessPaused;
    }

    public void setIncrProcessPaused(boolean incrProcessPaused) {
        this.incrProcessPaused = incrProcessPaused;
    }

    public int getBufferQueueRemainingCapacity() {
        return bufferQueueRemainingCapacity;
    }

    public void setBufferQueueRemainingCapacity(int bufferQueueRemainingCapacity) {
        this.bufferQueueRemainingCapacity = bufferQueueRemainingCapacity;
    }

    public int getBufferQueueUsedSize() {
        return bufferQueueUsedSize;
    }

    public void setBufferQueueUsedSize(int bufferQueueUsedSize) {
        this.bufferQueueUsedSize = bufferQueueUsedSize;
    }

    public int getConsumeErrorCount() {
        return consumeErrorCount;
    }

    public void setConsumeErrorCount(int consumeErrorCount) {
        this.consumeErrorCount = consumeErrorCount;
    }

    public int getIgnoreRowsCount() {
        return ignoreRowsCount;
    }

    public void setIgnoreRowsCount(int ignoreRowsCount) {
        this.ignoreRowsCount = ignoreRowsCount;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public long getTis30sAvgRT() {
        return tis30sAvgRT;
    }

    public void setTis30sAvgRT(long tis30sAvgRT) {
        this.tis30sAvgRT = tis30sAvgRT;
    }
}
