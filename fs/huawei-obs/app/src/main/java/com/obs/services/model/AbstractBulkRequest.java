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

import com.obs.services.internal.ObsConstraint;

/**
 * Abstract class for request parameters of bulk tasks
 *
 */
public abstract class AbstractBulkRequest extends GenericRequest {

    protected TaskProgressListener listener;

    protected int taskThreadNum = ObsConstraint.DEFAULT_TASK_THREAD_NUM;

    protected int taskQueueNum = ObsConstraint.DEFAULT_WORK_QUEUE_NUM;

    protected int taskProgressInterval = ObsConstraint.DEFAULT_TASK_PROGRESS_INTERVAL;

    public AbstractBulkRequest() {
    }

    public AbstractBulkRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the progress listener of the bulk task.
     * 
     * @return Progress listener
     */
    public TaskProgressListener getProgressListener() {
        return listener;
    }

    /**
     * Set the progress listener of the bulk task.
     * 
     * @param listener
     *            Progress listener
     */
    public void setProgressListener(TaskProgressListener listener) {
        this.listener = listener;
    }

    /**
     * Obtain the maximum number of concurrent bulk tasks. The default value is
     * 10.
     * 
     * @return Maximum number of threads
     */
    public int getTaskThreadNum() {
        return taskThreadNum;
    }

    /**
     * Set the maximum number of concurrent bulk tasks. The default value is 10.
     * 
     * @param taskThreadNum
     *            Maximum number of threads
     */
    public void setTaskThreadNum(int taskThreadNum) {
        this.taskThreadNum = taskThreadNum;
    }

    /**
     * Obtain the queue length of the bulk task. The default value is 20000.
     * 
     * @return Length of the task queue
     */
    public int getTaskQueueNum() {
        return taskQueueNum;
    }

    /**
     * Set the task queue length of the thread pool in the bulk task. The
     * default value is 20000.
     * 
     * @param taskQueueNum
     *            Length of the task queue
     */
    public void setTaskQueueNum(int taskQueueNum) {
        this.taskQueueNum = taskQueueNum;
    }

    /**
     * Obtain the callback threshold of the task progress listener. The default
     * value is 50.
     * 
     * @return Callback threshold of the task progress listener
     */
    public int getProgressInterval() {
        return taskProgressInterval;
    }

    /**
     * Set the callback threshold of the task progress listener. The default
     * value is 50.
     * 
     * @param taskProgressInterval
     *            Callback threshold of the task progress listener
     */
    public void setProgressInterval(int taskProgressInterval) {
        if (taskProgressInterval <= 0) {
            throw new IllegalArgumentException("ProgressInterval should be greater than 0.");
        }
        this.taskProgressInterval = taskProgressInterval;
    }
}
