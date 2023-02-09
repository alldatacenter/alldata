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

import com.aliyun.oss.common.utils.BinaryUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The request class that is to download file with multiple parts download.
 *
 */
public class DownloadFileRequest extends GenericRequest {

    public DownloadFileRequest(String bucketName, String key) {
        super(bucketName, key);
    }

    public DownloadFileRequest(String bucketName, String key, String downloadFile, long partSize) {
        super(bucketName, key);
        this.partSize = partSize;
        this.downloadFile = downloadFile;
    }

    public DownloadFileRequest(String bucketName, String key, String downloadFile, long partSize, int taskNum,
            boolean enableCheckpoint) {
        this(bucketName, key, downloadFile, partSize, taskNum, enableCheckpoint, null);
    }

    public DownloadFileRequest(String bucketName, String key, String downloadFile, long partSize, int taskNum,
            boolean enableCheckpoint, String checkpointFile) {
        super(bucketName, key);
        this.partSize = partSize;
        this.taskNum = taskNum;
        this.downloadFile = downloadFile;
        this.enableCheckpoint = enableCheckpoint;
        this.checkpointFile = checkpointFile;
    }

    public long getPartSize() {
        return partSize;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public int getTaskNum() {
        return taskNum;
    }

    public void setTaskNum(int taskNum) {
        this.taskNum = taskNum;
    }

    public String getDownloadFile() {
        return downloadFile;
    }

    public String getTempDownloadFile() {
        if (getVersionId() != null) {
            return downloadFile + "." + BinaryUtil.encodeMD5(getVersionId().getBytes()) + ".tmp";
        } else {
            return downloadFile + ".tmp";
        }
    }

    public void setDownloadFile(String downloadFile) {
        this.downloadFile = downloadFile;
    }

    public boolean isEnableCheckpoint() {
        return enableCheckpoint;
    }

    public void setEnableCheckpoint(boolean enableCheckpoint) {
        this.enableCheckpoint = enableCheckpoint;
    }

    public String getCheckpointFile() {
        return checkpointFile;
    }

    public void setCheckpointFile(String checkpointFile) {
        this.checkpointFile = checkpointFile;
    }

    /**
     * Gets the ETag matching constraints. The download only happens if the
     * specified ETag matches the source file's ETag. If ETag does not match,
     * returns the precondition failure (412)
     * 
     * @return The expected ETag list.
     */
    public List<String> getMatchingETagConstraints() {
        return matchingETagConstraints;
    }

    /**
     * Sets the ETag matching constraints (optional). The download only happens
     * if the specified ETag matches the source file's ETag. If ETag does not
     * match, returns the precondition failure (412)
     * 
     * @param eTagList
     *            The expected ETag list.
     */
    public void setMatchingETagConstraints(List<String> eTagList) {
        this.matchingETagConstraints.clear();
        if (eTagList != null && !eTagList.isEmpty()) {
            this.matchingETagConstraints.addAll(eTagList);
        }
    }

    public void clearMatchingETagConstraints() {
        this.matchingETagConstraints.clear();
    }

    /**
     * Gets the ETag non-matching constraints. The download only happens if the
     * specified ETag does not match the source file's ETag. If ETag matches,
     * returns the precondition failure (412)
     * 
     * @return The expected ETag list.
     */
    public List<String> getNonmatchingETagConstraints() {
        return nonmatchingEtagConstraints;
    }

    /**
     * Sets the ETag non-matching constraints. The download only happens if the
     * specified ETag does not match the source file's ETag. If ETag matches,
     * returns the precondition failure (412)
     * 
     * @param eTagList
     *            The expected ETag list. For now only the first ETag is used,
     *            though the parameter is the list.
     */
    public void setNonmatchingETagConstraints(List<String> eTagList) {
        this.nonmatchingEtagConstraints.clear();
        if (eTagList != null && !eTagList.isEmpty()) {
            this.nonmatchingEtagConstraints.addAll(eTagList);
        }
    }

    public void clearNonmatchingETagConstraints() {
        this.nonmatchingEtagConstraints.clear();
    }

    /**
     * Gets the unmodified since constraint.
     * 
     * @return The time threshold. If it's same or later than the actual
     *         modified time, download the file.
     */
    public Date getUnmodifiedSinceConstraint() {
        return unmodifiedSinceConstraint;
    }

    /**
     * Sets the unmodified since constraint.
     * 
     * @param date
     *            The time threshold. If it's same or later than the actual
     *            modified time, download the file.
     */
    public void setUnmodifiedSinceConstraint(Date date) {
        this.unmodifiedSinceConstraint = date;
    }

    /**
     * Gets the modified since constraint.
     * 
     * @return The time threshold. If it's earlier than the actual modified
     *         time, download the file.
     */
    public Date getModifiedSinceConstraint() {
        return modifiedSinceConstraint;
    }

    /**
     * Sets the modified since constraint.
     * 
     * @param date
     *            The time threshold. If it's earlier than the actual modified
     *            time, download the file.
     */
    public void setModifiedSinceConstraint(Date date) {
        this.modifiedSinceConstraint = date;
    }

    /**
     * Gets response headers to override.
     * 
     * @return The headers to override with.
     */
    public ResponseHeaderOverrides getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Sets response headers to override.
     * 
     * @param responseHeaders
     *            The headers to override with.
     */
    public void setResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Sets traffic limit speed, its unit is bit/s
     *
     * @param trafficLimit
     *            traffic limit.
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

    /**
     * Gets the range of the object to download. The range is in the form of
     * {start, end}---start and end is position of the object's content.
     *
     * @return The range of the object to download.
     */
    public long[] getRange() {
        return range;
    }

    /**
     * Sets the range of the object to download (optional).
     *
     * @param start
     *            <p>
     *            Start position
     *            </p>
     *            <p>
     *            When the start is non-negative, it means the starting position
     *            to download. When the start is -1, it means the range is
     *            determined by the end only and the end could not be -1. For
     *            example, when start is -1 and end is 100. It means the
     *            download range will be the last 100 bytes.
     *            </p>
     * @param end
     *            <p>
     *            End position
     *            </p>
     *            <p>
     *            When the end is non-negative, it means the ending position to
     *            download. When the end is -1, it means the range is determined
     *            by the start only and the start could not be -1. For example,
     *            when end is -1 and start is 100. It means the download range
     *            will be all exception first 100 bytes.
     *            </p>
     */
    public void setRange(long start, long end) {
        range = new long[] { start, end };
    }

    // Part size in byte, by default it's 100KB.
    private long partSize = 1024 * 100;
    // Thread count for downloading parts, by default it's 1.
    private int taskNum = 1;
    // The local file path for the download.
    private String downloadFile;
    // Flag of enabling checkpoint.
    private boolean enableCheckpoint;
    // The local file path of the checkpoint file
    private String checkpointFile;

    // The matching ETag constraints
    private List<String> matchingETagConstraints = new ArrayList<String>();
    // The non-matching ETag constraints.
    private List<String> nonmatchingEtagConstraints = new ArrayList<String>();
    // The unmodified since constraint.
    private Date unmodifiedSinceConstraint;
    // The modified since constraints.
    private Date modifiedSinceConstraint;
    // The response headers to override.
    private ResponseHeaderOverrides responseHeaders;

    // Traffic limit speed, its uint is bit/s
    private int trafficLimit;

    private long[] range;
}
