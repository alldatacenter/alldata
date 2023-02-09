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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadFileRequest;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

public class UploadResumableClient {

    private static final ILogger log = LoggerBuilder.getLogger("com.obs.services.ObsClient");
    private AbstractClient obsClient;

    public UploadResumableClient(AbstractClient obsClient) {
        this.obsClient = obsClient;
    }

    public CompleteMultipartUploadResult uploadFileResume(UploadFileRequest uploadFileRequest) {
        ServiceUtils.assertParameterNotNull(uploadFileRequest, "UploadFileRequest is null");
        ServiceUtils.assertParameterNotNull(uploadFileRequest.getBucketName(), "bucketName is null");
        ServiceUtils.assertParameterNotNull2(uploadFileRequest.getObjectKey(), "objectKey is null");
        ServiceUtils.assertParameterNotNull(uploadFileRequest.getUploadFile(), "uploadfile is null");
        if (uploadFileRequest.isEnableCheckpoint()) {
            if (!ServiceUtils.isValid(uploadFileRequest.getCheckpointFile())) {
                uploadFileRequest.setCheckpointFile(uploadFileRequest.getUploadFile() + ".uploadFile_record");
            }
        }
        if (uploadFileRequest.getCallback() != null) {
            ServiceUtils.assertParameterNotNull(uploadFileRequest.getCallback().getCallbackUrl(),
                    "callbackUrl is null");
            ServiceUtils.assertParameterNotNull(uploadFileRequest.getCallback().getCallbackBody(),
                    "callbackBody is null");
        }

        try {
            return uploadFileCheckPoint(uploadFileRequest);
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

    protected void abortMultipartUploadSilent(String uploadId, UploadFileRequest uploadFileRequest) {
        try {
            abortMultipartUpload(uploadId, uploadFileRequest);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Abort multipart upload failed", e);
            }
        }
    }

    protected HeaderResponse abortMultipartUpload(String uploadId, UploadFileRequest uploadFileRequest) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(uploadFileRequest.getBucketName(),
                uploadFileRequest.getObjectKey(), uploadId);
        request.setRequesterPays(uploadFileRequest.isRequesterPays());
        request.setUserHeaders(uploadFileRequest.getUserHeaders());
        return this.obsClient.abortMultipartUpload(request);
    }

    private CompleteMultipartUploadResult uploadFileCheckPoint(UploadFileRequest uploadFileRequest) throws Exception {
        UploadCheckPoint uploadCheckPoint = new UploadCheckPoint();
        if (uploadFileRequest.isEnableCheckpoint()) {
            prepareWithCheckpoint(uploadFileRequest, uploadCheckPoint);
        } else {
            prepare(uploadFileRequest, uploadCheckPoint);
        }

        // 开始上传
        List<PartResult> partResults = uploadfile(uploadFileRequest, uploadCheckPoint);

        // 有错误抛出异常,无异常则合并多段
        for (PartResult partResult : partResults) {
            if (partResult.isFailed() && partResult.getException() != null) {
                // 未开启，取消多段
                if (!uploadFileRequest.isEnableCheckpoint()) {
                    this.abortMultipartUploadSilent(uploadCheckPoint.uploadID, uploadFileRequest);
                } else if (uploadCheckPoint.isAbort) {
                    this.abortMultipartUploadSilent(uploadCheckPoint.uploadID, uploadFileRequest);
                    if (uploadCheckPoint.isDeleteUploadRecordFile) {
                        ServiceUtils.deleteFileIgnoreException(uploadFileRequest.getCheckpointFile());
                    }
                }
                throw partResult.getException();
            }
        }

        // 合并多段
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                uploadFileRequest.getBucketName(), uploadFileRequest.getObjectKey(), uploadCheckPoint.uploadID,
                uploadCheckPoint.partEtags);
        completeMultipartUploadRequest.setRequesterPays(uploadFileRequest.isRequesterPays());
        completeMultipartUploadRequest.setEncodingType(uploadFileRequest.getEncodingType());
        completeMultipartUploadRequest.setIsIgnorePort(uploadFileRequest.getIsIgnorePort());
        completeMultipartUploadRequest.setCallback(uploadFileRequest.getCallback());
        completeMultipartUploadRequest.setUserHeaders(uploadFileRequest.getUserHeaders());

        try {
            CompleteMultipartUploadResult result = this.obsClient
                    .completeMultipartUpload(completeMultipartUploadRequest);
            if (uploadFileRequest.isEnableCheckpoint()) {
                ServiceUtils.deleteFileIgnoreException(uploadFileRequest.getCheckpointFile());
            }
            return result;
        } catch (ObsException e) {
            if (!uploadFileRequest.isEnableCheckpoint()) {
                this.abortMultipartUpload(uploadCheckPoint.uploadID, uploadFileRequest);
            } else {
                if (e.getResponseCode() >= 300 && e.getResponseCode() < 500 && e.getResponseCode() != 408) {
                    this.abortMultipartUploadSilent(uploadCheckPoint.uploadID, uploadFileRequest);
                    ServiceUtils.deleteFileIgnoreException(uploadFileRequest.getCheckpointFile());
                }
            }
            throw e;
        }
    }

    private void prepareWithCheckpoint(UploadFileRequest uploadFileRequest, UploadCheckPoint uploadCheckPoint)
            throws IOException, Exception {
        boolean needRecreate = false;
        try {
            uploadCheckPoint.load(uploadFileRequest.getCheckpointFile());
        } catch (Exception e) {
            needRecreate = true;
        }

        if (!needRecreate) {
            if (!(uploadFileRequest.getBucketName().equals(uploadCheckPoint.bucketName)
                    && uploadFileRequest.getObjectKey().equals(uploadCheckPoint.objectKey)
                    && uploadFileRequest.getUploadFile().equals(uploadCheckPoint.uploadFile))) {
                needRecreate = true;
            } else if (!uploadCheckPoint.isValid(uploadFileRequest.getUploadFile())) {
                needRecreate = true;
            }
        }

        if (needRecreate) {
            if (uploadCheckPoint.bucketName != null && uploadCheckPoint.objectKey != null
                    && uploadCheckPoint.uploadID != null) {
                this.abortMultipartUploadSilent(uploadCheckPoint.uploadID, uploadFileRequest);
            }
            ServiceUtils.deleteFileIgnoreException(uploadFileRequest.getCheckpointFile());
            prepare(uploadFileRequest, uploadCheckPoint);
        }
    }

    private List<PartResult> uploadfile(UploadFileRequest uploadFileRequest, UploadCheckPoint uploadCheckPoint)
            throws Exception {
        ArrayList<PartResult> pieceResults = new ArrayList<PartResult>();
        ExecutorService executorService = Executors.newFixedThreadPool(uploadFileRequest.getTaskNum());
        ArrayList<Future<PartResult>> futures = new ArrayList<Future<PartResult>>();

        ProgressManager progressManager = null;
        if (uploadFileRequest.getProgressListener() == null) {
            for (int i = 0; i < uploadCheckPoint.uploadParts.size(); i++) {
                UploadPart uploadPart = uploadCheckPoint.uploadParts.get(i);
                if (uploadPart.isCompleted) {
                    PartResult pr = new PartResult(uploadPart.partNumber, uploadPart.offset, uploadPart.size);
                    pr.setFailed(false);
                    pieceResults.add(pr);
                } else {
                    futures.add(executorService
                            .submit(new Mission(i, uploadCheckPoint, i, uploadFileRequest, this.obsClient)));
                }
            }
        } else {
            long transferredBytes = 0L;
            List<Mission> unfinishedUploadMissions = new LinkedList<Mission>();
            for (int i = 0; i < uploadCheckPoint.uploadParts.size(); i++) {
                UploadPart uploadPart = uploadCheckPoint.uploadParts.get(i);
                if (uploadPart.isCompleted) {
                    PartResult pr = new PartResult(uploadPart.partNumber, uploadPart.offset, uploadPart.size);
                    pr.setFailed(false);
                    pieceResults.add(pr);
                    transferredBytes += uploadPart.size;
                } else {
                    unfinishedUploadMissions
                            .add(new Mission(i, uploadCheckPoint, i, uploadFileRequest, this.obsClient));
                }
            }
            progressManager = new ConcurrentProgressManager(uploadCheckPoint.uploadFileStatus.size, transferredBytes,
                    uploadFileRequest.getProgressListener(), uploadFileRequest.getProgressInterval() > 0
                            ? uploadFileRequest.getProgressInterval() : ObsConstraint.DEFAULT_PROGRESS_INTERVAL);
            for (Mission mission : unfinishedUploadMissions) {
                mission.setProgressManager(progressManager);
                futures.add(executorService.submit(mission));
            }
        }

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        for (Future<PartResult> future : futures) {
            try {
                PartResult tr = future.get();
                pieceResults.add(tr);
            } catch (ExecutionException e) {
                if (!uploadFileRequest.isEnableCheckpoint()) {
                    this.abortMultipartUploadSilent(uploadCheckPoint.uploadID, uploadFileRequest);
                }
                throw e;
            }
        }

        if (progressManager != null) {
            progressManager.progressEnd();
        }

        return pieceResults;
    }

    static class Mission implements Callable<PartResult> {

        private int id;
        private UploadCheckPoint uploadCheckPoint;
        private int partIndex;
        private UploadFileRequest uploadFileRequest;
        private AbstractClient obsClient;
        private ProgressManager progressManager;

        public Mission(int id, UploadCheckPoint uploadCheckPoint, int partIndex, UploadFileRequest uploadFileRequest,
                AbstractClient obsClient) {
            this.id = id;
            this.uploadCheckPoint = uploadCheckPoint;
            this.partIndex = partIndex;
            this.uploadFileRequest = uploadFileRequest;
            this.obsClient = obsClient;
        }

        @Override
        public PartResult call() throws Exception {
            PartResult tr = null;
            UploadPart uploadPart = uploadCheckPoint.uploadParts.get(partIndex);
            tr = new PartResult(partIndex + 1, uploadPart.offset, uploadPart.size);
            if (!uploadCheckPoint.isAbort) {
                InputStream input = null;
                try {
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(uploadFileRequest.getBucketName());
                    uploadPartRequest.setObjectKey(uploadFileRequest.getObjectKey());
                    uploadPartRequest.setUploadId(uploadCheckPoint.uploadID);
                    uploadPartRequest.setPartSize(uploadPart.size);
                    uploadPartRequest.setPartNumber(uploadPart.partNumber);
                    uploadPartRequest.setRequesterPays(uploadFileRequest.isRequesterPays());
                    uploadPartRequest.setUserHeaders(uploadFileRequest.getUserHeaders());

                    if (this.progressManager == null) {
                        uploadPartRequest.setFile(new File(uploadFileRequest.getUploadFile()));
                        uploadPartRequest.setOffset(uploadPart.offset);
                    } else {
                        input = new FileInputStream(uploadFileRequest.getUploadFile());
                        long offset = uploadPart.offset;
                        long skipByte = input.skip(offset);
                        if (offset < skipByte) {
                            log.error(String.format(
                                    "The actual number of skipped bytes (%d) is less than expected (%d): ", skipByte,
                                    offset));
                        }
                        // TODO no md5
                        uploadPartRequest.setInput(new ProgressInputStream(input, this.progressManager, false));
                    }

                    UploadPartResult result = obsClient.uploadPart(uploadPartRequest);

                    PartEtag partEtag = new PartEtag(result.getEtag(), result.getPartNumber());
                    uploadCheckPoint.update(partIndex, partEtag, true);
                    tr.setFailed(false);

                    if (uploadFileRequest.isEnableCheckpoint()) {
                        uploadCheckPoint.record(uploadFileRequest.getCheckpointFile());
                    }

                } catch (ObsException e) {
                    if (e.getResponseCode() >= 300 && e.getResponseCode() < 500 && e.getResponseCode() != 408) {
                        uploadCheckPoint.isAbort = true;
                    }
                    if (e.getResponseCode() == 403) {
                        uploadCheckPoint.isDeleteUploadRecordFile = false;
                    }
                    // 有异常打印到日志文件
                    tr.setFailed(true);
                    tr.setException(e);
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Task %d:%s upload part %d failed: ", id, "upload" + id, partIndex + 1),
                                e);
                    }
                } catch (Exception e) {
                    tr.setFailed(true);
                    tr.setException(e);
                    if (log.isErrorEnabled()) {
                        log.error(String.format("Task %d:%s upload part %d failed: ", id, "upload" + id, partIndex + 1),
                                e);
                    }
                } finally {
                    if (null != input) {
                        input.close();
                    }
                }
            } else {
                tr.setFailed(true);
            }
            return tr;
        }

        public void setProgressManager(ProgressManager progressManager) {
            this.progressManager = progressManager;
        }

    }

    private void prepare(UploadFileRequest uploadFileRequest, UploadCheckPoint uploadCheckPoint) throws Exception {
        uploadCheckPoint.uploadFile = uploadFileRequest.getUploadFile();
        uploadCheckPoint.bucketName = uploadFileRequest.getBucketName();
        uploadCheckPoint.objectKey = uploadFileRequest.getObjectKey();
        uploadCheckPoint.uploadFileStatus = FileStatus.getFileStatus(uploadCheckPoint.uploadFile,
                uploadFileRequest.isEnableCheckSum());
        uploadCheckPoint.uploadParts = splitUploadFile(uploadCheckPoint.uploadFileStatus.size,
                uploadFileRequest.getPartSize());
        uploadCheckPoint.partEtags = new ArrayList<PartEtag>();

        InitiateMultipartUploadRequest initiateUploadRequest = new InitiateMultipartUploadRequest(
                uploadFileRequest.getBucketName(), uploadFileRequest.getObjectKey());

        initiateUploadRequest.setExtensionPermissionMap(uploadFileRequest.getExtensionPermissionMap());
        initiateUploadRequest.setAcl(uploadFileRequest.getAcl());
        initiateUploadRequest.setSuccessRedirectLocation(uploadFileRequest.getSuccessRedirectLocation());
        initiateUploadRequest.setSseCHeader(uploadFileRequest.getSseCHeader());
        initiateUploadRequest.setSseKmsHeader(uploadFileRequest.getSseKmsHeader());
        initiateUploadRequest.setMetadata(uploadFileRequest.getObjectMetadata());
        initiateUploadRequest.setRequesterPays(uploadFileRequest.isRequesterPays());
        initiateUploadRequest.setEncodingType(uploadFileRequest.getEncodingType());
        initiateUploadRequest.setIsEncodeHeaders(uploadFileRequest.isEncodeHeaders());
        initiateUploadRequest.setUserHeaders(uploadFileRequest.getUserHeaders());

        InitiateMultipartUploadResult initiateUploadResult = this.obsClient
                .initiateMultipartUpload(initiateUploadRequest);
        uploadCheckPoint.uploadID = initiateUploadResult.getUploadId();
        if (uploadFileRequest.isEnableCheckpoint()) {
            try {
                uploadCheckPoint.record(uploadFileRequest.getCheckpointFile());
            } catch (Exception e) {
                this.abortMultipartUploadSilent(uploadCheckPoint.uploadID, uploadFileRequest);
                throw e;
            }
        }
    }

    private ArrayList<UploadPart> splitUploadFile(long size, long partSize) {
        ArrayList<UploadPart> parts = new ArrayList<UploadPart>();

        long partNum = size / partSize;
        if (partNum >= 10000) {
            partSize = size % 10000 == 0 ? size / 10000 : size / 10000 + 1;
            partNum = size / partSize;
        }
        if (size % partSize > 0) {
            partNum++;
        }
        if (partNum == 0) {
            UploadPart part = new UploadPart();
            part.partNumber = 1;
            part.offset = 0;
            part.size = 0;
            part.isCompleted = false;
            parts.add(part);
        } else {
            for (long i = 0; i < partNum; i++) {
                UploadPart part = new UploadPart();
                part.partNumber = (int) (i + 1);
                part.offset = i * partSize;
                part.size = partSize;
                part.isCompleted = false;
                parts.add(part);
            }
            if (size % partSize > 0) {
                parts.get(parts.size() - 1).size = size % partSize;
            }
        }

        return parts;
    }

    /**
     * 断点续传上传所需类
     */
    static class UploadCheckPoint implements Serializable {

        private static final long serialVersionUID = 5564757792864743464L;

        /**
         * 从checkpoint文件中加载checkpoint数据
         * 
         * @param checkPointFile
         * @throws Exception
         */
        public void load(String checkPointFile) throws Exception {
            FileInputStream fileInput = null;
            SecureObjectInputStream in = null;
            try {
                fileInput = new FileInputStream(checkPointFile);
                in = new SecureObjectInputStream(fileInput);
                UploadCheckPoint tmp = (UploadCheckPoint) in.readObject();
                assign(tmp);
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
                
                if (null != fileInput) {
                    try {
                        fileInput.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
            }
        }

        /**
         * 把checkpoint数据写到checkpoint文件
         * 
         * @param checkPointFile
         * @throws IOException
         */
        public synchronized void record(String checkPointFile) throws IOException {
            this.md5 = hashCode();
            FileOutputStream fileOutput = null;
            ObjectOutputStream outStream = null;
            try {
                fileOutput = new FileOutputStream(checkPointFile);
                outStream = new ObjectOutputStream(fileOutput);
                outStream.writeObject(this);
            } finally {
                if (null != outStream) {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
                
                if (null != fileOutput) {
                    try {
                        fileOutput.close();
                    } catch (IOException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("close failed.", e);
                        }
                    }
                }
            }
        }

        /**
         * 分块上传完成，更新状态
         * 
         * @param partIndex
         * @param partETag
         * @param completed
         */
        public synchronized void update(int partIndex, PartEtag partETag, boolean completed) {
            partEtags.add(partETag);
            uploadParts.get(partIndex).isCompleted = completed;
        }

        /**
         * 判读本地文件与checkpoint中记录的信息是否相符合，校验一致性
         * 
         * @param uploadFile
         * @return boolean
         * @throws IOException
         */
        public boolean isValid(String uploadFile) throws IOException {
            if (this.md5 != hashCode()) {
                return false;
            }

            File upload = new File(uploadFile);
            if (!this.uploadFile.equals(uploadFile) || this.uploadFileStatus.size != upload.length()
                    || this.uploadFileStatus.lastModified != upload.lastModified()) {
                return false;
            }

            if (this.uploadFileStatus.checkSum != null) {
                try {
                    return this.uploadFileStatus.checkSum
                            .equals(ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(new FileInputStream(upload))));
                } catch (NoSuchAlgorithmException e) {
                    throw new ObsException("computeMD5Hash failed.", e);
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((objectKey == null) ? 0 : objectKey.hashCode());
            result = prime * result + ((bucketName == null) ? 0 : bucketName.hashCode());
            result = prime * result + ((partEtags == null) ? 0 : partEtags.hashCode());
            result = prime * result + ((uploadFile == null) ? 0 : uploadFile.hashCode());
            result = prime * result + ((uploadFileStatus == null) ? 0 : uploadFileStatus.hashCode());
            result = prime * result + ((uploadID == null) ? 0 : uploadID.hashCode());
            result = prime * result + ((uploadParts == null) ? 0 : uploadParts.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof UploadCheckPoint) {
                    UploadCheckPoint uploadCheckPoint = (UploadCheckPoint) obj;
                    return uploadCheckPoint.hashCode() == obj.hashCode();
                }
            }
            return false;
        }

        private void assign(UploadCheckPoint tmp) {
            this.md5 = tmp.md5;
            this.bucketName = tmp.bucketName;
            this.uploadFile = tmp.uploadFile;
            this.uploadFileStatus = tmp.uploadFileStatus;
            this.objectKey = tmp.objectKey;
            this.uploadID = tmp.uploadID;
            this.uploadParts = tmp.uploadParts;
            this.partEtags = tmp.partEtags;
        }

        public int md5;
        public String uploadFile;
        public FileStatus uploadFileStatus;
        public String bucketName;
        public String objectKey;
        public String uploadID;
        public ArrayList<UploadPart> uploadParts;
        public ArrayList<PartEtag> partEtags;
        public transient volatile boolean isAbort = false;
        public transient volatile boolean isDeleteUploadRecordFile = true;
    }

    static class FileStatus implements Serializable {
        private static final long serialVersionUID = -3135754191745936521L;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((checkSum == null) ? 0 : checkSum.hashCode());
            result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
            result = prime * result + (int) (size ^ (size >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof FileStatus) {
                    FileStatus fileStatus = (FileStatus) obj;
                    return fileStatus.hashCode() == obj.hashCode();
                }
            }
            return false;
        }

        public static FileStatus getFileStatus(String uploadFile, boolean checkSum) throws IOException {
            FileStatus fileStatus = new FileStatus();
            File file = new File(uploadFile);
            fileStatus.size = file.length();
            fileStatus.lastModified = file.lastModified();
            if (checkSum) {
                try {
                    fileStatus.checkSum = ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(new FileInputStream(file)));
                } catch (NoSuchAlgorithmException e) {
                    throw new ObsException("computeMD5Hash failed.", e);
                }
            }
            return fileStatus;
        }

        public long size; // 文件大小
        public long lastModified; // 文件最后修改时间
        public String checkSum; // 文件checkSum
    }

    static class UploadPart implements Serializable {

        private static final long serialVersionUID = 751520598820222785L;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isCompleted ? 1 : 0);
            result = prime * result + partNumber;
            result = prime * result + (int) (offset ^ (offset >>> 32));
            result = prime * result + (int) (size ^ (size >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else {
                if (obj instanceof UploadPart) {
                    UploadPart uploadPart = (UploadPart) obj;
                    return uploadPart.hashCode() == obj.hashCode();
                }
            }
            return false;
        }

        public int partNumber; // 分片序号
        public long offset; // 分片在文件中的偏移量
        public long size; // 分片大小
        public boolean isCompleted; // 该分片上传是否完成
    }

    static class PartResult {

        public PartResult(int partNumber, long offset, long length) {
            this.partNumber = partNumber;
            this.offset = offset;
            this.length = length;
        }

        public int getpartNumber() {
            return partNumber;
        }

        public void setpartNumber(int partNumber) {
            this.partNumber = partNumber;
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }

        public boolean isFailed() {
            return isFailed;
        }

        public void setFailed(boolean isFailed) {
            this.isFailed = isFailed;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        private int partNumber; // 分片序号
        private long offset; // 分片在文件中的偏移
        private long length; // 分片长度
        private boolean isFailed; // 分片上传是否失败
        private Exception exception; // 分片上传异常
    }
}
