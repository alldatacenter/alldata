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
 * Response to the request for reading ahead objects
 *
 */
public class ReadAheadResult extends HeaderResponse {
    @JsonProperty(value = "bucket")
    private String bucketName;

    @JsonProperty(value = "prefix")
    private String prefix;

    @JsonProperty(value = "taskID")
    private String taskId;

    /**
     * Constructor
     */
    public ReadAheadResult() {

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Name prefix of objects to be read ahead
     * @param taskId
     *            ID of the read-ahead task
     */
    public ReadAheadResult(String bucketName, String prefix, String taskId) {
        this.bucketName = bucketName;
        this.prefix = prefix;
        this.taskId = taskId;
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
     * Obtain the ID of the read-ahead task.
     * 
     * @return ID of the read-ahead task
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Set the ID of the read-ahead task.
     * 
     * @param taskId
     *            ID of the read-ahead task
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String toString() {
        return "ReadAheadResult [bucketName=" + bucketName + ", prefix=" + prefix + ", taskId=" + taskId + "]";
    }
}
