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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.task.DefaultTaskProgressStatus;
import com.obs.services.internal.task.LazyTaksCallback;
import com.obs.services.internal.task.PutObjectTask;
import com.obs.services.internal.task.RestoreObjectTask;
import com.obs.services.internal.task.ResumableUploadTask;
import com.obs.services.internal.task.UploadTaskProgressStatus;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.ExtensionObjectPermissionEnum;
import com.obs.services.model.KeyAndVersion;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import com.obs.services.model.ProgressListener;
import com.obs.services.model.ProgressStatus;
import com.obs.services.model.PutObjectBasicRequest;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.PutObjectsRequest;
import com.obs.services.model.RestoreObjectRequest;
import com.obs.services.model.RestoreObjectResult;
import com.obs.services.model.RestoreObjectsRequest;
import com.obs.services.model.SseCHeader;
import com.obs.services.model.SseKmsHeader;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.TaskCallback;
import com.obs.services.model.TaskProgressListener;
import com.obs.services.model.TaskProgressStatus;
import com.obs.services.model.UploadFileRequest;
import com.obs.services.model.UploadObjectsProgressListener;
import com.obs.services.model.UploadProgressStatus;
import com.obs.services.model.VersionOrDeleteMarker;

public abstract class AbstractBatchClient extends AbstractFileClient {
    private static final ILogger ILOG = LoggerBuilder.getLogger(AbstractBatchClient.class);
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#restoreObjects(com.obs.services.model.
     * RestoreObjectsRequest)
     */
    @Override
    public TaskProgressStatus restoreObjects(RestoreObjectsRequest request) throws ObsException {
        checkRestoreObjectsRequest(request);
        DefaultTaskProgressStatus progreStatus = new DefaultTaskProgressStatus();
        ThreadPoolExecutor executor = this.initThreadPool(request);

        try {
            TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback;
            TaskProgressListener listener;
            callback = (request.getCallback() == null)
                    ? new LazyTaksCallback<RestoreObjectResult, RestoreObjectRequest>() : request.getCallback();
            listener = request.getProgressListener();
            int progressInterval = request.getProgressInterval();
            int totalTasks = 0;
            if (request.getKeyAndVersions() != null) {
                totalTasks = request.getKeyAndVersions().size();
                for (KeyAndVersion kv : request.getKeyAndVersions()) {
                    RestoreObjectRequest taskRequest = new RestoreObjectRequest(request.getBucketName(), kv.getKey(),
                            kv.getVersion(), request.getDays(), request.getRestoreTier());
                    taskRequest.setRequesterPays(request.isRequesterPays());
                    RestoreObjectTask task = new RestoreObjectTask(this, request.getBucketName(), taskRequest,
                            callback, listener,
                            progreStatus, progressInterval);
                    executor.execute(task);
                }
            } else {
                if (request.isVersionRestored()) {
                    totalTasks = restoreVersions(request, progreStatus, executor, callback, listener, progressInterval);
                } else {
                    totalTasks = restoreCurrentObject(request, progreStatus, executor, callback, listener,
                            progressInterval);
                }
            }

            progreStatus.setTotalTaskNum(totalTasks);
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof ObsException) {
                throw (ObsException) e;
            } else {
                throw new ObsException(e.getMessage(), e);
            }
        }
        return progreStatus;
    }

    private int restoreCurrentObject(RestoreObjectsRequest request, DefaultTaskProgressStatus progreStatus,
            ThreadPoolExecutor executor, TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback,
            TaskProgressListener listener, int progressInterval) {
        int totalTasks = 0;
        ObjectListing objectsResult;
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest(request.getBucketName());
        listObjectsRequest.setRequesterPays(request.isRequesterPays());
        listObjectsRequest.setPrefix(request.getPrefix());
        listObjectsRequest.setEncodingType(request.getEncodingType());
        do {
            objectsResult = this.listObjects(listObjectsRequest);
            for (ObsObject o : objectsResult.getObjects()) {
                if (o.getMetadata().getObjectStorageClass() == StorageClassEnum.COLD) {
                    totalTasks++;
                    RestoreObjectRequest taskRequest = new RestoreObjectRequest(request.getBucketName(),
                            o.getObjectKey(), null, request.getDays(), request.getRestoreTier());
                    submitRestoreTask(request, progreStatus, executor, callback, listener, progressInterval, totalTasks,
                            taskRequest);
                }
            }
            listObjectsRequest.setMarker(objectsResult.getNextMarker());
        } while (objectsResult.isTruncated());
        return totalTasks;
    }

    private void submitRestoreTask(RestoreObjectsRequest request, DefaultTaskProgressStatus progreStatus,
            ThreadPoolExecutor executor, TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback,
            TaskProgressListener listener, int progressInterval, int totalTasks, RestoreObjectRequest taskRequest) {
        taskRequest.setRequesterPays(request.isRequesterPays());

        RestoreObjectTask task = new RestoreObjectTask(this, request.getBucketName(), taskRequest, callback,
                listener, progreStatus, progressInterval);
        executor.execute(task);
        if (ILOG.isInfoEnabled()) {
            if (totalTasks % 1000 == 0) {
                ILOG.info("RestoreObjects: " + totalTasks
                        + " tasks have submitted to restore objects");
            }
        }
    }

    private int restoreVersions(RestoreObjectsRequest request, DefaultTaskProgressStatus progreStatus,
            ThreadPoolExecutor executor, TaskCallback<RestoreObjectResult, RestoreObjectRequest> callback,
            TaskProgressListener listener, int progressInterval) {
        int totalTasks = 0;
        ListVersionsResult versionResult;
        ListVersionsRequest listRequest = new ListVersionsRequest(request.getBucketName());
        listRequest.setRequesterPays(request.isRequesterPays());
        listRequest.setEncodingType(request.getEncodingType());

        listRequest.setPrefix(request.getPrefix());
        do {
            versionResult = this.listVersions(listRequest);
            for (VersionOrDeleteMarker v : versionResult.getVersions()) {
                if (v.getObjectStorageClass() == StorageClassEnum.COLD) {
                    totalTasks++;
                    RestoreObjectRequest taskRequest = new RestoreObjectRequest(request.getBucketName(), v.getKey(),
                            v.getVersionId(), request.getDays(), request.getRestoreTier());
                    submitRestoreTask(request, progreStatus, executor, callback, listener, progressInterval, totalTasks,
                            taskRequest);
                }
            }
            listRequest.setKeyMarker(versionResult.getNextKeyMarker());
            listRequest.setVersionIdMarker(versionResult.getNextVersionIdMarker());
        } while (versionResult.isTruncated());
        return totalTasks;
    }

    private void checkRestoreObjectsRequest(RestoreObjectsRequest request) {
        ServiceUtils.assertParameterNotNull(request, "RestoreObjectsRequest is null");
        if (!this.isCname()) {
            ServiceUtils.assertParameterNotNull(request.getBucketName(), "bucketName is null");
        }

        if (request.getKeyAndVersions() != null && request.getPrefix() != null) {
            throw new IllegalArgumentException("Prefix and keyandVersions cannot coexist in the same request");
        }

        int days = request.getDays();
        if (!(days >= 1 && days <= 30)) {
            throw new IllegalArgumentException("Restoration days should be at least 1 and at most 30");
        }
    }
    
    @Override
    public UploadProgressStatus putObjects(final PutObjectsRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "PutObjectsRequest is null");

        ThreadPoolExecutor executor = this.initThreadPool(request);
        Date now = new Date();
        UploadTaskProgressStatus progressStatus = new UploadTaskProgressStatus(request.getTaskProgressInterval(), now);

        try {
            UploadObjectsProgressListener listener = request.getUploadObjectsProgressListener();
            TaskCallback<PutObjectResult, PutObjectBasicRequest> callback = (request.getCallback() == null)
                    ? new LazyTaksCallback<PutObjectResult, PutObjectBasicRequest>() : request.getCallback();
            String prefix = request.getPrefix() == null ? "" : request.getPrefix();

            int totalTasks = 0;
            if (request.getFolderPath() != null) {
                totalTasks = uploadFolder(request, executor, progressStatus, listener,
                        callback, prefix);
            } else if (request.getFilePaths() != null) {
                totalTasks = uploadFileLists(request, executor, progressStatus, listener,
                        callback, prefix);
            }

            progressStatus.setTotalTaskNum(totalTasks);
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
    
    /**
     * upload folder
     * @param request
     * @param executor
     * @param progressStatus
     * @param listener
     * @param callback
     * @param prefix
     * @return
     * @throws IOException
     */
    private int uploadFolder(final PutObjectsRequest request, ThreadPoolExecutor executor,
            UploadTaskProgressStatus progressStatus, UploadObjectsProgressListener listener,
            TaskCallback<PutObjectResult, PutObjectBasicRequest> callback, String prefix)
            throws IOException {
        int totalTasks = 0;
        String folderPath = request.getFolderPath();
        File fileRoot = new File(folderPath);

        if (!fileRoot.exists()) {
            String erroInfo = "putObjects: the folder \"" + folderPath + "\" dose not exist";
            ILOG.warn(erroInfo);
            throw new ObsException(erroInfo);
        }
        
        if (!fileRoot.isDirectory()) {
            String erroInfo = "putObjects: the folder \"" + folderPath + "\" dose not a folder";
            ILOG.warn(erroInfo);
            throw new ObsException(erroInfo);
        }
        
        String folderRoot = fileRoot.getName();
        LinkedList<File> list = new LinkedList<File>();
        list.add(fileRoot);
        File[] files = fileRoot.listFiles();
        File tempFile;
        while (!list.isEmpty()) {
            tempFile = list.removeFirst();
            if (null == tempFile) {
                continue;
            }
            files = tempFile.listFiles();
            if (null == files) {
                continue;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!file.exists()) {
                        String filePath = file.getCanonicalPath();
                        String erroInfo = "putObjects: the folder \"" + filePath
                                + "\" dose not a folder";
                        ILOG.warn(erroInfo);
                    } else {
                        list.add(file);
                    }
                } else {
                    // 文件上传
                    String filePath = file.getCanonicalPath();
                    if (!file.exists()) {
                        ILOG.warn("putObjects: the file \"" + filePath + "\" dose not exist");
                        continue;
                    }
                    totalTasks++;
                    String objectKey = prefix + folderRoot + filePath
                            .substring(folderPath.length(), filePath.length()).replace("\\", "/");
                    uploadObjectTask(request, filePath, objectKey, executor, progressStatus, callback,
                            listener);
                }
            }
        }
        
        return totalTasks;
    }

    /**
     * upload by file lists
     * @param request
     * @param executor
     * @param progressStatus
     * @param listener
     * @param callback
     * @param prefix
     * @return
     */
    private int uploadFileLists(final PutObjectsRequest request, ThreadPoolExecutor executor,
            UploadTaskProgressStatus progressStatus, UploadObjectsProgressListener listener,
            TaskCallback<PutObjectResult, PutObjectBasicRequest> callback, String prefix) {
        int totalTasks = 0;
        for (String filePath : request.getFilePaths()) {
            File file = new File(filePath);
            if (file.exists()) {
                totalTasks++;
                String objectKey = prefix + file.getName();
                uploadObjectTask(request, filePath, objectKey, executor, progressStatus, callback, listener);
            } else {
                ILOG.warn("putObjects: the file \"" + filePath + "\" is not exist");
            }
        }
        return totalTasks;
    }

    private void uploadObjectTask(final PutObjectsRequest request, final String filePath, final String objectKey,
            final ThreadPoolExecutor executor, final UploadTaskProgressStatus progressStatus,
            final TaskCallback<PutObjectResult, PutObjectBasicRequest> callback,
            final UploadObjectsProgressListener listener) {
        File fileObject = new File(filePath);
        String bucketName = request.getBucketName();
        int progressInterval = request.getProgressInterval();
        int taskNum = request.getTaskNum();
        long detailProgressInterval = request.getDetailProgressInterval();
        long bigfileThreshold = request.getBigfileThreshold();
        long partSize = request.getPartSize();
        AccessControlList acl = request.getAcl();
        Map<ExtensionObjectPermissionEnum, Set<String>> extensionPermissionMap = request.getExtensionPermissionMap();
        SseCHeader sseCHeader = request.getSseCHeader();
        SseKmsHeader sseKmsHeader = request.getSseKmsHeader();
        String successRedirectLocation = request.getSuccessRedirectLocation();

        if (fileObject.length() > bigfileThreshold) {
            UploadFileRequest taskRequest = 
                    new UploadFileRequest(bucketName, objectKey, filePath, partSize, taskNum, true);
            
            taskRequest.setExtensionPermissionMap(extensionPermissionMap);
            taskRequest.setAcl(acl);
            taskRequest.setSuccessRedirectLocation(successRedirectLocation);
            taskRequest.setSseCHeader(sseCHeader);
            taskRequest.setSseKmsHeader(sseKmsHeader);
            progressStatus.addTotalSize(fileObject.length());
            taskRequest.setRequesterPays(request.isRequesterPays());

            taskRequest.setProgressListener(createNewProgressListener(objectKey, progressStatus, listener));
            taskRequest.setProgressInterval(detailProgressInterval);

            ResumableUploadTask task = new ResumableUploadTask(this, bucketName, taskRequest, callback, listener,
                    progressStatus, progressInterval);
            executor.execute(task);
        } else {
            PutObjectRequest taskRequest = new PutObjectRequest(bucketName, objectKey, fileObject);
            taskRequest.setExtensionPermissionMap(extensionPermissionMap);
            taskRequest.setAcl(acl);
            taskRequest.setSuccessRedirectLocation(successRedirectLocation);
            taskRequest.setSseCHeader(sseCHeader);
            taskRequest.setSseKmsHeader(sseKmsHeader);
            progressStatus.addTotalSize(fileObject.length());
            taskRequest.setRequesterPays(request.isRequesterPays());

            taskRequest.setProgressListener(createNewProgressListener(objectKey, progressStatus, listener));
            taskRequest.setProgressInterval(detailProgressInterval);
            PutObjectTask task = new PutObjectTask(this, bucketName, taskRequest, callback, listener, progressStatus,
                    progressInterval);
            executor.execute(task);
        }
    }
    
    private ProgressListener createNewProgressListener(final String objectKey,
            final UploadTaskProgressStatus progressStatus, final UploadObjectsProgressListener listener) {
        return new ProgressListener() {
            @Override
            public void progressChanged(ProgressStatus status) {
                progressStatus.putTaskTable(objectKey, status);
                if (progressStatus.isRefreshprogress()) {
                    Date dateNow = new Date();
                    long totalMilliseconds = dateNow.getTime() - progressStatus.getStartDate().getTime();
                    progressStatus.setTotalMilliseconds(totalMilliseconds);
                    listener.progressChanged(progressStatus);
                }
            }
        };
    }
}
