/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.comm;

public abstract class RetryStrategy {

    private static final int DEFAULT_RETRY_PAUSE_SCALE = 300; // milliseconds.

    public abstract boolean shouldRetry(Exception ex, RequestMessage request, ResponseMessage response, int retries);

    public long getPauseDelay(int retries) {
        // make the pause time increase exponentially
        // based on an assumption that the more times it retries,
        // the less probability it succeeds.
        int scale = DEFAULT_RETRY_PAUSE_SCALE;
        long delay = (long) Math.pow(2, retries) * scale;

        return delay;
    }
}
