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


package com.obs.services.internal.task;

import com.obs.services.AbstractClient;
import com.obs.services.model.PutObjectBasicRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.TaskCallback;
import com.obs.services.model.UploadObjectsProgressListener;

public abstract class AbstractPutTask extends AbstractTask {
    private UploadObjectsProgressListener progressListener;

    private int taskProgressInterval;

    private TaskCallback<PutObjectResult, PutObjectBasicRequest> callback;

    private UploadTaskProgressStatus taskStatus;

    public AbstractPutTask(AbstractClient obsClient, String bucketName, 
            TaskCallback<PutObjectResult, PutObjectBasicRequest> callback,
            UploadObjectsProgressListener progressListener, UploadTaskProgressStatus progressStatus,
            int taskProgressInterval) {
        super(obsClient, bucketName);
        this.callback = callback;
        this.progressListener = progressListener;
        this.taskStatus = progressStatus;
        this.taskProgressInterval = taskProgressInterval;
    }

    public UploadObjectsProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(UploadObjectsProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public int getTaskProgressInterval() {
        return taskProgressInterval;
    }

    public void setTaskProgressInterval(int taskProgressInterval) {
        this.taskProgressInterval = taskProgressInterval;
    }

    public TaskCallback<PutObjectResult, PutObjectBasicRequest> getCallback() {
        return callback;
    }

    public void setCallback(TaskCallback<PutObjectResult, PutObjectBasicRequest> callback) {
        this.callback = callback;
    }

    public UploadTaskProgressStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(UploadTaskProgressStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}
