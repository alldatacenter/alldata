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

import java.util.Date;

import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Parameters in a request for downloading a file
 */
public class DownloadFileRequest extends BaseObjectRequest {
    private String downloadFile;

    private long partSize = 1024 * 1024 * 9L;

    private int taskNum = 1;

    private String checkpointFile;

    private boolean enableCheckpoint;

    private Date ifModifiedSince;

    private Date ifUnmodifiedSince;

    private String ifMatchTag;

    private String ifNoneMatchTag;

    private String versionId;

    private ProgressListener progressListener;

    private long progressInterval = ObsConstraint.DEFAULT_PROGRESS_INTERVAL;

    private CacheOptionEnum cacheOption;

    private long ttl;

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public DownloadFileRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.downloadFile = objectKey;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param downloadFile
     *            Path to the to-be-downloaded file
     */
    public DownloadFileRequest(String bucketName, String objectKey, String downloadFile) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.downloadFile = downloadFile;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param downloadFile
     *            Path to the to-be-downloaded file
     * @param partSize
     *            Part size
     */
    public DownloadFileRequest(String bucketName, String objectKey, String downloadFile, long partSize) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.downloadFile = downloadFile;
        this.partSize = partSize;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param downloadFile
     *            Path to the to-be-downloaded file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing download tasks
     *            concurrently
     */
    public DownloadFileRequest(String bucketName, String objectKey, String downloadFile, long partSize, int taskNum) {
        this(bucketName, objectKey, downloadFile, partSize, taskNum, false);
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param downloadFile
     *            Path to the to-be-downloaded file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing download tasks
     *            concurrently
     * @param enableCheckpoint
     *            Whether to enable the resumable mode
     * 
     */
    public DownloadFileRequest(String bucketName, String objectKey, String downloadFile, long partSize, int taskNum,
            boolean enableCheckpoint) {
        this(bucketName, objectKey, downloadFile, partSize, taskNum, enableCheckpoint, null);
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param downloadFile
     *            Path to the to-be-downloaded file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing download tasks
     *            concurrently
     * @param enableCheckpoint
     *            Whether to enable the resumable mode
     * @param checkpointFile
     *            File used to record download progresses in resumable mode
     * 
     */
    public DownloadFileRequest(String bucketName, String objectKey, String downloadFile, long partSize, int taskNum,
            boolean enableCheckpoint, String checkpointFile) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.partSize = partSize;
        this.taskNum = taskNum;
        this.downloadFile = downloadFile;
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
     * @param downloadFile
     *            Path to the to-be-downloaded file
     * @param partSize
     *            Part size
     * @param taskNum
     *            Maximum number of threads used for processing download tasks
     *            concurrently
     * @param enableCheckpoint
     *            Whether to enable the resumable mode
     * @param checkpointFile
     *            File used to record download progresses in resumable mode
     * @param versionId
     *            Version ID of the object
     * 
     */
    public DownloadFileRequest(String bucketName, String objectKey, String downloadFile, long partSize, int taskNum,
            boolean enableCheckpoint, String checkpointFile, String versionId) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.partSize = partSize;
        this.taskNum = taskNum;
        this.downloadFile = downloadFile;
        this.enableCheckpoint = enableCheckpoint;
        this.checkpointFile = checkpointFile;
        this.versionId = versionId;
    }

    /**
     * Obtain the path to the to-be-downloaded file.
     * 
     * @return Path to the to-be-downloaded file
     */
    public String getDownloadFile() {
        return downloadFile;
    }

    /**
     * Set the path to the to-be-downloaded file.
     * 
     * @param downloadFile
     *            Path to the to-be-downloaded file
     */
    public void setDownloadFile(String downloadFile) {
        this.downloadFile = downloadFile;
    }

    /**
     * Obtain the part size.
     * 
     * @return Part size
     */
    public long getPartSize() {
        return partSize;
    }

    /**
     * Set the part size.
     * 
     * @param partSize
     *            Part size
     */
    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    /**
     * Obtain the maximum number of threads used for processing download tasks
     * concurrently.
     * 
     * @return Maximum number of threads used for processing download tasks
     *         concurrently
     */
    public int getTaskNum() {
        return taskNum;
    }

    /**
     * Set the maximum number of threads used for processing download tasks
     * concurrently.
     * 
     * @param taskNum
     *            Maximum number of threads used for processing download tasks
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
     * File used to record download progresses in resumable mode
     * 
     * @return File used to record the download progress
     */
    public String getCheckpointFile() {
        return checkpointFile;
    }

    /**
     * Specify a file used to record resumable download progresses.
     * 
     * @param checkpointFile
     *            File used to record the download progress
     */
    public void setCheckpointFile(String checkpointFile) {
        this.checkpointFile = checkpointFile;
    }

    /**
     * Obtain the temporary file generated during the download.
     * 
     * @return Temporary file generated during the download
     */
    public String getTempDownloadFile() {
        return downloadFile + ".tmp";
    }

    /**
     * Obtain the time conditions set for downloading the object. Only when the
     * object is modified after the point in time specified by this parameter,
     * it will be downloaded. Otherwise, "304 Not Modified" will be returned.
     * 
     * @return Time condition set for downloading the object
     */
    public Date getIfModifiedSince() {
        return ServiceUtils.cloneDateIgnoreNull(this.ifModifiedSince);
    }

    /**
     * Set the time conditions set for downloading the object. Only when the
     * object is modified after the point in time specified by this parameter,
     * it will be downloaded. Otherwise, "304 Not Modified" will be returned.
     * 
     * @param ifModifiedSince
     *            Time condition set for downloading the object
     */
    public void setIfModifiedSince(Date ifModifiedSince) {
        this.ifModifiedSince = ServiceUtils.cloneDateIgnoreNull(ifModifiedSince);
    }

    /**
     * Obtain the time conditions for downloading the object. Only when the
     * object remains unchanged after the point in time specified by this
     * parameter, it will be downloaded; otherwise, "412 Precondition Failed"
     * will be returned.
     * 
     * @return Time condition set for downloading the object
     */
    public Date getIfUnmodifiedSince() {
        return ServiceUtils.cloneDateIgnoreNull(this.ifUnmodifiedSince);
    }

    /**
     * Set the time conditions for downloading the object. Only when the object
     * remains unchanged after the point in time specified by this parameter, it
     * will be downloaded; otherwise, "412 Precondition Failed" will be
     * returned.
     * 
     * @param ifUnmodifiedSince
     *            Time condition set for downloading the object
     */
    public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
        this.ifUnmodifiedSince = ServiceUtils.cloneDateIgnoreNull(ifUnmodifiedSince);
    }

    /**
     * Obtain the ETag verification conditions for downloading the object. Only
     * when the ETag of the object is the same as that specified by this
     * parameter, the object will be downloaded. Otherwise,
     * "412 Precondition Failed" will be returned.
     * 
     * @return ETag verification condition set for downloading the object
     */
    public String getIfMatchTag() {
        return ifMatchTag;
    }

    /**
     * Set the ETag verification conditions for downloading the object. Only
     * when the ETag of the object is the same as that specified by this
     * parameter, the object will be downloaded. Otherwise,
     * "412 Precondition Failed" will be returned.
     * 
     * @param ifMatchTag
     *            ETag verification condition set for downloading the object
     */
    public void setIfMatchTag(String ifMatchTag) {
        this.ifMatchTag = ifMatchTag;
    }

    /**
     * Obtain the ETag verification conditions for downloading the object. Only
     * when the ETag of the object is different from that specified by this
     * parameter, the object will be downloaded. Otherwise, "304 Not Modified"
     * will be returned.
     * 
     * @return ETag verification condition set for downloading the object
     */
    public String getIfNoneMatchTag() {
        return ifNoneMatchTag;
    }

    /**
     * Set the ETag verification conditions for downloading the object. Only
     * when the ETag of the object is different from that specified by this
     * parameter, the object will be downloaded. Otherwise, "304 Not Modified"
     * will be returned.
     * 
     * @param ifNoneMatchTag
     *            ETag verification condition set for downloading the object
     * 
     */
    public void setIfNoneMatchTag(String ifNoneMatchTag) {
        this.ifNoneMatchTag = ifNoneMatchTag;
    }

    /**
     * Obtain the object version ID.
     * 
     * @return Version ID of the object
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Set the version ID of the object.
     * 
     * @param versionId
     *            Version ID of the object
     * 
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
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
     * Obtain the control option of the read-ahead cache.
     * 
     * @return Control option of the read-ahead cache
     */
    public CacheOptionEnum getCacheOption() {
        return cacheOption;
    }

    /**
     * Set the control option of the read-ahead cache.
     * 
     * @param cacheOption
     *            Control option of the read-ahead cache
     */
    public void setCacheOption(CacheOptionEnum cacheOption) {
        this.cacheOption = cacheOption;
    }

    /**
     * Obtain the cache data expiration time.
     * 
     * @return Cache data expiration time
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * Set the cache data expiration time.
     * 
     * @param ttl
     *            Cache data expiration time
     */
    public void setTtl(long ttl) {
        if (ttl < 0 || ttl > 259200) {
            ttl = 60 * 60 * 24L;
        }
        this.ttl = ttl;
    }


    @Override
    public String toString() {
        return "DownloadFileRequest [bucketName=" + bucketName + ", objectKey=" + objectKey + ", downloadFile="
                + downloadFile + ", partSize=" + partSize + ", taskNum=" + taskNum + ", checkpointFile="
                + checkpointFile + ", enableCheckpoint=" + enableCheckpoint + ", ifModifiedSince=" + ifModifiedSince
                + ", ifUnmodifiedSince=" + ifUnmodifiedSince + ", ifMatchTag=" + ifMatchTag + ", ifNoneMatchTag="
                + ifNoneMatchTag + ", versionId=" + versionId + ", isEncodeHeaders=" + encodeHeaders + "]";
    }
}
