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

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;

import java.io.IOException;
import java.util.List;

/**
 * Multiple file download when downloading an entire virtual directory.
 */
public class MultipleImageAuditingImpl extends MultipleFileTransfer<ImageAuditingImpl>  {

    private List<ImageAuditingImpl> imageAuditingList;
    public MultipleImageAuditingImpl(String description, TransferProgress transferProgress,
                                     List<ImageAuditingImpl> imageAuditingList) {
        super(description, transferProgress, null, imageAuditingList);
        this.imageAuditingList = imageAuditingList;
    }

    /**
     * Waits for this transfer to complete. This is a blocking call; the current
     * thread is suspended until this transfer completes.
     *
     * @throws CosClientException   If any errors were encountered in the client while making the
     *                              request or handling the response.
     * @throws CosServiceException  If any errors occurred in Qcloud COS while processing the
     *                              request.
     * @throws InterruptedException If this thread is interrupted while waiting for the transfer
     *                              to complete.
     */
    @Override
    public void waitForCompletion()
            throws CosClientException, CosServiceException, InterruptedException {
        if (subTransfers.isEmpty())
            return;
        super.waitForCompletion();
    }

    /**
     * Aborts all outstanding downloads.
     */
    public void abort() throws IOException {
        for (ImageAuditingImpl subTransfer : subTransfers) {
            subTransfer.monitor.getFuture().cancel(true);
            subTransfer.setState(TransferState.Canceled);
        }
        setState(TransferState.Canceled);
    }

    public List<ImageAuditingImpl> getImageAuditingList() {
        return imageAuditingList;
    }

}
