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

import java.util.List;

import com.obs.services.internal.ProgressManager.BytesUnit;
import com.obs.services.model.ProgressStatus;

public class DefaultProgressStatus implements ProgressStatus {

    private final long newlyTransferredBytes;
    private final long transferredBytes;
    private final long totalBytes;
    private final long intervalMilliseconds;
    private final long totalMilliseconds;
    private long instantaneousSpeed;
    private List<BytesUnit> instantaneousBytes;

    public DefaultProgressStatus(long newlyTransferredBytes, long transferredBytes, long totalBytes,
                                 long intervalMilliseconds, long totalMilliseconds) {
        this.newlyTransferredBytes = newlyTransferredBytes;
        this.transferredBytes = transferredBytes;
        this.totalBytes = totalBytes;
        this.intervalMilliseconds = intervalMilliseconds;
        this.totalMilliseconds = totalMilliseconds;
    }

    @Override
    public double getInstantaneousSpeed() {
        if (this.intervalMilliseconds <= 0) {
            return -1d;
        }
        return this.instantaneousSpeed;
    }

    @Deprecated
    public double getOldInstantaneousSpeed() {
        if (this.instantaneousBytes != null) {
            long oldInstantaneousSpeed = 0;
            for (BytesUnit item : this.instantaneousBytes) {
                oldInstantaneousSpeed += item.bytes;
            }
            return oldInstantaneousSpeed;
        }

        if (this.intervalMilliseconds <= 0) {
            return -1d;
        }
        return this.newlyTransferredBytes * 1000.0d / this.intervalMilliseconds;
    }

    @Override
    public double getAverageSpeed() {
        if (this.totalMilliseconds <= 0) {
            return -1d;
        }
        return this.transferredBytes * 1000.0d / this.totalMilliseconds;
    }

    @Override
    public int getTransferPercentage() {
        if (this.totalBytes < 0) {
            return -1;
        } else if (this.totalBytes == 0) {
            return 100;
        }
        return (int) (this.transferredBytes * 100 / this.totalBytes);
    }

    @Override
    public long getNewlyTransferredBytes() {
        return this.newlyTransferredBytes;
    }

    @Override
    public long getTransferredBytes() {
        return this.transferredBytes;
    }

    @Override
    public long getTotalBytes() {
        return this.totalBytes;
    }

    @Deprecated
    public void setInstantaneousBytes(List<BytesUnit> instantaneousBytes) {
        this.instantaneousBytes = instantaneousBytes;
    }

    public void setInstantaneousSpeed(long instantaneousSpeed) {
        this.instantaneousSpeed = instantaneousSpeed;
    }

}
