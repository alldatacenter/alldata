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

package com.obs.services.internal;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.AbstractClient;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.io.ProgressInputStream;
import com.obs.services.internal.utils.SecureObjectInputStream;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.DownloadFileResult;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.MonitorableProgressListener;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;

public class DownloadResumableClient {

    private static final ILogger log = LoggerBuilder.getLogger("com.obs.services.ObsClient");
    private AbstractClient obsClient;

    public DownloadResumableClient(AbstractClient obsClient) {
        this.obsClient = obsClient;
    }

    public DownloadFileResult downloadFileResume(DownloadFileRequest downloadFileRequest) {
        ServiceUtils.assertParameterNotNull(downloadFileRequest, "DownloadFileRequest is null");
        ServiceUtils.assertParameterNotNull(downloadFileRequest.getBucketName(), "the bucketName is null");
        String key = downloadFileRequest.getObjectKey();
        ServiceUtils.assertParameterNotNull2(key, "the objectKey is null");

        if (downloadFileRequest.getDownloadFile() == null) {
            downloadFileRequest.setDownloadFile(key);
        }
        if (downloadFileRequest.isEnableCheckpoint()) {
            if (downloadFileRequest.getCheckpointFile() == null || downloadFileRequest.getCheckpointFile().isEmpty()) {
                downloadFileRequest.setCheckpointFile(downloadFileRequest.getDownloadFile() + ".downloadFile_record");
            }
        }
        try {
            return downloadCheckPoint(downloadFileRequest);
        } catch (ServiceException e) {
            throw ServiceUtils.changeFromServiceException(e);
        } catch (Exception e) {
            if (e instanceof ObsException) {
                throw (ObsException) e;
            } else {
                throw new ObsException(e.getMessage(), e);
            }
        }
    }

    private DownloadFileResult downloadCheckPoint(DownloadFileRequest downloadFileRequest) throws Exception {

        ObjectMetadata objectMetadata = getObjectMetadata(downloadFileRequest);

        DownloadFileResult downloadFileResult = new DownloadFileResult();
        downloadFileResult.setObjectMetadata(objectMetadata);

        if (objectMetadata.getContentLength() == 0) {
            ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getTempDownloadFile());
            ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getCheckpointFile());

            File dfile = new File(downloadFileRequest.getDownloadFile());
            if (!dfile.getParentFile().mkdirs()) {
                if (log.isWarnEnabled()) {
                    log.warn("create parent directory failed.");
                }
            }
            new RandomAccessFile(dfile, "rw").close();
            if (downloadFileRequest.getProgressListener() != null) {
                downloadFileRequest.getProgressListener().progressChanged(new DefaultProgressStatus(0, 0, 0, 0, 0));
            }
            return downloadFileResult;
        }

        DownloadCheckPoint downloadCheckPoint = new DownloadCheckPoint();
        if (downloadFileRequest.isEnableCheckpoint()) {
            boolean needRecreate = false;
            try {
                downloadCheckPoint.load(downloadFileRequest.getCheckpointFile());
            } catch (Exception e) {
                needRecreate = true;
            }
            if (!needRecreate) {
                if (!(downloadFileRequest.getBucketName().equals(downloadCheckPoint.bucketName)
                        && downloadFileRequest.getObjectKey().equals(downloadCheckPoint.objectKey)
                        && downloadFileRequest.getDownloadFile().equals(downloadCheckPoint.downloadFile))) {
                    needRecreate = true;
                } else if (!downloadCheckPoint.isValid(downloadFileRequest.getTempDownloadFile(), objectMetadata)) {
                    needRecreate = true;
                } else if (downloadFileRequest.getVersionId() == null) {
                    if (downloadCheckPoint.versionId != null) {
                        needRecreate = true;
                    }
                } else if (!downloadFileRequest.getVersionId().equals(downloadCheckPoint.versionId)) {
                    needRecreate = true;
                }
            }
            if (needRecreate) {
                if (downloadCheckPoint.tmpFileStatus != null) {
                    ServiceUtils.deleteFileIgnoreException(downloadCheckPoint.tmpFileStatus.tmpFilePath);
                }

                ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getCheckpointFile());

                prepare(downloadFileRequest, downloadCheckPoint, objectMetadata);
            }
        } else {
            prepare(downloadFileRequest, downloadCheckPoint, objectMetadata);
        }

        // 并发下载分片
        DownloadResult downloadResult = this.download(downloadCheckPoint, downloadFileRequest);
        checkDownloadResult(downloadFileRequest, downloadCheckPoint, downloadResult);

        // 重命名临时文件
        renameTo(downloadFileRequest.getTempDownloadFile(), downloadFileRequest.getDownloadFile());

        // 开启了断点下载，成功上传后删除checkpoint文件
        if (downloadFileRequest.isEnableCheckpoint()) {
            ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getCheckpointFile());
        }

        return downloadFileResult;
    }

    private void checkDownloadResult(DownloadFileRequest downloadFileRequest, DownloadCheckPoint downloadCheckPoint,
            DownloadResult downloadResult) throws Exception {
        for (PartResultDown partResult : downloadResult.getPartResults()) {
            if (partResult.isFailed() && partResult.getException() != null) {
                if (!downloadFileRequest.isEnableCheckpoint()) {
                    ServiceUtils.deleteFileIgnoreException(downloadCheckPoint.tmpFileStatus.tmpFilePath);
                } else if (downloadCheckPoint.isAbort) {
                    ServiceUtils.deleteFileIgnoreException(downloadCheckPoint.tmpFileStatus.tmpFilePath);
                    ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getCheckpointFile());
                }
                throw partResult.getException();
            }
        }
    }

    private ObjectMetadata getObjectMetadata(DownloadFileRequest downloadFileRequest) {
        ObjectMetadata objectMetadata;
        try {
            GetObjectMetadataRequest request = new GetObjectMetadataRequest(downloadFileRequest.getBucketName(),
                    downloadFileRequest.getObjectKey(), downloadFileRequest.getVersionId());
            request.setRequesterPays(downloadFileRequest.isRequesterPays());
            request.setIsEncodeHeaders(downloadFileRequest.isEncodeHeaders());
            objectMetadata = this.obsClient.getObjectMetadata(request);
        } catch (ObsException e) {
            if (e.getResponseCode() >= 300 && e.getResponseCode() < 500 && e.getResponseCode() != 408) {
                ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getTempDownloadFile());
                ServiceUtils.deleteFileIgnoreException(downloadFileRequest.getCheckpointFile());
            }
            throw e;
        }
        return objectMetadata;
    }

    private void renameTo(String tempDownloadFilePath, String downloadFilePath) throws IOException {
        File tmpfile = new File(tempDownloadFilePath);
        File downloadFile = new File(downloadFilePath);
        if (!tmpfile.exists()) {
            throw new FileNotFoundException("tmpFile '" + tmpfile + "' does not exist");
        }
        if (downloadFile.exists()) {
            if (!downloadFile.delete()) {
                throw new IOException("downloadFile '" + downloadFile + "' is exist");
            }
        }
        if (tmpfile.isDirectory() || downloadFile.isDirectory()) {
            throw new IOException("downloadPath is a directory");
        }
        final boolean renameFlag = tmpfile.renameTo(downloadFile);
        if (!renameFlag) {
            InputStream input = null;
            OutputStream output = null;
            try {
                input = new FileInputStream(tmpfile);
                output = new FileOutputStream(downloadFile);
                byte[] buffer = new byte[1024 * 8];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            } finally {
                if (null != input) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
                
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
            }
            if (!tmpfile.delete()) {
                if (log.isErrorEnabled()) {
                    log.error("the tmpfile '" + tmpfile
                            + "' can not delete, please delete it to ensure the download finish.");
                }
                throw new IOException("the tmpfile '" + tmpfile
                        + "' can not delete, please delete it to ensure the download finish.");
            }
        }
    }

    private DownloadResult download(final DownloadCheckPoint downloadCheckPoint,
            final DownloadFileRequest downloadFileRequest) throws Exception {
        ArrayList<PartResultDown> taskResults = new ArrayList<PartResultDown>();
        DownloadResult downloadResult = new DownloadResult();
        ArrayList<Future<PartResultDown>> futures = new ArrayList<Future<PartResultDown>>();

        List<Task> unfinishedTasks = new LinkedList<Task>();
        long transferredBytes = 0L;
        for (int i = 0; i < downloadCheckPoint.downloadParts.size(); i++) {
            DownloadPart downloadPart = downloadCheckPoint.downloadParts.get(i);
            if (!downloadPart.isCompleted) {
                Task task = new Task(i, "download-" + i, downloadCheckPoint, i, downloadFileRequest, this.obsClient);
                unfinishedTasks.add(task);
            } else {
                transferredBytes += downloadPart.end - downloadPart.offset + 1;
                taskResults.add(new PartResultDown(i + 1, downloadPart.offset, downloadPart.end));
            }
        }

        ProgressManager progressManager = null;
        if (null != downloadFileRequest.getProgressListener()) {
            progressManager = new ConcurrentProgressManager(downloadCheckPoint.objectStatus.size, transferredBytes,
                    downloadFileRequest.getProgressListener(), downloadFileRequest.getProgressInterval() > 0
                            ? downloadFileRequest.getProgressInterval() : ObsConstraint.DEFAULT_PROGRESS_INTERVAL);
        }

        ExecutorService service = Executors.newFixedThreadPool(downloadFileRequest.getTaskNum());
        for (Task task : unfinishedTasks) {
            task.setProgressManager(progressManager);
            futures.add(service.submit(task));
        }

        service.shutdown();
        List<Runnable> notStartTasks = null;
        try {
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            notStartTasks = service.shutdownNow();
            Thread.currentThread().interrupt();
            throw ie;
        } finally {
            if (null != notStartTasks) {
                if (log.isWarnEnabled()) {
                    log.warn("there are still " + notStartTasks.size() + " tasks not started for request : "
                            + downloadFileRequest);
                }
            }
        }

        for (Future<PartResultDown> future : futures) {
            PartResultDown tr = future.get();
            taskResults.add(tr);
        }

        downloadResult.setPartResults(taskResults);
        if (progressManager != null) {
            progressManager.progressEnd();
        }

        return downloadResult;
    }

    static class Task implements Callable<PartResultDown> {

        private int id;
        private String name;
        private DownloadCheckPoint downloadCheckPoint;
        private int partIndex;
        private final DownloadFileRequest downloadFileRequest;
        private AbstractClient obsClient;
        private ProgressManager progressManager;

        public Task(int id, String name, DownloadCheckPoint downloadCheckPoint, int partIndex,
                DownloadFileRequest downloadFileRequest, AbstractClient obsClient) {
            if (null == downloadCheckPoint
                    || null == downloadFileRequest
                    || null == obsClient) {
                log.warn("some parameters is null. { " + downloadCheckPoint 
                        + ", " + downloadFileRequest + ", " + obsClient + " }");
                throw new IllegalArgumentException("some parameters is null.");
            }
            this.id = id;
            this.name = name;
            this.downloadCheckPoint = downloadCheckPoint;
            this.partIndex = partIndex;
            this.downloadFileRequest = downloadFileRequest;
            this.obsClient = obsClient;
        }

        @Override
        public PartResultDown call() throws Exception {
            DownloadPart downloadPart = downloadCheckPoint.downloadParts.get(partIndex);
            PartResultDown tr = new PartResultDown(partIndex + 1, downloadPart.offset, downloadPart.end);
            
            if (downloadCheckPoint.isAbort) {
                tr.setFailed(true);
                return tr;
            }
            
            RandomAccessFile output = null;
            InputStream content = null;
            
            try {
                if (log.isDebugEnabled()) {
                    log.debug("start task : " + downloadPart.toString());
                }

                // 标记启动一个子任务
                startOneTask(downloadFileRequest);

                output = new RandomAccessFile(downloadFileRequest.getTempDownloadFile(), "rw");
                output.seek(downloadPart.offset);

                GetObjectRequest getObjectRequest = createNewGetObjectRequest(downloadFileRequest, downloadPart);
                getObjectRequest.setIsEncodeHeaders(downloadFileRequest.isEncodeHeaders());

                ObsObject object = obsClient.getObject(getObjectRequest);
                content = object.getObjectContent();
                if (this.progressManager != null) {
                    content = new ProgressInputStream(content, this.progressManager, false);
                }

                byte[] buffer = new byte[ObsConstraint.DEFAULT_CHUNK_SIZE];
                int bytesOffset;
                while ((bytesOffset = content.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesOffset);
                }
                downloadCheckPoint.update(partIndex, true, downloadFileRequest.getTempDownloadFile());
            } catch (ObsException e) {
                if (e.getResponseCode() >= 300 && e.getResponseCode() < 500 && e.getResponseCode() != 408) {
                    downloadCheckPoint.isAbort = true;
                }
                signPartResultFailed(tr, e);
                if (log.isErrorEnabled()) {
                    log.error(String.format("Task %d:%s download part %d failed: ", id, name, partIndex), e);
                }
            } catch (Exception e) {
                signPartResultFailed(tr, e);
                if (log.isErrorEnabled()) {
                    log.error(String.format("Task %d:%s download part %d failed: ", id, name, partIndex), e);
                }
            } finally {
                // 结束一个子任务
                finishOneTask(downloadFileRequest);

                closeResource(output);
                
                closeResource(content);
                
                if (log.isDebugEnabled()) {
                    log.debug("end task : " + downloadPart.toString());
                }

                if (downloadFileRequest.isEnableCheckpoint()) {
                    downloadCheckPoint.updateTmpFile(downloadFileRequest.getTempDownloadFile());
                    downloadCheckPoint.record(downloadFileRequest.getCheckpointFile());
                }
            }
            return tr;
        }

        private void closeResource(Closeable resource) {
            if (null != resource) {
                try {
                    resource.close();
                } catch (IOException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("close failed.", e);
                    }
                }
            }
        }

        private void finishOneTask(DownloadFileRequest downloadFileRequest) {
            if (null != downloadFileRequest.getProgressListener()
                    && downloadFileRequest.getProgressListener() instanceof MonitorableProgressListener) {
                ((MonitorableProgressListener) downloadFileRequest.getProgressListener()).finishOneTask();
            }
        }

        private void startOneTask(DownloadFileRequest downloadFileRequest) {
            if (null != downloadFileRequest.getProgressListener()
                    && downloadFileRequest.getProgressListener() instanceof MonitorableProgressListener) {
                ((MonitorableProgressListener) downloadFileRequest.getProgressListener()).startOneTask();
            }
        }

        private void signPartResultFailed(PartResultDown tr, Exception e) {
            tr.setFailed(true);
            tr.setException(e);
        }

        private GetObjectRequest createNewGetObjectRequest(DownloadFileRequest downloadFileRequest, 
                DownloadPart downloadPart) {
            GetObjectRequest getObjectRequest = new GetObjectRequest(downloadFileRequest.getBucketName(),
                    downloadFileRequest.getObjectKey(), downloadFileRequest.getVersionId());

            getObjectRequest.setRequesterPays(downloadFileRequest.isRequesterPays());
            getObjectRequest.setIfMatchTag(downloadFileRequest.getIfMatchTag());
            getObjectRequest.setIfNoneMatchTag(downloadFileRequest.getIfNoneMatchTag());
            getObjectRequest.setIfModifiedSince(downloadFileRequest.getIfModifiedSince());
            getObjectRequest.setIfUnmodifiedSince(downloadFileRequest.getIfUnmodifiedSince());
            getObjectRequest.setRangeStart(downloadPart.offset);
            getObjectRequest.setRangeEnd(downloadPart.end);
            getObjectRequest.setCacheOption(downloadFileRequest.getCacheOption());
            getObjectRequest.setTtl(downloadFileRequest.getTtl());
            return getObjectRequest;
        }

        public void setProgressManager(ProgressManager progressManager) {
            this.progressManager = progressManager;
        }
    }

    private void prepare(DownloadFileRequest downloadFileRequest, DownloadCheckPoint downloadCheckPoint,
            ObjectMetadata objectMetadata) throws Exception {
        downloadCheckPoint.bucketName = downloadFileRequest.getBucketName();
        downloadCheckPoint.objectKey = downloadFileRequest.getObjectKey();
        downloadCheckPoint.versionId = downloadFileRequest.getVersionId();
        downloadCheckPoint.downloadFile = downloadFileRequest.getDownloadFile();
        ObjectStatus objStatus = new ObjectStatus();
        objStatus.size = objectMetadata.getContentLength();
        objStatus.lastModified = objectMetadata.getLastModified();
        objStatus.etag = objectMetadata.getEtag();
        downloadCheckPoint.objectStatus = objStatus;
        downloadCheckPoint.downloadParts = splitObject(downloadCheckPoint.objectStatus.size,
                downloadFileRequest.getPartSize());
        File tmpfile = new File(downloadFileRequest.getTempDownloadFile());
        if (null != tmpfile.getParentFile()) {
            if (!tmpfile.getParentFile().mkdirs()) {
                if (log.isWarnEnabled()) {
                    log.warn("create parent directory for tempfile failed.");
                }
            }
        }
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(tmpfile, "rw");
            randomAccessFile.setLength(downloadCheckPoint.objectStatus.size);
        } finally {
            if (null != randomAccessFile) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    if (log.isWarnEnabled()) {
                        log.warn("close failed.", e);
                    }
                }
            }
        }
        downloadCheckPoint.tmpFileStatus = new TmpFileStatus(downloadCheckPoint.objectStatus.size,
                new Date(tmpfile.lastModified()), downloadFileRequest.getTempDownloadFile());

        if (downloadFileRequest.isEnableCheckpoint()) {
            try {
                downloadCheckPoint.record(downloadFileRequest.getCheckpointFile());
            } catch (Exception e) {
                ServiceUtils.deleteFileIgnoreException(tmpfile);
                throw e;
            }
        }
    }

    private ArrayList<DownloadPart> splitObject(long size, long partSize) {
        ArrayList<DownloadPart> parts = new ArrayList<DownloadPart>();

        long piece = size / partSize;
        if (piece >= 10000) {
            partSize = size % 10000 == 0 ? size / 10000 : size / 10000 + 1;
        }

        long offset = 0L;
        for (int i = 0; offset < size; offset += partSize, i++) {
            DownloadPart downloadPart = new DownloadPart();
            downloadPart.partNumber = i;
            downloadPart.offset = offset;
            if (offset + partSize > size) {
                downloadPart.end = size - 1;
            } else {
                downloadPart.end = offset + partSize - 1;
            }
            parts.add(downloadPart);
        }
        return parts;
    }

    /**
     * 断点续传的下载所需类
     */
    static class DownloadCheckPoint implements Serializable {
        private static final long serialVersionUID = 2282950186694419179L;

        public int md5;
        public String bucketName;
        public String objectKey;
        public String versionId;
        public String downloadFile;
        public ObjectStatus objectStatus;
        public TmpFileStatus tmpFileStatus;
        ArrayList<DownloadPart> downloadParts;
        public transient volatile boolean isAbort = false;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bucketName == null) ? 0 : bucketName.hashCode());
            result = prime * result + ((downloadFile == null) ? 0 : downloadFile.hashCode());
            result = prime * result + ((versionId == null) ? 0 : versionId.hashCode());
            result = prime * result + ((objectKey == null) ? 0 : objectKey.hashCode());
            result = prime * result + ((objectStatus == null) ? 0 : objectStatus.hashCode());
            result = prime * result + ((tmpFileStatus == null) ? 0 : tmpFileStatus.hashCode());
            result = prime * result + ((downloadParts == null) ? 0 : downloadParts.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof DownloadCheckPoint) {
                    DownloadCheckPoint downloadCheckPoint = (DownloadCheckPoint) obj;
                    if (downloadCheckPoint.hashCode() == this.hashCode()) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * 从checkpoint文件中加载checkpoint数据
         * 
         * @param checkPointFile
         * @throws Exception
         */
        public void load(String checkPointFile) throws Exception {
            FileInputStream fileIn = null;
            SecureObjectInputStream in = null;
            try {
                fileIn = new FileInputStream(checkPointFile);
                in = new SecureObjectInputStream(fileIn);
                DownloadCheckPoint info = (DownloadCheckPoint) in.readObject();
                assign(info);
            } finally {
                if (null != in) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
                
                if (null != fileIn) {
                    try {
                        fileIn.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
            }
        }

        private void assign(DownloadCheckPoint info) {
            this.md5 = info.md5;
            this.downloadFile = info.downloadFile;
            this.bucketName = info.bucketName;
            this.objectKey = info.objectKey;
            this.versionId = info.versionId;
            this.objectStatus = info.objectStatus;
            this.tmpFileStatus = info.tmpFileStatus;
            this.downloadParts = info.downloadParts;
        }

        /**
         * 判断序列化文件、临时文件和实际信息是否一致
         * 
         * @param tmpFilePath
         *        临时文件路径
         * @param objectMetadata
         *        对象元数据
         */
        public boolean isValid(String tmpFilePath, ObjectMetadata objectMetadata) {
            if (this.md5 != hashCode()) {
                return false;
            }
            if (objectMetadata.getContentLength() != this.objectStatus.size
                    || !objectMetadata.getLastModified().equals(this.objectStatus.lastModified)
                    || !objectMetadata.getEtag().equals(this.objectStatus.etag)) {
                return false;
            }

            File tmpfile = new File(tmpFilePath);
            return this.tmpFileStatus.size == tmpfile.length();
        }

        /**
         * 分片下载成功后，更新分片和临时文件信息
         * 
         * @param index
         * @param completed
         * @param tmpFilePath
         * @throws IOException
         */
        public synchronized void update(int index, boolean completed, String tmpFilePath) throws IOException {
            downloadParts.get(index).isCompleted = completed;
            File tmpfile = new File(tmpFilePath);
            this.tmpFileStatus.lastModified = new Date(tmpfile.lastModified());
        }

        /**
         * 出现网络异常时,更新临时文件的修改时间
         * 
         * @param tmpFilePath
         * @throws IOException
         */
        public synchronized void updateTmpFile(String tmpFilePath) throws IOException {
            File tmpfile = new File(tmpFilePath);
            this.tmpFileStatus.lastModified = new Date(tmpfile.lastModified());
        }

        /**
         * 把DownloadCheckPoint数据写到序列化文件
         * 
         * @throws IOException
         */
        public synchronized void record(String checkPointFilePath) throws IOException {
            FileOutputStream fileOutStream = null;
            ObjectOutputStream objOutStream = null;
            this.md5 = hashCode();
            try {
                fileOutStream = new FileOutputStream(checkPointFilePath);
                objOutStream = new ObjectOutputStream(fileOutStream);
                objOutStream.writeObject(this);
            } finally {
                if (objOutStream != null) {
                    try {
                        objOutStream.close();
                    } catch (Exception e) {
                        log.warn("close outputstrem failed.", e);
                    }
                }
                if (fileOutStream != null) {
                    try {
                        fileOutStream.close();
                    } catch (Exception e) {
                        log.warn("close outputstrem failed.", e);
                    }
                }
            }
        }
    }

    static class ObjectStatus implements Serializable {

        private static final long serialVersionUID = -6267040832855296342L;

        public long size; // 桶中对象大小
        public Date lastModified; // 对象的最后修改时间
        public String etag; // 对象的Etag

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((etag == null) ? 0 : etag.hashCode());
            result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
            result = prime * result + (int) (size ^ (size >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof ObjectStatus) {
                    ObjectStatus objectStatus = (ObjectStatus) obj;
                    return objectStatus.hashCode() == this.hashCode();
                }
            }
            return false;
        }
    }

    static class TmpFileStatus implements Serializable {
        private static final long serialVersionUID = 4478330948103112660L;

        public long size; // 对象大小
        public Date lastModified; // 对象的最后修改时间
        public String tmpFilePath;

        public TmpFileStatus(long size, Date lastMoidified, String tmpFilePath) {
            this.size = size;
            this.lastModified = lastMoidified;
            this.tmpFilePath = tmpFilePath;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((lastModified == null) ? 0 : lastModified.hashCode());
            result = prime * result + ((tmpFilePath == null) ? 0 : tmpFilePath.hashCode());
            result = prime * result + (int) (size ^ (size >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof TmpFileStatus) {
                    TmpFileStatus tmpFileStatus = (TmpFileStatus) obj;
                    return tmpFileStatus.hashCode() == this.hashCode();
                }
            }
            return false;
        }
    }

    static class DownloadPart implements Serializable {

        private static final long serialVersionUID = 961987949814206093L;

        public int partNumber; // 分片序号，从0开始编号
        public long offset; // 分片起始位置
        public long end; // 分片片结束位置
        public boolean isCompleted; // 该分片下载是否完成

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partNumber;
            result = prime * result + (isCompleted ? 0 : 8);
            result = prime * result + (int) (end ^ (end >>> 32));
            result = prime * result + (int) (offset ^ (offset >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof DownloadPart) {
                    DownloadPart downloadPart = (DownloadPart) obj;
                    return downloadPart.hashCode() == this.hashCode();
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "DownloadPart [partNumber=" + partNumber + ", offset=" + offset + ", end=" + end + ", isCompleted="
                    + isCompleted + "]";
        }
    }

    static class PartResultDown {
        private int partNumber; // 分片序号，从1开始编号
        private long start; // 分片开始位置
        private long end; // 分片结束位置
        private boolean isFailed; // 分片上传是否失败
        private Exception exception; // 分片上传异常

        public PartResultDown(int partNumber, long start, long end) {
            this.partNumber = partNumber;
            this.start = start;
            this.end = end;
        }

        /**
         * 获取分片的起始位置
         * 
         * @return 分片的起始位置
         */
        public long getStart() {
            return start;
        }

        /**
         * 设置分片的起始位置
         * 
         * @param start
         *            分片起始位置
         */
        public void setStart(long start) {
            this.start = start;
        }

        /**
         * 获取分片的结束位置
         * 
         * @return 分片的结束位置
         */
        public long getEnd() {
            return end;
        }

        /**
         * 设置分片的结束位置
         * 
         * @param end
         *            分片结束位置
         */
        public void setEnd(long end) {
            this.end = end;
        }

        /**
         * 获取分片的编号
         * 
         * @return 分片的编号
         */
        public int getpartNumber() {
            return partNumber;
        }

        /**
         * 获取分片的下载状态
         * 
         * @return 分片的下载状态
         */
        public boolean isFailed() {
            return isFailed;
        }

        /**
         * 设置分片的下载状态
         * 
         * @param failed
         *            分片的下载状态
         */
        public void setFailed(boolean failed) {
            this.isFailed = failed;
        }

        /**
         * 获取分片的下载异常
         * 
         * @return 分片的下载异常
         */
        public Exception getException() {
            return exception;
        }

        /**
         * 设置分片的下载异常
         * 
         * @param exception
         *            分片的下载异常
         */
        public void setException(Exception exception) {
            this.exception = exception;
        }
    }

    static class DownloadResult {

        private List<PartResultDown> partResults;

        /**
         * 获取分片的上传最终结果
         * 
         * @return 分片的上传汇总结果
         */
        public List<PartResultDown> getPartResults() {
            return partResults;
        }

        /**
         * 设置分片的上传最终结果
         * 
         * @param partResults
         *            分片的上传汇总结果
         */
        public void setPartResults(List<PartResultDown> partResults) {
            this.partResults = partResults;
        }

    }
}
