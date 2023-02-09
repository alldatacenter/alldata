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

package com.aliyun.oss.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The request class that is used to complete a multipart upload. It wraps all
 * parameters needed to complete a multipart upload.
 *
 */
public class CompleteMultipartUploadRequest extends GenericRequest {

    /** The ID of the multipart upload to complete */
    private String uploadId;

    /**
     * The list of part numbers and ETags to use when completing the multipart
     * upload
     */
    private List<PartETag> partETags = new ArrayList<PartETag>();

    /** The access control list for multipart uploaded object */
    private CannedAccessControlList cannedACL;

    /** callback */
    private Callback callback;

    /** process **/
    private String process;

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Object key.
     * @param uploadId
     *            Mutlipart upload Id.
     * @param partETags
     *            The Etags for the parts.
     */
    public CompleteMultipartUploadRequest(String bucketName, String key, String uploadId, List<PartETag> partETags) {
        super(bucketName, key);
        this.uploadId = uploadId;
        this.partETags = partETags;
        setObjectACL(null);
    }

    /**
     * Gets the multipart upload Id.
     * 
     * @return The multipart upload Id.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Sets the multipart upload Id.
     * 
     * @param uploadId
     *            The multipart upload Id.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    /**
     * Gets the Etags of the parts.
     * 
     * @return A List of {@link PartETag}
     */
    public List<PartETag> getPartETags() {
        return partETags;
    }

    /**
     * Sets the ETags of the parts.
     * 
     * @param partETags
     *            A list of {@link PartETag}
     */
    public void setPartETags(List<PartETag> partETags) {
        this.partETags = partETags;
    }

    /**
     * Gets Object ACL。
     * 
     * @return Object ACL。
     */
    public CannedAccessControlList getObjectACL() {
        return cannedACL;
    }

    /**
     * Sets Object ACL。
     * 
     * @param cannedACL
     *            ACL。
     */
    public void setObjectACL(CannedAccessControlList cannedACL) {
        this.cannedACL = cannedACL;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }
}
