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

package org.apache.inlong.tubemq.server.common.utils;

/**
 * reference :https://github.com/twitter/snowflake/blob/master/src/main/scala/com/twitter/service/snowflake/IdWorker.scala
 */
public class IdWorker {
    private static final long twepoch = 1350282310830L;
    private static final long workerIdBits = 10L;
    private static final long maxWorkerId = -1L ^ -1L << workerIdBits;
    private static final long sequenceBits = 12L;
    private static final long workerIdShift = sequenceBits;
    private static final long timestampLeftShift = sequenceBits + workerIdBits;
    private static final long sequenceMask = -1L ^ -1L << sequenceBits;
    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public IdWorker(final long workerId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format(
                    "worker Id required in range [%d , 0]", maxWorkerId));
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.nanoTime();
        if (this.lastTimestamp == timestamp) {
            this.sequence = this.sequence + 1 & sequenceMask;
            if (this.sequence == 0) {
                timestamp = this.tillNextMillis(this.lastTimestamp);
            }
        } else {
            this.sequence = 0;
        }
        this.lastTimestamp = timestamp;
        return Math.abs(timestamp - twepoch << timestampLeftShift | this.workerId << workerIdShift | this.sequence);
    }

    private long tillNextMillis(final long lastTimestamp) {
        long timestamp = System.nanoTime();
        while (timestamp <= lastTimestamp) {
            timestamp = System.nanoTime();
        }
        return timestamp;
    }

}
