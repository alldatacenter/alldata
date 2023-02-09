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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to the request for querying the progress of the read-ahead task
 *
 */
public class ReadAheadQueryResult extends HeaderResponse {
    @JsonProperty(value = "bucket")
    private String bucketName;

    @JsonProperty(value = "prefix")
    private String prefix;

    @JsonProperty(value = "consumedTime")
    private long consumedTime;

    @JsonProperty(value = "finishedObjectNum")
    private long finishedObjectNum;

    @JsonProperty(value = "finishedSize")
    private long finishedSize;

    @JsonProperty(value = "status")
    private String status;

    /**
     * Constructor
     */
    public ReadAheadQueryResult() {

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Name prefix of objects to be read ahead
     * @param consumedTime
     *            Consumed time (seconds)
     * @param finishedObjectNum
     *            Number of finished objects
     * @param finishedSize
     *            Size of finished objects
     * @param status
     *            Task status
     */
    public ReadAheadQueryResult(String bucketName, String prefix, long consumedTime, long finishedObjectNum,
            long finishedSize, String status) {
        this.bucketName = bucketName;
        this.prefix = prefix;
        this.consumedTime = consumedTime;
        this.finishedObjectNum = finishedObjectNum;
        this.finishedSize = finishedSize;
        this.status = status;
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set the bucket name.
     * 
     * @param bucketName
     *            Bucket name
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the name prefix of objects to be read ahead.
     * 
     * @return Name prefix of objects to be read ahead
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the name prefix of objects to be read ahead.
     * 
     * @param prefix
     *            Name prefix of objects to be read ahead
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Obtain the consumed time (seconds)
     * 
     * @return Consumed time (seconds)
     */
    public long getConsumedTime() {
        return consumedTime;
    }

    /**
     * Set the consumed time (seconds)
     * 
     * @param consumedTime
     *            Consumed time (seconds)
     */
    public void setConsumedTime(long consumedTime) {
        this.consumedTime = consumedTime;
    }

    /**
     * Obtain the number of finished objects.
     * 
     * @return Number of finished objects
     */
    public long getFinishedObjectNum() {
        return finishedObjectNum;
    }

    /**
     * Set the number of finished objects.
     * 
     * @param finishedObjectNum
     *            Number of finished objects
     */
    public void setFinishedObjectNum(long finishedObjectNum) {
        this.finishedObjectNum = finishedObjectNum;
    }

    /**
     * Obtain the size of finished objects.
     * 
     * @return Size of finished objects
     */
    public long getFinishedSize() {
        return finishedSize;
    }

    /**
     * Set the size of finished objects.
     * 
     * @param finishedSize
     *            Size of finished objects
     */
    public void setFinishedSize(long finishedSize) {
        this.finishedSize = finishedSize;
    }

    /**
     * Obtain the task status.
     * 
     * @return Task status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the task status.
     * 
     * @param status
     *            Task status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ReadAheadQueryResult [bucketName=" + bucketName + ", prefix=" + prefix + ", consumedTime="
                + consumedTime + ", finishedObjectNum=" + finishedObjectNum + ", finishedSize=" + finishedSize
                + ", status=" + status + "]";
    }
}
