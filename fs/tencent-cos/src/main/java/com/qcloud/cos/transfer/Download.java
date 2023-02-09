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

import java.io.IOException;

import com.qcloud.cos.exception.PauseException;
import com.qcloud.cos.model.ObjectMetadata;

/**
 * Represents an asynchronous download from Qcloud COS.
 * <p>
 * See {@link TransferManager} for more information about creating transfers.
 * </p>
 * 
 * @see TransferManager#download(com.qcloud.cos.model.GetObjectRequest,
 *      java.io.File)
 */
public interface Download extends Transfer {

    /**
     * Returns the ObjectMetadata for the object being downloaded.
     *
     * @return The ObjectMetadata for the object being downloaded.
     */
    public ObjectMetadata getObjectMetadata();

    /**
     * The name of the bucket where the object is being downloaded from.
     *
     * @return The name of the bucket where the object is being downloaded from.
     */
    public String getBucketName();

    /**
     * The key under which this object was stored in Qcloud COS.
     *
     * @return The key under which this object was stored in Qcloud COS.
     */
    public String getKey();

    /**
     * Cancels this download.
     *
     * @throws IOException
     */
    public void abort() throws IOException;

    /**
     * Pause the current download operation and returns the information that can
     * be used to resume the download at a later time.
     *
     * Resuming a download would not perform ETag check as range get is
     * performed for downloading the object's remaining contents.
     *
     * Resuming a download for an object encrypted using
     * {@link CryptoMode#StrictAuthenticatedEncryption} would result in
     * CosClientException as authenticity cannot be guaranteed for a range
     * get operation.
     *
     * @throws PauseException
     *             If any errors were encountered while trying to pause the
     *             download.
     */
    public PersistableDownload pause() throws PauseException;
}