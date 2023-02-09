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


package com.qcloud.cos.model;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import com.qcloud.cos.internal.CosServiceRequest;

public class UploadPartRequest extends CosServiceRequest implements SSECustomerKeyProvider, CosDataSource, Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Additional information about the part being uploaded, such as
     * referrer.
     */
    private ObjectMetadata objectMetadata;
    /**
     * The name of the bucket containing the initiated multipart upload with
     * which this new part will be associated.
     */
    private String bucketName;

    /** The key of the initiated multipart upload */
    private String key;

    /**
     * The ID of an existing, initiated multipart upload, with which this new
     * part will be associated.
     */
    private String uploadId;

    /**
     * The part number describing this part's position relative to the other
     * parts in the multipart upload. Part number must be between 1 and 10,000
     * (inclusive).
     */
    private int partNumber;

    /** The size of this part, in bytes. */
    private long partSize;

    /**
     * The optional, but recommended, MD5 hash of the content of this part. If
     * specified, this value will be sent to Qcloud COS to verify the data
     * integrity when the data reaches Qcloud COS.
     */
    private String md5Digest;

    /**
     * traffic limit speed in second, the unit is bit/s
     */
    private int trafficLimit = 0;

    /**
     * The stream containing the data to upload for the new part. Exactly one
     * File or InputStream must be specified as the input to this operation.
     */
    private transient InputStream inputStream;

    /**
     * The file containing the data to upload. Exactly one File or InputStream
     * must be specified as the input to this operation.
     */
    private File file;

    /**
     * The optional offset in the specified file, at which to begin uploading
     * data for this part. If not specified, data will be read from the
     * beginning of the file.
     */
    private long fileOffset;

    /**
     * Allows the caller to indicate if this is the last part being uploaded in
     * a multipart upload.
     */
    private boolean isLastPart;
    
    /**
     * The optional customer-provided server-side encryption key to use to
     * encrypt the object part being uploaded.
     */
    private SSECustomerKey sseCustomerKey;


    /**
     * Sets the stream containing the data to upload for the new part.
     *
     * @param inputStream
     *            the stream containing the data to upload for the new part.
     */
    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Returns the stream containing the data to upload for the new part.
     *
     * @return the stream containing the data to upload for the new part.
     */
    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * Sets the stream containing the data to upload for the new part, and
     * returns this updated object so that additional method calls can be
     * chained together.
     *
     * @param inputStream
     *            the stream containing the data to upload for the new part.
     *
     * @return The updated UploadPartRequest object.
     */
    public UploadPartRequest withInputStream(InputStream inputStream) {
        setInputStream(inputStream);
        return this;
    }

    /**
     * Returns the name of the bucket containing the existing, initiated
     * multipart upload, with which this new part will be associated.
     *
     * @return the name of the bucket containing the existing, initiated
     *         multipart upload, with which this new part will be associated.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket containing the existing, initiated multipart
     * upload, with which this new part will be associated.
     *
     * @param bucketName
     *            the name of the bucket containing the existing, initiated
     *            multipart upload, with which this new part will be associated.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket containing the existing, initiated multipart
     * upload, with which this new part will be associated, and returns this
     * updated object so that additional method calls can be chained together.
     *
     * @param bucketName
     *            the name of the bucket containing the existing, initiated
     *            multipart upload, with which this new part will be associated.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    /**
     * Returns the key of the initiated multipart upload.
     *
     * @return the key of the initiated multipart upload.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key of the initiated multipart upload.
     *
     * @param key
     *            the key of the initiated multipart upload.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the key of the initiated multipart upload, and returns this updated
     * object so that additional method calls can be chained together.
     *
     * @param key
     *            the key of the initiated multipart upload.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Returns the ID of the existing, initiated multipart upload with which
     * this new part will be associated.
     *
     * @return the ID of the existing, initiated multipart upload with which
     *         this new part will be associated.
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Sets the ID of the existing, initiated multipart upload with which this
     * new part will be associated.
     *
     * @param uploadId
     *            the ID of the existing, initiated multipart upload with which
     *            this new part will be associated.
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    /**
     * Sets the ID of the existing, initiated multipart upload with which this
     * new part will be associated, and returns this updated UploadPartRequest
     * object so that additional method calls can be chained together.
     *
     * @param uploadId
     *            the ID of the existing, initiated multipart upload with which
     *            this new part will be associated.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withUploadId(String uploadId) {
        this.uploadId = uploadId;
        return this;
    }

    /**
     * Returns the part number describing this part's position relative to the
     * other parts in the multipart upload. Part number must be between 1 and
     * 10,000 (inclusive).
     *
     * @return the part number describing this part's position relative to the
     *         other parts in the multipart upload. Part number must be between
     *         1 and 10,000 (inclusive).
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Sets the part number describing this part's position relative to the
     * other parts in the multipart upload. Part number must be between 1 and
     * 10,000 (inclusive).
     *
     * @param partNumber
     *            the part number describing this part's position relative to
     *            the other parts in the multipart upload. Part number must be
     *            between 1 and 10,000 (inclusive).
     */
    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Sets the part number describing this part's position relative to the
     * other parts in the multipart upload. Part number must be between 1 and
     * 10,000 (inclusive).
     * <p>
     * Returns this updated UploadPartRequest object so that additional method
     * calls can be chained together.
     *
     * @param partNumber
     *            the part number describing this part's position relative to
     *            the other parts in the multipart upload. Part number must be
     *            between 1 and 10,000 (inclusive).
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withPartNumber(int partNumber) {
        this.partNumber = partNumber;
        return this;
    }

    /**
     * Returns the size of this part, in bytes.
     *
     * @return the size of this part, in bytes.
     */
    public long getPartSize() {
        return partSize;
    }

    /**
     * Sets the size of this part, in bytes.
     *
     * @param partSize
     *            the size of this part, in bytes.
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    /**
     * Sets the size of this part, in bytes, and returns this updated
     * UploadPartRequest object so that additional method calls can be chained
     * together.
     *
     * @param partSize
     *            the size of this part, in bytes.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withPartSize(long partSize) {
        this.partSize = partSize;
        return this;
    }

    /**
     * Sets the speed of this part upload, in bit/s, and returns this updated
     * UploadPartRequest object so that additional method calls can be chained
     * together.
     *
     * @param trafficLimit
     *            the speed of upload, in bit/s.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
        return this;
    }

    /**
     * Returns the optional, but recommended, MD5 hash of the content of this
     * part. If specified, this value will be sent to Qcloud COS to verify the
     * data integrity when the data reaches Qcloud COS.
     *
     * @return The optional, but recommended, MD5 hash of the content of this
     *         part. If specified, this value will be sent to Qcloud COS to
     *         verify the data integrity when the data reaches Qcloud COS.
     */
    public String getMd5Digest() {
        return md5Digest;
    }

    /**
     * Sets the optional, but recommended, MD5 hash of the content of this part.
     * If specified, this value will be sent to Qcloud COS to verify the data
     * integrity when the data reaches Qcloud COS.
     *
     * @param md5Digest
     *            The optional, but recommended, MD5 hash of the content of this
     *            part. If specified, this value will be sent to Qcloud COS to
     *            verify the data integrity when the data reaches Qcloud COS.
     */
    public void setMd5Digest(String md5Digest) {
        this.md5Digest = md5Digest;
    }

    /**
     * Sets the optional, but recommended, MD5 hash of the content of this part.
     * If specified, this value will be sent to Qcloud COS to verify the data
     * integrity when the data reaches Qcloud COS.
     * <p>
     * Returns this updated UploadPartRequest object so that additional method
     * calls can be chained together.
     *
     * @param md5Digest
     *            The optional, but recommended, MD5 hash of the content of this
     *            part. If specified, this value will be sent to Qcloud COS to
     *            verify the data integrity when the data reaches Qcloud COS.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withMD5Digest(String md5Digest) {
        this.md5Digest = md5Digest;
        return this;
    }

    /**
     * Returns the file containing the data to upload. Exactly one File or
     * InputStream must be specified as the input to this operation.
     *
     * @return The file containing the data to upload. Exactly one File or
     *         InputStream must be specified as the input to this operation.
     */
    @Override
    public File getFile() {
        return file;
    }

    /**
     * Sets the file containing the data to upload. Exactly one File or
     * InputStream must be specified as the input to this operation.
     *
     * @param file
     *            The file containing the data to upload. Exactly one File or
     *            InputStream must be specified as the input to this operation.
     */
    @Override
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Sets the file containing the data to upload, and returns this updated
     * UploadPartRequest object so that additional method calls can be chained
     * together.
     * <p>
     * Exactly one File or InputStream must be specified as the input to this
     * operation.
     *
     * @param file
     *            The file containing the data to upload. Exactly one File or
     *            InputStream must be specified as the input to this operation.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withFile(File file) {
        setFile(file);
        return this;
    }

    /**
     * Returns the optional offset in the specified file, at which to begin
     * uploading data for this part. If not specified, data will be read from
     * the beginning of the file.
     *
     * @return The optional offset in the specified file, at which to begin
     *         uploading data for this part. If not specified, data will be read
     *         from the beginning of the file.
     */
    public long getFileOffset() {
        return fileOffset;
    }

    /**
     * Sets the optional offset in the specified file, at which to begin
     * uploading data for this part. If not specified, data will be read from
     * the beginning of the file.
     *
     * @param fileOffset
     *            The optional offset in the specified file, at which to begin
     *            uploading data for this part. If not specified, data will be
     *            read from the beginning of the file.
     */
    public void setFileOffset(long fileOffset) {
        this.fileOffset = fileOffset;
    }

    /**
     * Sets the optional offset in the specified file, at which to begin
     * uploading data for this part, and returns this updated UploadPartRequest
     * object so that additional method calls can be chained together.
     * <p>
     * If not specified, data will be read from the beginning of the file.
     *
     * @param fileOffset
     *            The optional offset in the specified file, at which to begin
     *            uploading data for this part. If not specified, data will be
     *            read from the beginning of the file.
     *
     * @return This updated UploadPartRequest object.
     */
    public UploadPartRequest withFileOffset(long fileOffset) {
        setFileOffset(fileOffset);
        return this;
    }


    /**
     * Returns true if the creator of this request has indicated this part is
     * the last part being uploaded in a multipart upload.
     *
     * @return True if the creator of this request has indicated this part is
     *         the last part being uploaded in a multipart upload.
     */
    public boolean isLastPart() {
        return isLastPart;
    }

    /**
    * Marks this part as the last part being uploaded in a multipart upload.
    *
    * @param isLastPart
    *            Whether or not this is the last part being uploaded in a
    *            multipart upload.
    */
    public void setLastPart(boolean isLastPart) {
        this.isLastPart = isLastPart;
    }

    /**
     * Marks this part as the last part being uploaded in a multipart upload,
     * and returns this updated request object so that additional method calls
     * can be chained together.
     *
     * @param isLastPart
     *            Whether or not this is the last part being uploaded in a
     *            multipart upload.
     *
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public UploadPartRequest withLastPart(boolean isLastPart) {
        setLastPart(isLastPart);
        return this;
    }
    
    @Override
    public SSECustomerKey getSSECustomerKey() {
        return sseCustomerKey;
    }

    /**
     * Sets the optional customer-provided server-side encryption key to use to
     * encrypt the object part being uploaded.
     *
     * @param sseKey
     *            The optional customer-provided server-side encryption key to
     *            use to encrypt the object part being uploaded.
     */
    public void setSSECustomerKey(SSECustomerKey sseKey) {
        this.sseCustomerKey = sseKey;
    }

    /**
     * Sets the optional customer-provided server-side encryption key to use to
     * encrypt the object part being uploaded, and returns the updated request
     * object so that additional method calls can be chained together.
     *
     * @param sseKey
     *            The optional customer-provided server-side encryption key to
     *            use to encrypt the object part being uploaded.
     *
     * @return This updated request object so that additional method calls can
     *         be chained together.
     */
    public UploadPartRequest withSSECustomerKey(SSECustomerKey sseKey) {
        setSSECustomerKey(sseKey);
        return this;
    }

    /**
     * Returns the additional information about the part being uploaded.
     */
    public ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    /**
     * Sets the additional information about the part being uploaded.
     */
    public void setObjectMetadata(ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    /**
     * Fluent API for {@link #setObjectMetadata(ObjectMetadata)}.
     */
    public UploadPartRequest withObjectMetadata(ObjectMetadata objectMetadata) {
        setObjectMetadata(objectMetadata);
        return this;
    }

    public int getTrafficLimit() {
        return trafficLimit;
    }

    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }
}
