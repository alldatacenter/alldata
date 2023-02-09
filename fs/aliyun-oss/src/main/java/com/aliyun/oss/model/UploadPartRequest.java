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

import java.io.InputStream;

import com.aliyun.oss.common.comm.io.BoundedInputStream;

/**
 * This is the request class to upload a file part in a multipart upload.
 */
public class UploadPartRequest extends GenericRequest {

    private String uploadId;

    private int partNumber;

    private long partSize = -1;

    private String md5Digest;

    private InputStream inputStream;

    private boolean useChunkEncoding = false;

    // Traffic limit speed, its uint is bit/s
    private int trafficLimit;

    public UploadPartRequest() {
    }

    public UploadPartRequest(String bucketName, String key) {
        super(bucketName, key);
    }

    public UploadPartRequest(String bucketName, String key, String uploadId, int partNumber, InputStream inputStream,
            long partSize) {
        super(bucketName, key);
        this.uploadId = uploadId;
        this.partNumber = partNumber;
        this.inputStream = inputStream;
        this.partSize = partSize;
    }

    /**
     * Sets the data stream for the part.
     * 
     * @param inputStream
     *            The part's data stream.
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Gets the data stream for the part.
     * 
     * @return The part's data stream.
     */
    public InputStream getInputStream() {
        return inputStream;
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
     * Gets the part number. For every part to upload, it has a number whose
     * range comes from 1 to 10000. For the given upload Id, the part number is
     * unique and uploading the data with an existing part number would lead to
     * the old data being overwritten.
     * 
     * @return Part number.
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Sets the part number. For every part to upload, it has a number whose
     * range comes from 1 to 10000. For the given upload Id, the part number is
     * unique and uploading the data with an existing part number would lead to
     * the old data being overwritten.
     * 
     * @param partNumber
     *            Part number.
     */
    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Gets the part size. The minimal part size is 100KB except the last part.
     * Note: the UploadPartCopyRequest's minimal part size is 5MB.
     * <p>
     * If the part size is -1, then it means the size is unknown. And it will
     * use chunked transfer encoding to upload the part's data.
     * </p>
     * 
     * @return Part size.
     *
     */
    public long getPartSize() {
        return this.partSize;
    }

    /**
     * Sets the part size. The minimal part size is 100KB except the last part.
     * Note: the UploadPartCopyRequest's minimal part size is 5MB.
     * <p>
     * If the part size is -1, then it means the size is unknown. And it will
     * use chunked transfer encoding to upload the part's data.
     * </p>
     * 
     * @param partSize
     *            Part size.
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    /**
     * Gets the part's MD5 value.
     * 
     * @return Part's MD5 value.
     */
    public String getMd5Digest() {
        return md5Digest;
    }

    /**
     * Sets the part's MD5 value.
     * 
     * @param md5Digest
     *            Part's MD5 value.
     */
    public void setMd5Digest(String md5Digest) {
        this.md5Digest = md5Digest;
    }

    /**
     * Gets the flag of using chunked transfer encoding.
     * 
     * @return true:using chunked transfer encoding
     */
    public boolean isUseChunkEncoding() {
        return useChunkEncoding || (this.partSize == -1);
    }

    /**
     * Sets the flag of using chunked transfer encoding.
     * 
     * @param useChunkEncoding
     *            true:using chunked transfer encoding
     */
    public void setUseChunkEncoding(boolean useChunkEncoding) {
        this.useChunkEncoding = useChunkEncoding;
    }

    public BoundedInputStream buildPartialStream() {
        return new BoundedInputStream(inputStream, (int) partSize);
    }

    /**
     * Sets traffic limit speed, its unit is bit/s
     *
     * @param trafficLimit
     *           the limit speed
     */
    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }

    /**
     * Gets traffic limit speed, its unit is bit/s
     * @return traffic limit speed
     */
    public int getTrafficLimit() {
        return trafficLimit;
    }
}
