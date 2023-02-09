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

import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import junit.framework.Assert;

public class TrafficLimitTest extends TestBase {
    final static int OBJECT_SIZE_1MB = 1024*1024;
    final static int LIMIT_100KB = 100*1024*8;

    @Test
    public void testPutObject() {
        String key = "traffic-limit-test-put-object";
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;

        try {
            startTimeSec = new Date().getTime()/1000;
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            putObjectRequest.setTrafficLimit(LIMIT_100KB);
            ossClient.putObject(putObjectRequest);
            endTimeSec = new Date().getTime()/1000;

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key); 
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetObject() throws Throwable {
        String key = "traffic-limit-test-get-object";
        // 1MB size.
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;
        File file = new File(key + ".txt");

        try {
            // Prepare 1MB object
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossClient.putObject(putObjectRequest);

            startTimeSec = new Date().getTime()/1000;
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            getObjectRequest.setTrafficLimit(LIMIT_100KB);
            ObjectMetadata meta = ossClient.getObject(getObjectRequest, file);
            Assert.assertEquals(meta.getContentLength(), OBJECT_SIZE_1MB);
            endTimeSec = new Date().getTime()/1000;

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e){
            Assert.fail(e.getMessage());
        } finally {
            file.deleteOnExit();
        }
    }

    @Test
    public void testAppendObject() throws Throwable {
        String key = "traffic-limit-test-append-object";
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;

        try {
            startTimeSec = new Date().getTime()/1000;
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream);
            appendObjectRequest.setTrafficLimit(LIMIT_100KB);
            appendObjectRequest.setPosition(0L);
            ossClient.appendObject(appendObjectRequest);
            endTimeSec = new Date().getTime()/1000;

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUploadFile() throws Throwable {
        String key = "requestpayment-test-upload_file";
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;
        File file = createSampleFile(key, inputStreamLength);

        try {
            startTimeSec = new Date().getTime()/1000;
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setTrafficLimit(LIMIT_100KB);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(1);
            ossClient.uploadFile(uploadFileRequest);
            endTimeSec = new Date().getTime()/1000;

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key);
        } catch(Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadFile() throws Throwable {
        String key = "requestpayment-test-download-file";
        String downFileName = key + "-down.txt";
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;

        try {
            // prepare object
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossClient.putObject(putObjectRequest);

            startTimeSec = new Date().getTime()/1000;
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setTrafficLimit(LIMIT_100KB);
            downloadFileRequest.setDownloadFile(downFileName);
            ossClient.downloadFile(downloadFileRequest);
            endTimeSec = new Date().getTime()/1000;

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key);
        } catch(Exception e){
            Assert.fail(e.getMessage());
        } finally {
            File downFile = new File(downFileName);
            downFile.deleteOnExit(); 
        }
    }

    @Test
    public void testUploadPart() throws Throwable {
        String key = "requestpayment-test-upload-part-object";
        // Create tmp file of 2MB
        File file = createSampleFile(key, OBJECT_SIZE_1MB*2);
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;
        try {
            // Set initiateMultipartUploadRequest with payer setting
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(initiateMultipartUploadRequest);

            String uploadId = upresult.getUploadId();

            // Create partETags
            List<PartETag> partETags =  new ArrayList<PartETag>();

            // Calc partsize
            final long partSize = 1 * 1024 * 1024L;   // 1MB

            long fileLength = file.length();
            int partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
            }
            // Upload part
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                InputStream instream = new FileInputStream(file);
                instream.skip(startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber( i + 1);
                uploadPartRequest.setTrafficLimit(LIMIT_100KB);
                startTimeSec = new Date().getTime()/1000;
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                endTimeSec = new Date().getTime()/1000;

                // Calculate expensed time
                expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

                // Theoretical time is 1MB/100KB = 10s
                BigDecimal theoreticalExpense = new BigDecimal("10");
                BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

                // Compare to minimum theoretical time
                if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                    Assert.fail("calc traffic expensive time is error.");
                }

                partETags.add(uploadPartResult.getPartETag());
            }

            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            ossClient.completeMultipartUpload(completeMultipartUploadRequest);

            ossClient.deleteObject(bucketName, key);
        } catch(Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutObjectPreSignedUrl() {
        String key = "traffic-limit-test-put-object-presigned-url";
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;

        try {
            Date date = new Date(); 
            // Set effictive time limit is 60s
            date.setTime(date.getTime() + 60*1000);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
            request.setExpiration(date);
            request.setTrafficLimit(LIMIT_100KB);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            startTimeSec = new Date().getTime()/1000;
            ossClient.putObject(signedUrl, instream, -1, null, true);
            endTimeSec = new Date().getTime()/1000;

            GenericRequest getObjectMetadataRequest = new GenericRequest(bucketName, key);
            ObjectMetadata objectMetadata = ossClient.getObjectMetadata(getObjectMetadataRequest);
            Assert.assertEquals(objectMetadata.getContentLength(), inputStreamLength);

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key); 
        } catch (Exception e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetObjectPreSignedUrl() {
        String key = "traffic-limit-test-get-object-presigned-url";
        long inputStreamLength = OBJECT_SIZE_1MB;
        long startTimeSec = 0;
        long endTimeSec = 0;
        BigDecimal expenseTimeSec = null;
        String fileName = key + ".txt";
        File file = new File(fileName);

        try {
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossClient.putObject(putObjectRequest);

            Date date = new Date(); 
            // Set effictive time limit is 60s
            date.setTime(date.getTime()+ 60*1000);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
            request.setExpiration(date);
            request.setTrafficLimit(LIMIT_100KB + 5*1024*8);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            startTimeSec = new Date().getTime()/1000;
            GetObjectRequest getObjectRequest =  new GetObjectRequest(signedUrl, null);
            ObjectMetadata objectMeta = ossClient.getObject(getObjectRequest, file);
            endTimeSec = new Date().getTime()/1000;

            // Calculate expensed time
            expenseTimeSec = new BigDecimal(endTimeSec - startTimeSec);

            // Theoretical time is 1MB/100KB = 10s
            BigDecimal theoreticalExpense = new BigDecimal("10");
            BigDecimal theoreticalExpenseMin = theoreticalExpense.multiply(new BigDecimal("0.7"));

            // Compare to minimum theoretical time
            if (expenseTimeSec.compareTo(theoreticalExpenseMin) < 0) {
                Assert.fail("calc traffic expensive time is error.");
            }

            ossClient.deleteObject(bucketName, key); 
        } catch (Exception e){
            Assert.fail(e.getMessage());
        } finally {
            file.deleteOnExit();
        }
    }

}