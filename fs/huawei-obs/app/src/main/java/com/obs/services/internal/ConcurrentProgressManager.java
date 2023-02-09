/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal;

import com.obs.services.model.ProgressListener;
import com.obs.services.model.ProgressStatus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentProgressManager extends ProgressManager {

    private AtomicBoolean startFlag = new AtomicBoolean(false);
    protected AtomicLong transferredBytes;
    protected AtomicLong newlyTransferredBytes;
    protected AtomicLong lastSecondBytes;
    protected AtomicLong currentSecondBytes;
    protected AtomicLong lastSwapTimeStamp;

    public ConcurrentProgressManager(long totalBytes, long transferredBytes, ProgressListener progressListener,
                                     long intervalBytes) {
        super(totalBytes, progressListener, intervalBytes);
        this.transferredBytes = transferredBytes < 0 ? new AtomicLong(0) : new AtomicLong(transferredBytes);
        this.newlyTransferredBytes = new AtomicLong(0);
        this.lastSecondBytes = new AtomicLong(-1);
        this.currentSecondBytes = new AtomicLong(0);
        this.lastSwapTimeStamp = new AtomicLong(System.currentTimeMillis());
    }

    public void progressStart() {
        if (startFlag.compareAndSet(false, true)) {
            super.progressStart();
        }
    }

    public void progressEnd() {
        if (this.progressListener == null) {
            return;
        }
        synchronized (this) {
            long now = System.currentTimeMillis();
            ProgressStatus status = new DefaultProgressStatus(this.newlyTransferredBytes.get(),
                    this.transferredBytes.get(), this.totalBytes, now - this.lastCheckpoint,
                    now - this.startCheckpoint);
            this.progressListener.progressChanged(status);
        }
    }

    @Override
    protected void doProgressChanged(int bytes) {
        long transferred = this.transferredBytes.addAndGet(bytes);
        long newlyTransferred = this.newlyTransferredBytes.addAndGet(bytes);
        // 获取当前时间戳，减去上次更新速度的时间，如若时间差大于 1000ms，则对上一秒传输字节数进行更新
        long now = System.currentTimeMillis();
        // 采用局部变量保证线程安全
        long swapIntervalTime = now - lastSwapTimeStamp.get();
        currentSecondBytes.addAndGet(bytes);
        // 上一秒传输字节设置为 当前传输字节 / 耗时，更新当前传输字节为 0，重设更新时间
        if (swapIntervalTime > 1000) {
            lastSecondBytes.set((long) (currentSecondBytes.get() / (swapIntervalTime / 1000.0)));
            currentSecondBytes.set(0);
            lastSwapTimeStamp.set(now);
        }
        // 当新传输字节数大于用户设置阈值时更新回调函数，其中瞬时速度使用上一秒传输字节数作为近似值
        if (newlyTransferred >= this.intervalBytes
                && (transferred < this.totalBytes || this.totalBytes == -1)) {
            if (this.newlyTransferredBytes.compareAndSet(newlyTransferred, -newlyTransferred)) {
                DefaultProgressStatus status = new DefaultProgressStatus(newlyTransferred, transferred,
                        this.totalBytes, now - this.lastCheckpoint, now - this.startCheckpoint);
                status.setInstantaneousSpeed(lastSecondBytes.get());
                this.progressListener.progressChanged(status);
                this.lastCheckpoint = now;
            }
        }
    }

}
