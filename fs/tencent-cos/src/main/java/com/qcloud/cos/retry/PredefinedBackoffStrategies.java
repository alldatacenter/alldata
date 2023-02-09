/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */

package com.qcloud.cos.retry;

import com.qcloud.cos.utils.ValidationUtils;


public class PredefinedBackoffStrategies {

    /**
     * Default base sleep time (milliseconds) for non-throttled exceptions.
     **/
    private static final int SDK_DEFAULT_BASE_DELAY = 100;

    /**
     * Default maximum back-off time before retrying a request
     */
    static final int SDK_DEFAULT_MAX_BACKOFF_IN_MILLISECONDS = 20 * 1000;

    /**
     * Maximum retry limit. Avoids integer overflow issues.
     *
     * NOTE: If the value is greater than 30, there can be integer overflow
     * issues during delay calculation.
     **/
    private static final int MAX_RETRIES = 30;
    /**
     * SDK default retry policy
     */
    public static final BackoffStrategy DEFAULT;

    static {
        DEFAULT = new ExponentialBackoffStrategy(SDK_DEFAULT_BASE_DELAY, SDK_DEFAULT_MAX_BACKOFF_IN_MILLISECONDS);
    }

    private static int calculateExponentialDelay(int retriesAttempted, int baseDelay, int maxBackoffTime) {
        int retries = Math.min(retriesAttempted, MAX_RETRIES);
        return (int) Math.min((1L << retries) * baseDelay, maxBackoffTime);
    }

    public static class ExponentialBackoffStrategy implements BackoffStrategy {

        private final int baseDelay;
        private final int maxBackoffTime;

        public ExponentialBackoffStrategy(final int baseDelay,
                final int maxBackoffTime) {
            this.baseDelay = ValidationUtils.assertIsPositive(baseDelay, "Base delay");
            this.maxBackoffTime = ValidationUtils.assertIsPositive(maxBackoffTime, "Max backoff");
        }

        @Override
        public long computeDelayBeforeNextRetry(int retryIndex) {
            return calculateExponentialDelay(retryIndex, baseDelay, maxBackoffTime);
        }
    }

}
