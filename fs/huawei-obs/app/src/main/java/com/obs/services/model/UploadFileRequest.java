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
 */

package com.obs.services.model;

import com.obs.services.internal.Constants;
import com.obs.services.internal.ObsConstraint;

/**
 * Parameters in a file upload request
 */
public class UploadFileRequest extends PutObjectBasicRequest {

    // Part size, in bytes. The default value is 9 MB.
    private long partSize = 1024 * 1024 * 9L;
    // Number of threads for uploading parts. The default value is 1.
    private int taskNum = 1;

    private String uploadFile;

    private boolean enableCheckpoint = false;

    private String checkpointFile;

    private ObjectMetadata objectMetadata;

    private boolean enableCheckSum = false;

    private ProgressListener progressListener;

    private String encodingType;

    private Callback callback;

    private long progressInterval = ObsConstraint.DEFAULT_PROGRESS_INTERVAL;

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public UploadFileRequest(String bucketName, String objectKey) {
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
     * @param uploadFile
     *            To-be-uploaded local file
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadFile = uploadFile;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadFile
     *            To-be-uploaded local file
     * @param encodingType
     *            Encoding type used for encoding objectKey
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile, String encodingType) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadFile = uploadFile;
        this.encodingType = encodingType;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadFile
     *            To-be-uploaded local file
     * @param partSize
     *            Part size
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile, long partSize) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadFile = uploadFile;
        this.partSize = partSize;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadFile
     *            To-be-uploaded local file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing upload tasks
     *            concurrently
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile, long partSize, int taskNum) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadFile = uploadFile;
        this.partSize = partSize;
        this.taskNum = taskNum;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadFile
     *            To-be-uploaded local file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing upload tasks
     *            concurrently
     * @param enableCheckpoint
     *            Whether to enable the resumable mode
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile, long partSize, int taskNum,
            boolean enableCheckpoint) {
        this(bucketName, objectKey, uploadFile, partSize, taskNum, enableCheckpoint, null);

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadFile
     *            To-be-uploaded local file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing upload tasks
     *            concurrently
     * @param enableCheckpoint
     *            Whether to enable the resumable mode
     * @param checkpointFile
     *            File used to record resumable upload progresses
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile, long partSize, int taskNum,
            boolean enableCheckpoint, String checkpointFile) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.partSize = partSize;
        this.taskNum = taskNum;
        this.uploadFile = uploadFile;
        this.enableCheckpoint = enableCheckpoint;
        this.checkpointFile = checkpointFile;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadFile
     *            To-be-uploaded local file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing upload tasks
     *            concurrently
     * @param enableCheckpoint
     *            Whether to enable the resumable mode
     * @param checkpointFile
     *            File used to record resumable upload progresses
     * @param enableCheckSum
     *            Whether to verify the to-be-uploaded file upon non-initial
     *            uploads in resumable upload mode
     */
    public UploadFileRequest(String bucketName, String objectKey, String uploadFile, long partSize, int taskNum,
            boolean enableCheckpoint, String checkpointFile, boolean enableCheckSum) {
        this(bucketName, objectKey, uploadFile, partSize, taskNum, enableCheckpoint, checkpointFile);
        this.enableCheckSum = enableCheckSum;
    }

    /**
     * Obtain the part size set for uploading the object.
     * 
     * @return Part size
     */
    public long getPartSize() {
        return partSize;
    }

    /**
     * Set the part size for uploading the object.
     * 
     * @param partSize
     *            Part size
     */
    public void setPartSize(long partSize) {
        if (partSize < Constants.MIN_PART_SIZE) {
            this.partSize = Constants.MIN_PART_SIZE;
        } else {
            this.partSize = Math.min(partSize, Constants.MAX_PART_SIZE);
        }
    }

    /**
     * Obtain the maximum number of threads used for processing upload tasks
     * concurrently.
     * 
     * @return Maximum number of threads used for processing upload tasks
     *         concurrently
     */
    public int getTaskNum() {
        return taskNum;
    }

    /**
     * Set the maximum number of threads used for executing upload tasks
     * concurrently.
     * 
     * @param taskNum
     *            Maximum number of threads used for processing upload tasks
     *            concurrently
     */
    public void setTaskNum(int taskNum) {
        if (taskNum < 1) {
            this.taskNum = 1;
        } else {
            this.taskNum = Math.min(taskNum, 1000);
        }
    }

    /**
     * Obtain the to-be-uploaded local file.
     * 
     * @return To-be-uploaded local file
     */
    public String getUploadFile() {
        return uploadFile;
    }

    /**
     * Specify the local file to be uploaded.
     * 
     * @param uploadFile
     *            To-be-uploaded local file
     */
    public void setUploadFile(String uploadFile) {
        this.uploadFile = uploadFile;
    }

    /**
     * Identify whether the resumable mode is enabled.
     * 
     * @return Identifier specifying whether the resumable mode is enabled
     */
    public boolean isEnableCheckpoint() {
        return enableCheckpoint;
    }

    /**
     * Specify whether to enable the resumable mode.
     * 
     * @param enableCheckpoint
     *            Identifier specifying whether the resumable mode is enabled
     */
    public void setEnableCheckpoint(boolean enableCheckpoint) {
        this.enableCheckpoint = enableCheckpoint;
    }

    /**
     * Obtain the file used to record resumable upload progresses.
     * 
     * @return File used to record upload progresses
     */
    public String getCheckpointFile() {
        return checkpointFile;
    }

    /**
     * Specify a file used to record resumable upload progresses.
     * 
     * @param checkpointFile
     *            File used to record upload progresses
     */
    public void setCheckpointFile(String checkpointFile) {
        this.checkpointFile = checkpointFile;
    }

    /**
     * Obtain object properties.
     * 
     * @return Object properties
     */
    public ObjectMetadata getObjectMetadata() {
        return objectMetadata;
    }

    /**
     * Set object properties.
     * 
     * @param objectMetadata
     *            Object properties
     */
    public void setObjectMetadata(ObjectMetadata objectMetadata) {
        this.objectMetadata = objectMetadata;
    }

    /**
     * Identify whether the file to be uploaded in resumable mode will be
     * verified.
     * 
     * @return Identifier specifying whether to verify the to-be-uploaded file
     */
    public boolean isEnableCheckSum() {
        return enableCheckSum;
    }

    /**
     * Specify whether to verify the file to be uploaded in resumable mode.
     * 
     * @param enableCheckSum
     *            Identifier specifying whether to verify the to-be-uploaded
     *            file
     */
    public void setEnableCheckSum(boolean enableCheckSum) {
        this.enableCheckSum = enableCheckSum;
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

    /**
     * Set encoding type for encoding objectKey, could choose "url"
     * @param encodingType
     *            encoding type for encoding objectKey
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Get encoding type for encoding objectKey
     * @return encodingType
     */
    public String getEncodingType() {
        return encodingType;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public String toString() {
        return "UploadFileRequest [bucketName=" + bucketName + ", objectKey=" + objectKey + ", partSize=" + partSize
                + ", taskNum=" + taskNum + ", uploadFile=" + uploadFile + ", enableCheckpoint=" + enableCheckpoint
                + ", checkpointFile=" + checkpointFile + ", objectMetadata=" + objectMetadata
                + ", isEncodeHeaders=" + encodeHeaders + ", enableCheckSum=" + enableCheckSum
                + ", encodingType=" + encodingType + "]";
    }

}
