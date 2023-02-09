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

import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.event.TransferStateChangeListener;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;

import java.io.IOException;

public class ImageAuditingImpl extends AbstractTransfer implements CIPostJob {

    private ImageAuditingRequest request;
    private ImageAuditingResponse response;
    private String errMsg;

    public ImageAuditingImpl(String description, TransferProgress transferProgress,
                             ProgressListenerChain progressListenerChain,
                             TransferStateChangeListener listener,
                             ImageAuditingRequest imageRequest) {
        super(description, transferProgress, progressListenerChain, listener);
        this.request = imageRequest;
//        COSProgressPublisher.publishTransferPersistable(progressListenerChain,
//                persistableDownload);
    }

    /**
     * Cancels this job.
     *
     * @throws IOException
     */
    public synchronized void abort() throws IOException {
        this.monitor.getFuture().cancel(true);
        setState(TransferState.Canceled);
    }

    /**
     * This method is also responsible for firing COMPLETED signal to the
     * listeners.
     */
    @Override
    public void setState(TransferState state) {
        super.setState(state);

        if (state == TransferState.Completed) {
            fireProgressEvent(ProgressEventType.TRANSFER_COMPLETED_EVENT);
        }
    }

    public ImageAuditingRequest getRequest() {
        return request;
    }

    public void setRequest(ImageAuditingRequest request) {
        this.request = request;
    }

    public ImageAuditingResponse getResponse() {
        return response;
    }

    public void setResponse(ImageAuditingResponse response) {
        this.response = response;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}