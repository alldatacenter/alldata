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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.obs.services.exception.ObsException;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectResult;
import com.obs.services.model.TaskCallback;

public class BlockRejectedExecutionHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
            executor.getQueue().put(r);
        } catch (InterruptedException e) {
            if (r instanceof RunnableFuture) {
                throw new RejectedExecutionException(e);
            }
            
            ObsException obsException = new ObsException(e.getMessage(), e);
            if (r instanceof RestoreObjectTask) {
                RestoreObjectTask task = (RestoreObjectTask) r;
                task.getProgressStatus().failTaskIncrement();
                TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback = task.getCallback();
                callback.onException(obsException, task.getTaskRequest());
            } 
        }
    }
}
