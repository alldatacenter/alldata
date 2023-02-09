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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

public class UploadPartFromFileTest extends TestBase {
    
    @Test
    public void testMultipartUploadSample() throws IOException {
        int thrs = 2;
        UploadThread[] uploadThrs = new UploadThread[thrs];
        long fileSize = 1024 * 1024 * 1;
        long partSize = 1024 * 100;
        int  keyNum = 2;
        
        System.out.println("File size " + fileSize + ",part size " + partSize + ",key num " + keyNum);
        
        for (int i = 0; i < thrs; i++) {
            uploadThrs[i] = new UploadThread(i, fileSize, partSize, keyNum);
        }

        // 启动线程
        for (int j = 0; j < thrs; j++) {
            uploadThrs[j].start();
        }

        // 等待线程结束
        for (int j = 0; j < thrs; j++) {
            try {
                uploadThrs[j].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class UploadThread extends Thread {
        private int thrNum;
        private UUID uuid  =  UUID.randomUUID(); 
        @SuppressWarnings("static-access")
        private String fileName = uuid.randomUUID().toString();
        private int partCount = 0;
        private long partSize = 0;
        private long fileLength = 0;
        private File sampleFile = null;
        private String key;
        private long uploads = 0;
        private List<PartETag> partETags = new ArrayList<PartETag>();
        
        public UploadThread(int thrNum,long fileSize, long partSize, int uploads) throws IOException {
            this.thrNum = thrNum;
            this.fileLength = fileSize;
            this.partSize = partSize;
            this.uploads = uploads;
            
            sampleFile = createSampleFile(fileName, fileLength);
            fileLength = sampleFile.length();
            
            partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
            }
        }
        
        @Override
        @SuppressWarnings("static-access")
        public void run() {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println(df.format(new Date()) + " thread " + this.thrNum + " begin to upload");
            
            for (int n = 0; n < uploads; n++) {
                key = uuid.randomUUID().toString();
                String uploadId = claimUploadId();
                
                partETags.clear();
                for (int i = 0; i < partCount; i++) {
                    long startPos = i * partSize;
                    long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                    
                    upload(sampleFile, startPos, curPartSize, i + 1, uploadId);
                    
                    //System.out.println(df.format(new Date()) + " Upload slice " + i);
                }
                
                if (partETags.size() != partCount) {
                    throw new IllegalStateException("Upload multiparts failed, some parts are not finished yet");
                } 
                
                completeMultipartUpload(uploadId);
                
                if (this.thrNum > 10 && uploads > 10) {
                    System.out.println(df.format(new Date()) + " thread " + this.thrNum + " exit to upload");
                    return;
                }
            }
            
            System.out.println(df.format(new Date()) + " thread " + this.thrNum + " completed to upload");
        }
        
        private File createSampleFile(String fileName, long size) throws IOException {
            File file = File.createTempFile(fileName, ".txt");
            file.deleteOnExit();
            String context = "abcdefghijklmnopqrstuvwxyz0123456789011234567890\n";

            Writer writer = new OutputStreamWriter(new FileOutputStream(file));
            for (int i = 0; i < size / context.length() ; i++) {
                writer.write(context);
            }
            writer.close();

            return file;
        }
        
        private String claimUploadId() {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            return result.getUploadId();
        }
        
        private void upload(File localFile, long startPos, long partSize,
                int partNumber, String uploadId) {
            InputStream instream = null;
            try {
                instream = new FileInputStream(localFile);
                instream.skip(startPos);

                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setPartNumber(partNumber);

                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
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

        private void completeMultipartUpload(String uploadId) {
            // Make part numbers in ascending order
            Collections.sort(partETags, new Comparator<PartETag>() {

                @Override
                public int compare(PartETag p1, PartETag p2) {
                    return p1.getPartNumber() - p2.getPartNumber();
                }
            });

            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            ossClient.completeMultipartUpload(completeMultipartUploadRequest);
        }
    }
    
}
