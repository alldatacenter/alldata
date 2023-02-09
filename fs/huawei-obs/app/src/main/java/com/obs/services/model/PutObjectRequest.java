/**
* Copyright 2019 Huawei Technologies Co.,Ltd.
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use
* this file except in compliance with the License.  You may obtain a copy of the
* License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software distributed
* under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations under the License.
**/

package com.obs.services.model;

import java.io.File;
import java.io.InputStream;

import com.obs.services.internal.ObsConstraint;

/**
 * Parameters in an object upload request
 */
public class PutObjectRequest extends PutObjectBasicRequest {
    protected File file;

    protected InputStream input;

    protected ObjectMetadata metadata;

    protected int expires = -1;

    protected long offset;

    private boolean autoClose = true;

    private ProgressListener progressListener;

    private long progressInterval = ObsConstraint.DEFAULT_PROGRESS_INTERVAL;

    public PutObjectRequest() {
    }

    public PutObjectRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public PutObjectRequest(PutObjectBasicRequest request) {
        if (request != null) {
            this.bucketName = request.getBucketName();
            this.objectKey = request.getObjectKey();
            this.acl = request.getAcl();
            this.extensionPermissionMap = request.getExtensionPermissionMap();
            this.sseCHeader = request.getSseCHeader();
            this.sseKmsHeader = request.getSseKmsHeader();
            this.successRedirectLocation = request.getSuccessRedirectLocation();
            this.setRequesterPays(request.isRequesterPays());
        }
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public PutObjectRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param file
     *            File to be uploaded
     */
    public PutObjectRequest(String bucketName, String objectKey, File file) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.file = file;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param input
     *            Data stream to be uploaded
     */
    public PutObjectRequest(String bucketName, String objectKey, InputStream input) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.input = input;
    }

    /**
     * Obtain the start position of the to-be-uploaded content in the file. This
     * parameter is effective only when the path where the file is to be
     * uploaded is configured.
     * 
     * @return Start position of the content to be uploaded in the local file
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Set the start position of the to-be-uploaded content in the file. This
     * parameter is effective only when the path where the file is to be
     * uploaded is configured. The unit is byte and the default value is 0.
     * 
     * @param offset
     *            Start position of the content to be uploaded in the local file
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Obtain the data stream to be uploaded, which cannot be used with the file
     * to be uploaded.
     * @return Data stream to be uploaded
     */
    public InputStream getInput() {
        return input;
    }

    /**
     * Set the data stream to be uploaded, which cannot be used with the file to
     * be uploaded.
     * 
     * @param input
     *            Data stream to be uploaded
     * 
     */
    public void setInput(InputStream input) {
        this.input = input;
        this.file = null;
    }

    /**
     * Obtain object properties, including "content-type", "content-length",
     * "content-md5", and customized metadata.
     * 
     * @return Object properties
     */
    public ObjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * Set the object properties, including "content-type", "content-length",
     * and customized metadata.
     * 
     * @param metadata
     *            Object properties
     */
    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Obtain the file to be uploaded, which cannot be used with the data
     * stream.
     * 
     * @return File to be uploaded
     */
    public File getFile() {
        return file;
    }

    /**
     * Set the file to be uploaded, which cannot be used with the data stream.
     * 
     * @param file
     *            File to be uploaded
     */
    public void setFile(File file) {
        this.file = file;
        this.input = null;
    }

    /**
     * Obtain the expiration time of the object.
     * 
     * @return Expiration time of the object
     */
    public int getExpires() {
        return expires;
    }

    /**
     * Set the expiration time of the object. The value must be a positive
     * integer.
     * 
     * @param expires
     *            Expiration time of the object
     */
    public void setExpires(int expires) {
        this.expires = expires;
    }

    /**
     * Check whether the input stream will be automatically closed. The default
     * value is "true".
     * 
     * @return Identifier specifying whether the input stream will be
     *         automatically closed
     */
    public boolean isAutoClose() {
        return autoClose;
    }

    /**
     * Specify whether to automatically close the input stream. The default
     * value is "true".
     * 
     * @param autoClose
     *            Identifier specifying whether the input stream will be
     *            automatically closed
     */
    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    /**
     * Obtain the data transfer listener.
     * 
     * @return Data transfer listener
     */
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    /**
     * Set the data transfer listener.
     * 
     * @param progressListener
     *            Data transfer listener
     */
    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    /**
     * Obtain the callback threshold of the data transfer listener. The default
     * value is 100 KB.
     * 
     * @return Callback threshold of the data transfer listener
     */
    public long getProgressInterval() {
        return progressInterval;
    }

    /**
     * Set the callback threshold of the data transfer listener. The default
     * value is 100 KB.
     * 
     * @param progressInterval
     *            Callback threshold of the data transfer listener
     */
    public void setProgressInterval(long progressInterval) {
        this.progressInterval = progressInterval;
    }

    @Override
    public String toString() {
        return "PutObjectRequest [file=" + file + ", input=" + input + ", metadata=" + metadata
                + ", isEncodeHeaders=" + encodeHeaders + ", expires=" + expires
                + ", offset=" + offset + ", autoClose=" + autoClose + ", progressListener=" + progressListener
                + ", progressInterval=" + progressInterval + ", getBucketName()=" + getBucketName()
                + ", getObjectKey()=" + getObjectKey() + ", getSseKmsHeader()=" + getSseKmsHeader()
                + ", getSseCHeader()=" + getSseCHeader() + ", getAcl()=" + getAcl() + ", getSuccessRedirectLocation()="
                + getSuccessRedirectLocation() + ", getAllGrantPermissions()=" + getAllGrantPermissions()
                + ", getExtensionPermissionMap()=" + getExtensionPermissionMap() + ", isRequesterPays()="
                + isRequesterPays() + "]";
    }

}
