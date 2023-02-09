/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.task;

import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.ProgressStatus;
import com.obs.services.model.UploadProgressStatus;

public class UploadTaskProgressStatus extends DefaultTaskProgressStatus implements UploadProgressStatus {

    private final long progressInterval;
    private final Date startDate;
    private ConcurrentHashMap<String, ProgressStatus> taskTable = new ConcurrentHashMap<String, ProgressStatus>();
    private AtomicLong totalSize = new AtomicLong();
    private AtomicLong totalMilliseconds = new AtomicLong();
    private AtomicLong endingTaskSize = new AtomicLong();
    private AtomicLong taskTagSize = new AtomicLong();

    public UploadTaskProgressStatus(long progressInterval, Date startDate) {
        this.progressInterval = progressInterval;
        this.startDate = ServiceUtils.cloneDateIgnoreNull(startDate);
    }

    public void setTaskTagSize(long taskTagSize) {
        this.taskTagSize = new AtomicLong(taskTagSize);
    }

    public long getTaskTagSize() {
        return taskTagSize.get();
    }

    public boolean isRefreshprogress() {
        if (this.progressInterval <= 0) {
            return false;
        }

        long transferredSize = this.getTransferredSize();
        long taskSize = this.getTaskTagSize();
        if (transferredSize - taskSize >= progressInterval) {
            this.setTaskTagSize(transferredSize);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long getTotalSize() {
        // 任务未加载完毕时，返回-1
        if (getTotalTaskNum() <= 0) {
            return -1L;
        } else {
            return totalSize.get();
        }
    }

    @Override
    public long getTransferredSize() {
        long transferredSize = this.endingTaskSize.get();
        ConcurrentHashMap<String, ProgressStatus> taskStatusTable = new ConcurrentHashMap<String, ProgressStatus>(
                this.taskTable);
        for (Entry<String, ProgressStatus> entry : taskStatusTable.entrySet()) {
            transferredSize += entry.getValue().getTransferredBytes();
        }
        return transferredSize;
    }

    @Override
    public double getInstantaneousSpeed() {
        if (this.taskTable != null) {
            long instantaneousSpeed = 0;
            for (Entry<String, ProgressStatus> entry : this.taskTable.entrySet()) {
                instantaneousSpeed += entry.getValue().getInstantaneousSpeed();
            }
            return instantaneousSpeed;
        } else {
            return -1d;
        }
    }

    @Override
    public double getAverageSpeed() {
        if (this.totalMilliseconds.get() <= 0) {
            return -1d;
        }
        return this.getTransferredSize() * 1000.0d / this.totalMilliseconds.get();
    }

    @Override
    public ConcurrentHashMap<String, ProgressStatus> getTaskTable() {
        ConcurrentHashMap<String, ProgressStatus> taskStatusTable = new ConcurrentHashMap<String, ProgressStatus>(
                this.taskTable);
        return taskStatusTable;
    }

    @Override
    public ProgressStatus getTaskStatus(String key) {
        return this.taskTable.get(key);
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = new AtomicLong(totalSize);
    }

    public void addTotalSize(long bytes) {
        this.totalSize.addAndGet(bytes);
    }

    public void setTaskTable(ConcurrentHashMap<String, ProgressStatus> taskTable) {
        this.taskTable = taskTable;
    }

    public void putTaskTable(String key, ProgressStatus status) {
        this.taskTable.put(key, status);
    }

    public void removeTaskTable(String key) {
        if (null == this.taskTable) {
            return;
        }
        this.taskTable.remove(key);
    }

    public long getTotalMilliseconds() {
        return totalMilliseconds.get();
    }

    public void setTotalMilliseconds(long totalMilliseconds) {
        this.totalMilliseconds = new AtomicLong(totalMilliseconds);
    }

    public long getEndingTaskSize() {
        return endingTaskSize.get();
    }

    public void setEndingTaskSize(long endingTaskSize) {
        this.endingTaskSize = new AtomicLong(endingTaskSize);
    }

    public void addEndingTaskSize(long bytes) {
        this.endingTaskSize.addAndGet(bytes);
    }

    public Date getStartDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.startDate);
    }

}
