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

import java.io.File;
import java.io.IOException;

import com.qcloud.cos.event.COSProgressPublisher;
import com.qcloud.cos.event.ProgressEventType;
import com.qcloud.cos.event.ProgressListenerChain;
import com.qcloud.cos.event.TransferStateChangeListener;
import com.qcloud.cos.exception.PauseException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.transfer.TransferManagerUtils;

public class DownloadImpl extends AbstractTransfer implements Download {
    private COSObject cosObject;

    /**
     * Information to resume if the download is paused.
     */
    private final PersistableDownload persistableDownload;

    public DownloadImpl(String description, TransferProgress transferProgress,
            ProgressListenerChain progressListenerChain, COSObject cosObject,
            TransferStateChangeListener listener,
            GetObjectRequest getObjectRequest, File file) {
        super(description, transferProgress, progressListenerChain, listener);
        this.cosObject = cosObject;
        this.persistableDownload = captureDownloadState(getObjectRequest, file);
        COSProgressPublisher.publishTransferPersistable(progressListenerChain,
                persistableDownload);
    }

    /**
     * Returns the ObjectMetadata for the object being downloaded.
     *
     * @return The ObjectMetadata for the object being downloaded.
     */
    public ObjectMetadata getObjectMetadata() {
        return cosObject.getObjectMetadata();
    }

    /**
     * The name of the bucket where the object is being downloaded from.
     *
     * @return The name of the bucket where the object is being downloaded from.
     */
    public String getBucketName() {
        return cosObject.getBucketName();
    }

    /**
     * The key under which this object was stored in Qcloud COS.
     *
     * @return The key under which this object was stored in Qcloud COS.
     */
    public String getKey() {
        return cosObject.getKey();
    }

    /**
     * Cancels this download.
     *
     * @throws IOException
     */
    public synchronized void abort() throws IOException {

        this.monitor.getFuture().cancel(true);

        if ( cosObject != null ) {
              cosObject.getObjectContent().abort();
        }
        setState(TransferState.Canceled);
    }

    /**
     * Cancels this download, but skip notifying the state change listeners.
     *
     * @throws IOException
     */
    public synchronized void abortWithoutNotifyingStateChangeListener() throws IOException {
        this.monitor.getFuture().cancel(true);
        this.state = TransferState.Canceled;
    }



    public COSObject getCosObject() {
        return cosObject;
    }

    public void setCosObject(COSObject cosObject) {
        this.cosObject = cosObject;
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

    /**
     * Returns the captured state of the download; or null if it should not be
     * captured (for security reason).
     */
    private PersistableDownload captureDownloadState(
            final GetObjectRequest getObjectRequest, final File file) {
        if (getObjectRequest.getSSECustomerKey() == null) {
            return new PersistableDownload(
                    getObjectRequest.getBucketName(),
                    getObjectRequest.getKey(), getObjectRequest.getVersionId(),
                    getObjectRequest.getRange(),
                    getObjectRequest.getResponseHeaders(),
                    file.getAbsolutePath());
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.qcloud.cos.transfer.Download#pause()
     */
    @Override
    public PersistableDownload pause() throws PauseException {
        boolean forceCancel = true;
        TransferState currentState = getState();
        this.monitor.getFuture().cancel(true);

        if (persistableDownload == null) {
            throw new PauseException(TransferManagerUtils.determinePauseStatus(
                    currentState, forceCancel));
        }
        return persistableDownload;
    }
}