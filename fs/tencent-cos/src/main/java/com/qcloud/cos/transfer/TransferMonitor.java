/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


package com.qcloud.cos.transfer;

import java.util.concurrent.Future;

/**
 * Monitors long-running transfers.
 */
public interface TransferMonitor {

    /**
     * Returns a Future to wait on. Calling get() on this future will block
     * while the transfer progress is checked, but its return does not guarantee
     * the transfer is complete. Call isDone() to check for completion.  Repeated
     * calls to getFuture() can return different objects.
     */
    public Future<?> getFuture();

    /**
     * Returns whether the transfer is completed. A failure or cancellation
     * counts as completion as well; to gather any exceptions thrown, call
     * getFuture().get()
     */
    public boolean isDone();

}
