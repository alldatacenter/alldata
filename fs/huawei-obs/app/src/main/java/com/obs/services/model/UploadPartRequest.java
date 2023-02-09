/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.io.File;
import java.io.InputStream;

import com.obs.services.internal.ObsConstraint;

/**
 * Parameters in a part upload request
 *
 */
public class UploadPartRequest extends AbstractMultipartRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private int partNumber;

    private Long partSize;

    private long offset;

    private SseCHeader sseCHeader;

    private String contentMd5;

    private boolean attachMd5 = false;

    private File file;

    private InputStream input;

    private boolean autoClose = true;

    private ProgressListener progressListener;

    private long progressInterval = ObsConstraint.DEFAULT_PROGRESS_INTERVAL;

    public UploadPartRequest() {
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Name of the bucket to which the multipart upload belongs
     * @param objectKey
     *            Name of the object involved in the multipart upload
     */
    public UploadPartRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Name of the bucket to which the multipart upload belongs
     * @param objectKey
     *            Name of the object involved in the multipart upload
     * @param fileName
     *            File name to be uploaded
     */
    public UploadPartRequest(String bucketName, String objectKey, String fileName) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.file = new File(fileName);
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Name of the bucket to which the multipart upload belongs
     * @param objectKey
     *            Name of the object involved in the multipart upload
     * @param file
     *            File to be uploaded
     */
    public UploadPartRequest(String bucketName, String objectKey, File file) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.file = file;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Name of the bucket to which the multipart upload belongs
     * @param objectKey
     *            Name of the object involved in the multipart upload
     * @param partSize
     *            Part size (in bytes)
     * @param input
     *            Data stream to be uploaded
     */
    public UploadPartRequest(String bucketName, String objectKey, Long partSize, InputStream input) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.partSize = partSize;
        this.input = input;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Name of the bucket to which the multipart upload belongs
     * @param objectKey
     *            Name of the object involved in the multipart upload
     * @param partSize
     *            Part size (in bytes)
     * @param offset
     *            Offset of the part in the file. The default value is 0 (in
     *            bytes).
     * @param file
     *            File to be uploaded
     */
    public UploadPartRequest(String bucketName, String objectKey, Long partSize, long offset, File file) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.partSize = partSize;
        this.offset = offset;
        this.file = file;
    }

    /**
     * Obtain SSE-C encryption headers.
     *
     * @return SSE-C encryption headers
     */
    public SseCHeader getSseCHeader() {
        return sseCHeader;
    }

    /**
     * Set SSE-C encryption headers.
     *
     * @param sseCHeader
     *            SSE-C encryption headers
     */
    public void setSseCHeader(SseCHeader sseCHeader) {
        this.sseCHeader = sseCHeader;
    }

    /**
     * Obtain the offset of the part in the file. The default value is 0 (in
     * bytes).
     *
     * @return Offset of the part in the file
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
     *            Offset of the part in the file
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * Obtain the part number.
     *
     * @return Part number
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Set the part number.
     *
     * @param partNumber
     *            Part number
     */
    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    /**
     * Set the part size (in bytes).
     *
     * @param partSize
     *            Part size
     */
    public void setPartSize(Long partSize) {
        this.partSize = partSize;
    }

    /**
     * Obtain the part size, in bytes.
     *
     * @return Part size
     */
    public Long getPartSize() {
        return partSize;
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
     * Obtain the data stream to be uploaded, which cannot be used with the file
     * to be uploaded.
     *
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
     */
    public void setInput(InputStream input) {
        this.input = input;
        this.file = null;
    }

    /**
     * Check whether the MD5 value of the data to be uploaded will be
     * automatically calculated. If the MD5 value is set, this parameter can be
     * ignored.
     *
     * @return Identifier specifying whether to automatically calculate the MD5
     *         value of the data to be uploaded
     */
    public boolean isAttachMd5() {
        return attachMd5;
    }

    /**
     * Specify whether to automatically calculate the MD5 value of the data to
     * be uploaded. If the MD5 value is set, this parameter can be ignored.
     *
     * @param attachMd5
     *            Identifier specifying whether to automatically calculate the
     *            MD5 value of the data to be uploaded
     */
    public void setAttachMd5(boolean attachMd5) {
        this.attachMd5 = attachMd5;
    }

    /**
     * Set the MD5 value of the data to be uploaded.
     *
     * @return MD5 value of the data to be uploaded
     */
    public String getContentMd5() {
        return contentMd5;
    }

    /**
     * Obtain the MD5 value of the data to be uploaded.
     *
     * @param contentMd5
     *            MD5 value of the data to be uploaded
     */
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
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
        return "UploadPartRequest [uploadId=" + this.getUploadId() + ", bucketName=" + this.getBucketName()
                + ", objectKey=" + this.getObjectKey()
                + ", partNumber=" + partNumber + ", partSize=" + partSize + ", offset=" + offset + ", sseCHeader="
                + sseCHeader + ", contentMd5=" + contentMd5 + ", attachMd5=" + attachMd5 + ", file=" + file + ", input="
                + input + "]";
    }

}
