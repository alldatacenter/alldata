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

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.event.ProgressEvent;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.SimplifiedObjectMeta;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

public class ProgressBarTest extends TestBase {
    
    static class PutObjectProgressListener implements ProgressListener {

        private long bytesWritten = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        
        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            long bytes = progressEvent.getBytes();
            ProgressEventType eventType = progressEvent.getEventType();
            switch (eventType) {
            case TRANSFER_STARTED_EVENT:
                System.out.println("Start to upload......");
                break;
            
            case REQUEST_CONTENT_LENGTH_EVENT:
                this.totalBytes = bytes;
                System.out.println(this.totalBytes + " bytes in total will be uploaded to OSS");
                break;
            
            case REQUEST_BYTE_TRANSFER_EVENT:
                this.bytesWritten += bytes;
                if (this.totalBytes != -1) {
                    int percent = (int)(this.bytesWritten * 100.0 / this.totalBytes);
                    System.out.println(bytes + " bytes have been written at this time, upload progress: " +
                            percent + "%(" + this.bytesWritten + "/" + this.totalBytes + ")");
                } else {
                    System.out.println(bytes + " bytes have been written at this time, upload ratio: unknown" +
                            "(" + this.bytesWritten + "/...)");
                }
                break;
                
            case TRANSFER_COMPLETED_EVENT:
                this.succeed = true;
                System.out.println("Succeed to upload, " + this.bytesWritten + " bytes have been transferred in total");
                break;
                
            case TRANSFER_FAILED_EVENT:
                System.out.println("Failed to upload, " + this.bytesWritten + " bytes have been transferred");
                break;
                
            default:
                break;
            }
        }

        public boolean isSucceed() {
            return succeed;
        }
    }
    
    static class GetObjectProgressListener implements ProgressListener {
        
        private long bytesRead = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        
        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            long bytes = progressEvent.getBytes();
            ProgressEventType eventType = progressEvent.getEventType();
            switch (eventType) {
            case TRANSFER_STARTED_EVENT:
                System.out.println("Start to download......");
                break;
            
            case RESPONSE_CONTENT_LENGTH_EVENT:
                this.totalBytes = bytes;
                System.out.println(this.totalBytes + " bytes in total will be downloaded to a local file");
                break;
            
            case RESPONSE_BYTE_TRANSFER_EVENT:
                this.bytesRead += bytes;
                if (this.totalBytes != -1) {
                    int percent = (int)(this.bytesRead * 100.0 / this.totalBytes);
                    System.out.println(bytes + " bytes have been read at this time, download progress: " +
                            percent + "%(" + this.bytesRead + "/" + this.totalBytes + ")");
                } else {
                    System.out.println(bytes + " bytes have been read at this time, download ratio: unknown" +
                            "(" + this.bytesRead + "/...)");
                }
                break;
                
            case TRANSFER_COMPLETED_EVENT:
                this.succeed = true;
                System.out.println("Succeed to download, " + this.bytesRead + " bytes have been transferred in total");
                break;
                
            case TRANSFER_FAILED_EVENT:
                System.out.println("Failed to download, " + this.bytesRead + " bytes have been transferred");
                break;
                
            default:
                break;
            }
        }
        
        public boolean isSucceed() {
            return succeed;
        }
    }
    
    static class UploadPartProgressListener implements ProgressListener {
        
        private int partNumber;
        
        private long bytesWritten = 0;
        private long totalBytes = -1;
        private boolean succeed = false;
        
        public UploadPartProgressListener(int partNumber) {
            this.partNumber = partNumber;
        }
        
        @Override
        public void progressChanged(ProgressEvent progressEvent) {
            long bytes = progressEvent.getBytes();
            ProgressEventType eventType = progressEvent.getEventType();
            switch (eventType) {
            case TRANSFER_PART_STARTED_EVENT:
                System.out.println("Start to upload part #" + this.partNumber + " ......");
                break;
            
            case REQUEST_CONTENT_LENGTH_EVENT:
                this.totalBytes = bytes;
                System.out.println("part #" + this.partNumber + " with " + this.totalBytes + " bytes will be uploaded to OSS");
                break;
            
            case REQUEST_BYTE_TRANSFER_EVENT:
                this.bytesWritten += bytes;
                if (this.totalBytes != -1) {
                    int percent = (int)(this.bytesWritten * 100.0 / this.totalBytes);
                    System.out.println(bytes + " bytes have been written at this time, part #" + this.partNumber + 
                            "upload progress: " + percent + "%(" + this.bytesWritten + "/" + this.totalBytes + ")");
                } else {
                    System.out.println(bytes + " bytes have been written at this time, part #" + this.partNumber + 
                            "upload ratio: unknown(" + this.bytesWritten + "/...)");
                }
                break;
                
            case TRANSFER_PART_COMPLETED_EVENT:
                this.succeed = true;
                System.out.println("Succeed to upload part #" + this.partNumber +", " + this.bytesWritten + 
                        " bytes have been transferred in total");
                break;
                
            case TRANSFER_PART_FAILED_EVENT:
                System.out.println("Failed to upload part #" + this.partNumber +", " + this.bytesWritten + 
                        " bytes have been transferred");
                break;
                
            default:
                break;
            }
        }
        
        public boolean isSucceed() {
            return succeed;
        }
    }
    
    @Test
    public void testSimpleObjectProgressBar() throws Exception {        
        final String key = "put-object-progress-bar";
        final int instreamLength = 64 * 1024;
        final String filePath = TestUtils.buildFilePath();
        
        try {
            File fileToUpload = new File(genFixedLengthFile(instreamLength));
            ossClient.putObject(new PutObjectRequest(bucketName, key, fileToUpload).
                    <PutObjectRequest>withProgressListener(new PutObjectProgressListener()));
            ObjectMetadata metadata = ossClient.getObject(new GetObjectRequest(bucketName, key).
                    <GetObjectRequest>withProgressListener(new GetObjectProgressListener()), new File(filePath));
            Assert.assertEquals(instreamLength, metadata.getContentLength());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Ignore
    public void testMultipartsProgressBar() throws Exception {
        final String key = "multiparts-progress-bar";
        final int instreamLength = 64 * 1024 * 1024;
        
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<PartETag> partETags = Collections.synchronizedList(new ArrayList<PartETag>());

        String uploadId = TestUtils.claimUploadId(ossClient, bucketName, key);
        
        final long partSize = 5 * 1024 * 1024L;   // 5MB
        final File tempFile = new File(genFixedLengthFile(instreamLength));
        long fileLength = tempFile.length();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }
        
        for (int i = 0; i < partCount; i++) {
            long startPos = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
            executorService.execute(new PartUploader(
                    key, tempFile, startPos, curPartSize, i + 1, uploadId, partETags, 
                    new UploadPartProgressListener(i + 1)));
        }
        
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(partCount, partETags.size());
 
        Collections.sort(partETags, new Comparator<PartETag>() {

            @Override
            public int compare(PartETag p1, PartETag p2) {
                return p1.getPartNumber() - p2.getPartNumber();
            }
        });
        
        try {
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            ossClient.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testDownloadFileProgressBar() throws Throwable {        
        final String key = "download-file-progress-bar";
        final int instreamLength = 4 * 1024 * 1024;
        final String localFile = genFixedLengthFile(instreamLength);
        
        try {
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(localFile);
            uploadFileRequest.setPartSize(1 * 1024 * 1024);
            uploadFileRequest.setTaskNum(3);
            uploadFileRequest.setProgressListener(new PutObjectProgressListener());
            
            ossClient.uploadFile(uploadFileRequest);
            
            SimplifiedObjectMeta metadata = ossClient.getSimplifiedObjectMeta(bucketName, key);            
            Assert.assertEquals(instreamLength, metadata.getSize());
            
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(localFile + "-df.tmp");
            downloadFileRequest.setPartSize(1 * 1024 * 1024);
            downloadFileRequest.setTaskNum(3);
            downloadFileRequest.setProgressListener(new GetObjectProgressListener());
            
            ossClient.downloadFile(downloadFileRequest);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }
    
    private static class PartUploader implements Runnable {
        
        private String key;
        private File localFile;
        private long startPos;        
        
        private long partSize;
        private int partNumber;
        private String uploadId;
        
        private List<PartETag> partETags;
        private ProgressListener listener;
        
        public PartUploader(String key, File localFile, long startPos, long partSize, int partNumber, String uploadId,
                List<PartETag> partETags, ProgressListener listener) {
            this.key = key;
            this.localFile = localFile;
            this.startPos = startPos;
            this.partSize = partSize;
            this.partNumber = partNumber;
            this.uploadId = uploadId;
            this.partETags = partETags;
            this.listener = listener;
        }
        
        @Override
        public void run() {
            InputStream instream = null;
            try {
                instream = new FileInputStream(this.localFile);
                instream.skip(this.startPos);
                
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(this.key);
                uploadPartRequest.setUploadId(this.uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(this.partSize);
                uploadPartRequest.setPartNumber(this.partNumber);
                uploadPartRequest.setProgressListener(this.listener);
                
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                synchronized (this.partETags) {
                    this.partETags.add(uploadPartResult.getPartETag());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } 
    }
    
}
