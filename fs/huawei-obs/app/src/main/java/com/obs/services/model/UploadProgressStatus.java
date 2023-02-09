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

import java.util.concurrent.ConcurrentHashMap;

/*
 * Status information of the batch uploading of objects
 */
public interface UploadProgressStatus extends TaskProgressStatus {

    /**
     * Obtain the total size of uploaded objects.
     * 
     * @return Total size of uploaded objects. The value -1 indicates that the
     *         total size is still being calculated.
     */
    public long getTotalSize();

    /**
     * Obtain the size of transferred data in bytes.
     * 
     * @return Size of data in bytes that have been transferred
     */
    public long getTransferredSize();

    /**
     * Obtain the instantaneous speed.
     * 
     * @return Instantaneous speed
     */
    public double getInstantaneousSpeed();

    /**
     * Obtain the average speed.
     * 
     * @return Average speed
     */
    public double getAverageSpeed();

    /**
     * Obtain the progress of the current uploading task.
     * 
     * @return taskTable Progress of the current uploading task
     */
    public ConcurrentHashMap<String, ProgressStatus> getTaskTable();

    /**
     * Obtain the upload progress of a specified object.
     * 
     * @param key
     *            Object name
     * @return Upload progress of a specified object
     */
    public ProgressStatus getTaskStatus(String key);
}
