/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.internal;

import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.common.utils.LogUtils.logException;
import static com.aliyun.oss.internal.OSSUtils.ensureBucketNameValid;
import static com.aliyun.oss.internal.OSSUtils.ensureObjectKeyValid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.InconsistentException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.CRC64;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.event.ProgressPublisher;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.Payer;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadFileResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

/**
 * OSSUploadOperation
 *
 */
public class OSSUploadOperation {

    protected UploadCheckPoint createUploadCheckPointWrap() {
        return new UploadCheckPoint();
    }

    protected void loadUploadCheckPointWrap(UploadCheckPoint uploadCheckPoint, String checkpointFile) throws Throwable {
        uploadCheckPoint.load(checkpointFile);
    }

    protected InitiateMultipartUploadResult initiateMultipartUploadWrap(UploadCheckPoint uploadCheckPoint,
            InitiateMultipartUploadRequest initiateMultipartUploadRequest) throws OSSException, ClientException {
        return multipartOperation.initiateMultipartUpload(initiateMultipartUploadRequest);
    }

    protected UploadPartResult uploadPartWrap(UploadCheckPoint uploadCheckPoint, UploadPartRequest request) throws OSSException, ClientException {
        return multipartOperation.uploadPart(request);
    }

    protected CompleteMultipartUploadResult completeMultipartUploadWrap(UploadCheckPoint uploadCheckPoint, CompleteMultipartUploadRequest request)
            throws OSSException, ClientException {
        return multipartOperation.completeMultipartUpload(request);
    }

    static class UploadCheckPoint implements Serializable {

        private static final long serialVersionUID = 5424904565837227164L;

        private static final String UPLOAD_MAGIC = "FE8BB4EA-B593-4FAC-AD7A-2459A36E2E62";

        /**
         * Gets the checkpoint data from the checkpoint file.
         */
        public synchronized void load(String cpFile) throws IOException, ClassNotFoundException {
            FileInputStream fileIn = new FileInputStream(cpFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            UploadCheckPoint ucp = (UploadCheckPoint) in.readObject();
            assign(ucp);
            in.close();
            fileIn.close();
        }

        /**
         * Writes the checkpoint data to the checkpoint file.
         */
        public synchronized void dump(String cpFile) throws IOException {
            this.md5 = hashCode();
            FileOutputStream fileOut = new FileOutputStream(cpFile);
            ObjectOutputStream outStream = new ObjectOutputStream(fileOut);
            outStream.writeObject(this);
            outStream.close();
            fileOut.close();
        }

        /**
         * The part upload complete, update the status.
         * 
         * @throws IOException
         */
        public synchronized void update(int partIndex, PartETag partETag, boolean completed) throws IOException {
            partETags.add(partETag);
            uploadParts.get(partIndex).isCompleted = completed;
        }

        /**
         * Check if the local file matches the checkpoint.
         */
        public synchronized boolean isValid(String uploadFile) {
            // 比较checkpoint的magic和md5
            // Compares the magic field in checkpoint and the file's md5.
            if (this.magic == null || !this.magic.equals(UPLOAD_MAGIC) || this.md5 != hashCode()) {
                return false;
            }

            // Checks if the file exists.
            File upload = new File(uploadFile);
            if (!upload.exists()) {
                return false;
            }

            // The file name, size and last modified time must be same as the
            // checkpoint.
            // If any item is changed, return false (re-upload the file).
            if (!this.uploadFile.equals(uploadFile) || this.uploadFileStat.size != upload.length()
                    || this.uploadFileStat.lastModified != upload.lastModified()) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((magic == null) ? 0 : magic.hashCode());
            result = prime * result + ((partETags == null) ? 0 : partETags.hashCode());
            result = prime * result + ((uploadFile == null) ? 0 : uploadFile.hashCode());
            result = prime * result + ((uploadFileStat == null) ? 0 : uploadFileStat.hashCode());
            result = prime * result + ((uploadID == null) ? 0 : uploadID.hashCode());
            result = prime * result + ((uploadParts == null) ? 0 : uploadParts.hashCode());
            result = prime * result + (int) originPartSize;
            return result;
        }

        public void assign(UploadCheckPoint ucp) {
            this.magic = ucp.magic;
            this.md5 = ucp.md5;
            this.uploadFile = ucp.uploadFile;
            this.uploadFileStat = ucp.uploadFileStat;
            this.key = ucp.key;
            this.uploadID = ucp.uploadID;
            this.uploadParts = ucp.uploadParts;
            this.partETags = ucp.partETags;
            this.originPartSize = ucp.originPartSize;
        }

        public String magic;
        public int md5;
        public String uploadFile;
        public FileStat uploadFileStat;
        public String key;
        public String uploadID;
        public ArrayList<UploadPart> uploadParts;
        public ArrayList<PartETag> partETags;
        public long originPartSize;
    }

    static class FileStat implements Serializable {
        private static final long serialVersionUID = -1223810339796425415L;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((digest == null) ? 0 : digest.hashCode());
            result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
            result = prime * result + (int) (size ^ (size >>> 32));
            return result;
        }

        public static FileStat getFileStat(String uploadFile) {
            FileStat fileStat = new FileStat();
            File file = new File(uploadFile);
            fileStat.size = file.length();
            fileStat.lastModified = file.lastModified();
            return fileStat;
        }

        public long size; // file size
        public long lastModified; // file last modified time.
        public String digest; // file content's digest (signature).
    }

    static class UploadPart implements Serializable {
        private static final long serialVersionUID = 6692863980224332199L;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isCompleted ? 1231 : 1237);
            result = prime * result + number;
            result = prime * result + (int) (offset ^ (offset >>> 32));
            result = prime * result + (int) (size ^ (size >>> 32));
            result = prime * result + (int) (crc ^ (crc >>> 32));
            return result;
        }

        public int number; // part number
        public long offset; // the offset in the file
        public long size; // part size
        public boolean isCompleted; // upload completeness flag.
        public long crc; //part crc
    }

    static class PartResult {

        public PartResult(int number, long offset, long length) {
            this.number = number;
            this.offset = offset;
            this.length = length;
        }

        public PartResult(int number, long offset, long length, long partCRC) {
            this.number = number;
            this.offset = offset;
            this.length = length;
            this.partCRC = partCRC;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
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
            return failed;
        }

        public void setFailed(boolean failed) {
            this.failed = failed;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public Long getPartCRC() {
            return partCRC;
        }

        public void setPartCRC(Long partCRC) {
            this.partCRC = partCRC;
        }

        private int number; // part number
        private long offset; // offset in the file
        private long length; // part size
        private boolean failed; // part upload failure flag
        private Exception exception; // part upload exception
        private Long partCRC;
    }

    public OSSUploadOperation(OSSMultipartOperation multipartOperation) {
        this.multipartOperation = multipartOperation;
    }

    public UploadFileResult uploadFile(UploadFileRequest uploadFileRequest) throws Throwable {
        assertParameterNotNull(uploadFileRequest, "uploadFileRequest");

        String bucketName = uploadFileRequest.getBucketName();
        String key = uploadFileRequest.getKey();

        assertParameterNotNull(bucketName, "bucketName");
        assertParameterNotNull(key, "key");
        ensureBucketNameValid(bucketName);
        ensureObjectKeyValid(key);

        assertParameterNotNull(uploadFileRequest.getUploadFile(), "uploadFile");

        // The checkpoint is enabled without specifying the checkpoint file,
        // using the default one.
        if (uploadFileRequest.isEnableCheckpoint()) {
            if (uploadFileRequest.getCheckpointFile() == null || uploadFileRequest.getCheckpointFile().isEmpty()) {
                uploadFileRequest.setCheckpointFile(uploadFileRequest.getUploadFile() + ".ucp");
            }
        }

        return uploadFileWithCheckpoint(uploadFileRequest);
    }

    private UploadFileResult uploadFileWithCheckpoint(UploadFileRequest uploadFileRequest) throws Throwable {
        UploadFileResult uploadFileResult = new UploadFileResult();
        UploadCheckPoint uploadCheckPoint = createUploadCheckPointWrap();

        // The checkpoint is enabled, reading the checkpoint data from the
        // checkpoint file.
        if (uploadFileRequest.isEnableCheckpoint()) {
            // The checkpoint file either does not exist, or is corrupted, the
            // whole file needs the re-upload.
            try {
                loadUploadCheckPointWrap(uploadCheckPoint, uploadFileRequest.getCheckpointFile());
            } catch (Exception e) {
                remove(uploadFileRequest.getCheckpointFile());
            }

            // The file uploaded is updated, re-upload.
            if (!uploadCheckPoint.isValid(uploadFileRequest.getUploadFile())) {
                prepare(uploadCheckPoint, uploadFileRequest);
                remove(uploadFileRequest.getCheckpointFile());
            }
        } else {
            // The checkpoint is not enabled, re-upload.
            prepare(uploadCheckPoint, uploadFileRequest);
        }
        
        // The progress tracker starts
        ProgressListener listener = uploadFileRequest.getProgressListener();
        ProgressPublisher.publishProgress(listener, ProgressEventType.TRANSFER_STARTED_EVENT);

        // Concurrently upload parts.
        List<PartResult> partResults = upload(uploadCheckPoint, uploadFileRequest);
        for (PartResult partResult : partResults) {
            if (partResult.isFailed()) {
                ProgressPublisher.publishProgress(listener, ProgressEventType.TRANSFER_PART_FAILED_EVENT);
                throw partResult.getException();
            }
        }

        // The progress tracker publishes the data.
        ProgressPublisher.publishProgress(listener, ProgressEventType.TRANSFER_COMPLETED_EVENT);

        // Complete parts.
        CompleteMultipartUploadResult multipartUploadResult = complete(uploadCheckPoint, uploadFileRequest);
        uploadFileResult.setMultipartUploadResult(multipartUploadResult);

        // check crc64
        if (multipartOperation.getInnerClient().getClientConfiguration().isCrcCheckEnabled()) {
            Long clientCRC = calcObjectCRCFromParts(partResults);
            multipartUploadResult.setClientCRC(clientCRC);
            try {
                OSSUtils.checkChecksum(clientCRC, multipartUploadResult.getServerCRC(), multipartUploadResult.getRequestId());
            } catch (Exception e) {
                ProgressPublisher.publishProgress(listener, ProgressEventType.TRANSFER_FAILED_EVENT);
                throw new InconsistentException(clientCRC, multipartUploadResult.getServerCRC(), multipartUploadResult.getRequestId());
            }
        }

        // The checkpoint is enabled and upload the checkpoint file.
        if (uploadFileRequest.isEnableCheckpoint()) {
            remove(uploadFileRequest.getCheckpointFile());
        }

        return uploadFileResult;
    }

    private static Long calcObjectCRCFromParts(List<PartResult> partResults) {
        long crc = 0;

        for (PartResult partResult : partResults) {
            if (partResult.getPartCRC() == null || partResult.getLength() <= 0) {
                return null;
            }
            crc = CRC64.combine(crc, partResult.getPartCRC(), partResult.getLength());
        }
        return new Long(crc);
    }

    private void prepare(UploadCheckPoint uploadCheckPoint, UploadFileRequest uploadFileRequest) {
        uploadCheckPoint.magic = UploadCheckPoint.UPLOAD_MAGIC;
        uploadCheckPoint.uploadFile = uploadFileRequest.getUploadFile();
        uploadCheckPoint.key = uploadFileRequest.getKey();
        uploadCheckPoint.uploadFileStat = FileStat.getFileStat(uploadCheckPoint.uploadFile);
        uploadCheckPoint.uploadParts = splitFile(uploadCheckPoint.uploadFileStat.size, uploadFileRequest.getPartSize());
        uploadCheckPoint.partETags = new ArrayList<PartETag>();
        uploadCheckPoint.originPartSize = uploadFileRequest.getPartSize();

        ObjectMetadata metadata = uploadFileRequest.getObjectMetadata();
        if (metadata == null) {
            metadata = new ObjectMetadata();
        }

        if (metadata.getContentType() == null) {
            metadata.setContentType(
                    Mimetypes.getInstance().getMimetype(uploadCheckPoint.uploadFile, uploadCheckPoint.key));
        }

        InitiateMultipartUploadRequest initiateUploadRequest = new InitiateMultipartUploadRequest(
                uploadFileRequest.getBucketName(), uploadFileRequest.getKey(), metadata);

        Payer payer = uploadFileRequest.getRequestPayer();
        if (payer != null) {
            initiateUploadRequest.setRequestPayer(payer);
        }

        initiateUploadRequest.setSequentialMode(uploadFileRequest.getSequentialMode());

        InitiateMultipartUploadResult initiateUploadResult = initiateMultipartUploadWrap(uploadCheckPoint, initiateUploadRequest);
        uploadCheckPoint.uploadID = initiateUploadResult.getUploadId();
    }

    private ArrayList<PartResult> upload(UploadCheckPoint uploadCheckPoint, UploadFileRequest uploadFileRequest)
            throws Throwable {
        ArrayList<PartResult> taskResults = new ArrayList<PartResult>();
        ExecutorService service = Executors.newFixedThreadPool(uploadFileRequest.getTaskNum());
        ArrayList<Future<PartResult>> futures = new ArrayList<Future<PartResult>>();
        ProgressListener listener = uploadFileRequest.getProgressListener();

        // Compute the size of the data pending upload.
        long contentLength = 0;
        long completedLength = 0;
        for (int i = 0; i < uploadCheckPoint.uploadParts.size(); i++) {
            long partSize = uploadCheckPoint.uploadParts.get(i).size;
            contentLength += partSize;
            if (uploadCheckPoint.uploadParts.get(i).isCompleted) {
                completedLength += partSize;
            }
        }

        ProgressPublisher.publishRequestContentLength(listener, contentLength);
        ProgressPublisher.publishRequestBytesTransferred(listener, completedLength);
        uploadFileRequest.setProgressListener(null);

        // Upload parts.
        for (int i = 0; i < uploadCheckPoint.uploadParts.size(); i++) {
            if (!uploadCheckPoint.uploadParts.get(i).isCompleted) {
                futures.add(service.submit(new Task(i, "upload-" + i, uploadCheckPoint, i, uploadFileRequest,
                        multipartOperation, listener)));
            } else {
                taskResults.add(new PartResult(i + 1, uploadCheckPoint.uploadParts.get(i).offset,
                        uploadCheckPoint.uploadParts.get(i).size, uploadCheckPoint.uploadParts.get(i).crc));
            }
        }
        service.shutdown();

        // Waiting for parts upload complete.
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        for (Future<PartResult> future : futures) {
            try {
                PartResult tr = future.get();
                taskResults.add(tr);
            } catch (ExecutionException e) {
                uploadFileRequest.setProgressListener(listener);
                throw e.getCause();
            }
        }

        // Sorts PartResult by the part numnber.
        Collections.sort(taskResults, new Comparator<PartResult>() {
            @Override
            public int compare(PartResult p1, PartResult p2) {
                return p1.getNumber() - p2.getNumber();
            }
        });
        uploadFileRequest.setProgressListener(listener);

        return taskResults;
    }

    class Task implements Callable<PartResult> {

        public Task(int id, String name, UploadCheckPoint uploadCheckPoint, int partIndex,
                UploadFileRequest uploadFileRequest, OSSMultipartOperation multipartOperation,
                ProgressListener progressListener) {
            this.id = id;
            this.name = name;
            this.uploadCheckPoint = uploadCheckPoint;
            this.partIndex = partIndex;
            this.uploadFileRequest = uploadFileRequest;
            this.multipartOperation = multipartOperation;
            this.progressListener = progressListener;
        }

        @Override
        public PartResult call() throws Exception {
            PartResult tr = null;
            InputStream instream = null;

            try {
                UploadPart uploadPart = uploadCheckPoint.uploadParts.get(partIndex);
                tr = new PartResult(partIndex + 1, uploadPart.offset, uploadPart.size);

                instream = new FileInputStream(uploadCheckPoint.uploadFile);
                instream.skip(uploadPart.offset);

                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(uploadFileRequest.getBucketName());
                uploadPartRequest.setKey(uploadFileRequest.getKey());
                uploadPartRequest.setUploadId(uploadCheckPoint.uploadID);
                uploadPartRequest.setPartNumber(uploadPart.number);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(uploadPart.size);

                Payer payer = uploadFileRequest.getRequestPayer();
                if (payer != null) {
                    uploadPartRequest.setRequestPayer(payer);
                }
                
                int limit = uploadFileRequest.getTrafficLimit();
                if (limit > 0) {
                    uploadPartRequest.setTrafficLimit(limit);
                }

                UploadPartResult uploadPartResult  = uploadPartWrap(uploadCheckPoint, uploadPartRequest);

                if(multipartOperation.getInnerClient().getClientConfiguration().isCrcCheckEnabled()) {
                    OSSUtils.checkChecksum(uploadPartResult.getClientCRC(), uploadPartResult.getServerCRC(), uploadPartResult.getRequestId());
                    tr.setPartCRC(uploadPartResult.getClientCRC());
                    tr.setLength(uploadPartResult.getPartSize());
                    uploadPart.crc = uploadPartResult.getClientCRC();
                }
                PartETag partETag = new PartETag(uploadPartResult.getPartNumber(), uploadPartResult.getETag());
                uploadCheckPoint.update(partIndex, partETag, true);
                if (uploadFileRequest.isEnableCheckpoint()) {
                    uploadCheckPoint.dump(uploadFileRequest.getCheckpointFile());
                }
                ProgressPublisher.publishRequestBytesTransferred(progressListener, uploadPart.size);
            } catch (Exception e) {
                tr.setFailed(true);
                tr.setException(e);
                logException(String.format("Task %d:%s upload part %d failed: ", id, name, partIndex + 1), e);
            } finally {
                if (instream != null) {
                    instream.close();
                }
            }

            return tr;
        }

        private int id;
        private String name;
        private UploadCheckPoint uploadCheckPoint;
        private int partIndex;
        private UploadFileRequest uploadFileRequest;
        private OSSMultipartOperation multipartOperation;
        private ProgressListener progressListener;
    }

    private CompleteMultipartUploadResult complete(UploadCheckPoint uploadCheckPoint,
            UploadFileRequest uploadFileRequest) {
        Collections.sort(uploadCheckPoint.partETags, new Comparator<PartETag>() {
            @Override
            public int compare(PartETag p1, PartETag p2) {
                return p1.getPartNumber() - p2.getPartNumber();
            }
        });
        CompleteMultipartUploadRequest completeUploadRequest = new CompleteMultipartUploadRequest(
                uploadFileRequest.getBucketName(), uploadFileRequest.getKey(), uploadCheckPoint.uploadID,
                uploadCheckPoint.partETags);
     
        Payer payer = uploadFileRequest.getRequestPayer();
        if (payer != null) {
            completeUploadRequest.setRequestPayer(payer);
        }

        ObjectMetadata metadata = uploadFileRequest.getObjectMetadata();
        if (metadata != null) {
            String acl = (String) metadata.getRawMetadata().get(OSSHeaders.OSS_OBJECT_ACL);
            if (acl != null && !acl.equals("")) {
                CannedAccessControlList accessControlList = CannedAccessControlList.parse(acl);
                completeUploadRequest.setObjectACL(accessControlList);
            }
        }

        completeUploadRequest.setCallback(uploadFileRequest.getCallback());

        return completeMultipartUploadWrap(uploadCheckPoint, completeUploadRequest);
    }

    private ArrayList<UploadPart> splitFile(long fileSize, long partSize) {
        ArrayList<UploadPart> parts = new ArrayList<UploadPart>();

        long partNum = fileSize / partSize;
        if (partNum >= 10000) {
            partSize = fileSize / (10000 - 1);
            partNum = fileSize / partSize;
        }

        for (long i = 0; i < partNum; i++) {
            UploadPart part = new UploadPart();
            part.number = (int) (i + 1);
            part.offset = i * partSize;
            part.size = partSize;
            part.isCompleted = false;
            parts.add(part);
        }

        if (fileSize % partSize > 0) {
            UploadPart part = new UploadPart();
            part.number = parts.size() + 1;
            part.offset = parts.size() * partSize;
            part.size = fileSize % partSize;
            part.isCompleted = false;
            parts.add(part);
        }

        return parts;
    }

    private boolean remove(String filePath) {
        boolean flag = false;
        File file = new File(filePath);

        if (file.isFile() && file.exists()) {
            flag = file.delete();
        }

        return flag;
    }

    protected OSSMultipartOperation multipartOperation;
}
