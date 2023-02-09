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
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.ProgressStatus;
import com.obs.services.model.PutObjectBasicRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.TaskCallback;
import com.obs.services.model.UploadFileRequest;
import com.obs.services.model.UploadObjectsProgressListener;

public class ResumableUploadTask extends AbstractPutTask {
    private UploadFileRequest taskRequest;

    public ResumableUploadTask(AbstractClient obsClient, String bucketName, UploadFileRequest taskRequest,
            TaskCallback<PutObjectResult, PutObjectBasicRequest> callback,
            UploadObjectsProgressListener progressListener, UploadTaskProgressStatus progressStatus,
            int taskProgressInterval) {
        super(obsClient, bucketName, callback, progressListener, progressStatus, taskProgressInterval);
        
        this.taskRequest = taskRequest;
    }

    public UploadFileRequest getTaskRequest() {
        return taskRequest;
    }

    public void setTaskRequest(UploadFileRequest taskRequest) {
        this.taskRequest = taskRequest;
    }

    private void resumableUpload() {
        try {
            CompleteMultipartUploadResult result = this.getObsClient().uploadFile(taskRequest);
            this.getTaskStatus().succeedTaskIncrement();
            PutObjectResult ret = new PutObjectResult(result.getBucketName(), result.getObjectKey(), result.getEtag(),
                    result.getVersionId(), result.getObjectUrl(), result.getResponseHeaders(), result.getStatusCode());
            this.getCallback().onSuccess(ret);
        } catch (ObsException e) {
            this.getTaskStatus().failTaskIncrement();
            this.getCallback().onException(e, taskRequest);
        } finally {
            this.getTaskStatus().execTaskIncrement();
            if (this.getProgressListener() != null) {
                if (this.getTaskStatus().getExecTaskNum() % this.getTaskProgressInterval() == 0) {
                    this.getProgressListener().progressChanged(this.getTaskStatus());
                }
                if (this.getTaskStatus().getExecTaskNum() == this.getTaskStatus().getTotalTaskNum()) {
                    this.getProgressListener().progressChanged(this.getTaskStatus());
                }
            }

            final String key = taskRequest.getObjectKey();
            ProgressStatus status = this.getTaskStatus().getTaskStatus(key);
            if (status != null) {
                this.getTaskStatus().addEndingTaskSize(status.getTransferredBytes());
            }
            this.getTaskStatus().removeTaskTable(key);
        }
    }

    @Override
    public void run() {
        resumableUpload();
    }

}
