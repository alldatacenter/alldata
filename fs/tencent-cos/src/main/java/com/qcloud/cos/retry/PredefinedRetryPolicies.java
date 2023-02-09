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

import com.qcloud.cos.http.CosHttpRequest;
import com.qcloud.cos.internal.CosServiceRequest;
import java.io.IOException;
import org.apache.http.HttpResponse;


public class PredefinedRetryPolicies {

    /**
     * No retry policy
     **/
    public static final RetryPolicy NO_RETRY_POLICY = new RetryPolicy() {
        @Override
        public <X extends CosServiceRequest> boolean shouldRetry(CosHttpRequest<X> request,
                HttpResponse response,
                Exception exception,
                int retryIndex) {
            return false;
        }
    };

    /**
     * SDK default retry policy
     */
    public static final RetryPolicy DEFAULT;

    static {
        DEFAULT = getDefaultRetryPolicy();
    }

    public static class SdkDefaultRetryPolicy extends RetryPolicy {

        @Override
        public <X extends CosServiceRequest> boolean shouldRetry(CosHttpRequest<X> request,
                HttpResponse response,
                Exception exception,
                int retryIndex) {
            if (RetryUtils.isRetryableServiceException(exception)) {
                return true;
            }
            // Always retry on client exceptions caused by IOException
            if (exception.getCause() instanceof IOException) {
                return true;
            }
            return false;
        }
    }

    /**
     * Returns the SDK default retry policy. This policy will honor the
     * maxErrorRetry set in ClientConfiguration.
     *
     * @see com.qcloud.cos.ClientConfig#setMaxErrorRetry(int)
     */
    public static RetryPolicy getDefaultRetryPolicy() {
        return new SdkDefaultRetryPolicy();
    }

}
