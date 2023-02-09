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


package com.obs.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.DownloadResumableClient;
import com.obs.services.internal.UploadResumableClient;
import com.obs.services.internal.task.DefaultTaskProgressStatus;
import com.obs.services.internal.task.DropFolderTask;
import com.obs.services.internal.task.LazyTaksCallback;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.DownloadFileResult;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.MonitorableProgressListener;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import com.obs.services.model.TaskCallback;
import com.obs.services.model.TaskProgressListener;
import com.obs.services.model.TaskProgressStatus;
import com.obs.services.model.UploadFileRequest;
import com.obs.services.model.fs.DropFolderRequest;

public abstract class AbstractFileClient extends AbstractPFSClient {
    private static final ILogger ILOG = LoggerBuilder.getLogger(AbstractFileClient.class);
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#uploadFile(com.obs.services.model.
     * UploadFileRequest)
     */
    @Override
    public CompleteMultipartUploadResult uploadFile(UploadFileRequest uploadFileRequest) throws ObsException {
        return new UploadResumableClient(this).uploadFileResume(uploadFileRequest);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#downloadFile(com.obs.services.model.
     * DownloadFileRequest)
     */
    @Override
    public DownloadFileResult downloadFile(DownloadFileRequest downloadFileRequest) throws ObsException {
        try {
            return new DownloadResumableClient(this).downloadFileResume(downloadFileRequest);
        } finally {
            if (null != downloadFileRequest.getProgressListener()
                    && downloadFileRequest.getProgressListener() instanceof MonitorableProgressListener) {
                ((MonitorableProgressListener)downloadFileRequest.getProgressListener()).finishOneTask();
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IFSClient#deleteFolder(com.obs.services.model.fs.
     * DeleteFSFolderRequest)
     */
    @Override
    public TaskProgressStatus dropFolder(DropFolderRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "DropFolderRequest is null");
        if (!this.isCname()) {
            ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucketName is null");
        }
        ThreadPoolExecutor executor = this.initThreadPool(request);
        DefaultTaskProgressStatus progressStatus = new DefaultTaskProgressStatus();
        try {
            String folderName = request.getFolderName();
            String delimiter = this.getFileSystemDelimiter();
            if (!folderName.endsWith(delimiter)) {
                folderName = folderName + delimiter;
            }
            TaskCallback<DeleteObjectResult, String> callback = (request.getCallback() == null)
                    ? new LazyTaksCallback<DeleteObjectResult, String>()
                    : request.getCallback();
            TaskProgressListener listener = request.getProgressListener();
            int interval = request.getProgressInterval();
            int[] totalTasks = {0};
            boolean isSubDeleted = recurseFolders(request, folderName, callback, progressStatus, executor, totalTasks);
            Map<String, Future<?>> futures = new HashMap<String, Future<?>>();
            totalTasks[0]++;
            progressStatus.setTotalTaskNum(totalTasks[0]);

            if (isSubDeleted) {
                submitDropTask(request, folderName, callback, progressStatus, executor, futures);
                checkDropFutures(futures, progressStatus, callback, listener, interval);
            } else {
                progressStatus.failTaskIncrement();
                callback.onException(new ObsException("Failed to delete due to child file deletion failed"),
                        folderName);
                recordBulkTaskStatus(progressStatus, callback, listener, interval);
            }

            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof ObsException) {
                throw (ObsException) e;
            } else {
                throw new ObsException(e.getMessage(), e);
            }
        }
        return progressStatus;
    }


    private boolean recurseFolders(DropFolderRequest dropRequest, String folders, 
            TaskCallback<DeleteObjectResult, String> callback,
            DefaultTaskProgressStatus progressStatus, ThreadPoolExecutor executor, int[] count) {
        ListObjectsRequest request = new ListObjectsRequest(dropRequest.getBucketName());
        request.setDelimiter("/");
        request.setPrefix(folders);
        request.setRequesterPays(dropRequest.isRequesterPays());
        request.setEncodingType(dropRequest.getEncodingType());
        ObjectListing result;
        boolean isDeleted = true;
        do {
            result = this.listObjects(request);
            Map<String, Future<?>> futures = new HashMap<String, Future<?>>();

            for (ObsObject o : result.getObjects()) {
                if (!o.getObjectKey().endsWith("/")) {
                    count[0]++;
                    isDeleted = submitDropTask(dropRequest, o.getObjectKey(), callback, 
                            progressStatus, executor, futures) && isDeleted;
                    if (ILOG.isInfoEnabled()) {
                        if (count[0] % 1000 == 0) {
                            ILOG.info("DropFolder: " + Arrays.toString(count)
                                    + " tasks have submitted to delete objects");
                        }
                    }
                }
            }

            for (String prefix : result.getCommonPrefixes()) {
                boolean isSubDeleted = recurseFolders(dropRequest, prefix, callback, progressStatus, executor, count);
                count[0]++;
                if (isSubDeleted) {
                    isDeleted = submitDropTask(dropRequest, prefix, callback, 
                            progressStatus, executor, futures) && isDeleted;
                } else {
                    progressStatus.failTaskIncrement();
                    callback.onException(new ObsException("Failed to delete due to child file deletion failed"),
                            prefix);
                    recordBulkTaskStatus(progressStatus, callback, dropRequest.getProgressListener(), 
                            dropRequest.getProgressInterval());
                }
                if (ILOG.isInfoEnabled()) {
                    if (count[0] % 1000 == 0) {
                        ILOG.info("DropFolder: " + Arrays.toString(count) + " tasks have submitted to delete objects");
                    }
                }
            }

            request.setMarker(result.getNextMarker());
            isDeleted = checkDropFutures(futures, progressStatus, callback, dropRequest.getProgressListener(), 
                    dropRequest.getProgressInterval()) && isDeleted;
        } while (result.isTruncated());
        return isDeleted;
    }

    private boolean submitDropTask(DropFolderRequest request, String key, 
            TaskCallback<DeleteObjectResult, String> callback,
            DefaultTaskProgressStatus progreStatus,
            ThreadPoolExecutor executor, Map<String, Future<?>> futures) {
        DropFolderTask task = new DropFolderTask(this, request.getBucketName(), key, progreStatus, 
                request.getProgressListener(), request.getProgressInterval(), callback,
                request.isRequesterPays());
        try {
            futures.put(key, executor.submit(task));
        } catch (RejectedExecutionException e) {
            progreStatus.failTaskIncrement();
            callback.onException(new ObsException(e.getMessage(), e), key);
            return false;
        }
        return true;
    }

    private boolean checkDropFutures(Map<String, Future<?>> futures, DefaultTaskProgressStatus progressStatus,
            TaskCallback<DeleteObjectResult, String> callback, TaskProgressListener listener, int interval) {
        boolean isDeleted = true;
        for (Entry<String, Future<?>> entry : futures.entrySet()) {
            try {
                entry.getValue().get();
            } catch (ExecutionException e) {
                progressStatus.failTaskIncrement();
                if (e.getCause() instanceof ObsException) {
                    callback.onException((ObsException) e.getCause(), entry.getKey());
                } else {
                    callback.onException(new ObsException(e.getMessage(), e), entry.getKey());
                }
                isDeleted = false;
            } catch (InterruptedException e) {
                progressStatus.failTaskIncrement();
                callback.onException(new ObsException(e.getMessage(), e), entry.getKey());
                isDeleted = false;
            }
            recordBulkTaskStatus(progressStatus, callback, listener, interval);
        }
        return isDeleted;
    }
}
