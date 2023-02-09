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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.InconsistentException;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.GenericResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadFileResult;


public class CRCChecksumTest extends TestBase {
    
    @Test
    public void testPutObjectCRC() {
        String key = "put-object-crc";
        String content = "Hello OSS, Hi OSS, OSS OK.";
        
        try {
            // 测试字符串上传
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, key, 
                    new ByteArrayInputStream(content.getBytes()));
            checkCRC(putObjectResult);
            ossClient.deleteObject(bucketName, key);
            
            // 测试文件上传
            File file = createSampleFile(key, 1024 * 500);
            putObjectResult = ossClient.putObject(bucketName, key, file);
            checkCRC(putObjectResult);
            ossClient.deleteObject(bucketName, key);
            file.delete();
            
            // 测试文件流上传
            file = createSampleFile(key, 1024 * 1000);
            putObjectResult = ossClient.putObject(bucketName, key, new FileInputStream(file));
            checkCRC(putObjectResult);
            ossClient.deleteObject(bucketName, key);
            file.delete();
            
            // 测试网络流上传
            InputStream inputStream = new URL("https://www.aliyun.com/product/oss").openStream();
            putObjectResult = ossClient.putObject(bucketName, key, inputStream);
            checkCRC(putObjectResult);
            ossClient.deleteObject(bucketName, key);
            
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testAppendObjectCRC() {
        String key = "append-object-crc";
        String content = "Hello OSS, Hi OSS.";
        
        try {
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, 
                    new ByteArrayInputStream(content.getBytes())).withPosition(0L);
            
            // 第一次追加
            appendObjectRequest.setInitCRC(0L);
            AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);
            checkCRC(appendObjectResult);
            
            // 第二次追加
            appendObjectRequest = new AppendObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
            appendObjectRequest.setInitCRC(appendObjectResult.getClientCRC());
            appendObjectResult = ossClient.appendObject(appendObjectRequest);
            checkCRC(appendObjectResult);
            
            // 第三次追加
            appendObjectRequest = new AppendObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
            appendObjectRequest.setInitCRC(appendObjectResult.getClientCRC());
            appendObjectResult = ossClient.appendObject(appendObjectRequest);
            checkCRC(appendObjectResult);
            
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testAppendObjectCRCNegative() {
        String key = "append-object-crc-neg";
        String content = "Hello OSS, Hi OSS.";
        
        try {
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, 
                    new ByteArrayInputStream(content.getBytes())).withPosition(0L); 
            appendObjectRequest.setInitCRC(1L);
            
            ossClient.appendObject(appendObjectRequest);
            
            Assert.fail("Append object should not be successful.");
        } catch (Exception e) {
        	Assert.assertTrue(e instanceof InconsistentException);
        	ossClient.deleteObject(bucketName, key);
        	System.out.println(e);
        }
    }
    
    @Test
    public void testMutilUploadCRC() {
        String key = "mutil-upload-object-crc";
        
        try {
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            // 初始化上传任务
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = 
                    new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult initiateMultipartUploadResult = 
                    ossClient.initiateMultipartUpload(initiateMultipartUploadRequest);
            Assert.assertEquals(initiateMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);
            String uploadId = initiateMultipartUploadResult.getUploadId();

            // 上传分片
            for (int i = 0; i < 5 ; i++) {
                long fileLen = 1024 * 100 * (i + 1);
                String filePath = TestUtils.genFixedLengthFile(fileLen);
                UploadPartRequest request = new UploadPartRequest(bucketName, key, uploadId, i + 1, 
                        new FileInputStream(filePath), fileLen);
                UploadPartResult uploadPartResult = ossClient.uploadPart(request);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
                checkCRC(uploadPartResult);
            }
            
            // 提交上传任务，服务器返回整个文件的CRC，客户端没有计算
            CompleteMultipartUploadRequest CompleteMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags); 
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(CompleteMultipartUploadRequest);
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);
            checkCRC(completeMultipartUploadResult);
            
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUploadFileCRC() {
        final String key = "upload-file-crc";

        try {
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setEnableCheckpoint(true);

            UploadFileResult uploadRes = ossClient.uploadFile(uploadFileRequest);
            Assert.assertEquals(uploadRes.getMultipartUploadResult().getClientCRC(), uploadRes.getMultipartUploadResult().getServerCRC());
        } catch (Throwable e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetObjectCRC() {
        String key = "get-object-crc";

        try {
            InputStream inputStream = TestUtils.genFixedLengthInputStream(1024 * 100);
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, key, inputStream);
            checkCRC(putObjectResult);
            
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            Assert.assertNull(ossObject.getClientCRC());
            Assert.assertNotNull(ossObject.getServerCRC());
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            
            InputStream content = ossObject.getObjectContent();
            while (content.read() != -1) {
            }
            
            ossObject.setClientCRC(IOUtils.getCRCValue(content));
            checkCRC(ossObject);
            content.close();
            
            // 范围CRC上不支持
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            getObjectRequest.setRange(100, 10000);
            OSSObject ossRangeObject = ossClient.getObject(getObjectRequest);
            Assert.assertNull(ossRangeObject.getClientCRC());
            Assert.assertNotNull(ossRangeObject.getServerCRC());
            Assert.assertEquals(ossObject.getServerCRC(), ossRangeObject.getServerCRC());
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
                        
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }
    
    @Test
    public void testGetObjectToFileCRC() {
        String key = "get-object-to-file-crc";

        try {
            InputStream inputStream = TestUtils.genFixedLengthInputStream(1024 * 100);
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, key, inputStream);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            checkCRC(putObjectResult);
            
            String filePath = genFixedLengthFile(0);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, new File(filePath));
            
            // 范围CRC上不支持
            getObjectRequest.setRange(100, 10000);
            getObjectRequest = new GetObjectRequest(bucketName, key);
            ossClient.getObject(getObjectRequest, new File(filePath));
                        
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }
    
    @Test
    public void testEmpytObjectCRC() {
        String key = "empty-object-crc";

        try {
            // put
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, key,
                    new ByteArrayInputStream(new String("").getBytes()));
            Assert.assertTrue(putObjectResult.getClientCRC() == 0L);
            Assert.assertTrue(putObjectResult.getServerCRC() == 0L);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            
            String localFile = TestUtils.genFixedLengthFile(0);
            putObjectResult = ossClient.putObject(bucketName, key, new FileInputStream(localFile));
            Assert.assertEquals(putObjectResult.getClientCRC(), putObjectResult.getServerCRC());
            Assert.assertTrue(putObjectResult.getClientCRC() == 0L);
            Assert.assertTrue(putObjectResult.getServerCRC() == 0L);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            
            putObjectResult = ossClient.putObject(bucketName, key, new File(localFile));
            Assert.assertEquals(putObjectResult.getClientCRC(), putObjectResult.getServerCRC());
            Assert.assertTrue(putObjectResult.getClientCRC() == 0L);
            Assert.assertTrue(putObjectResult.getServerCRC() == 0L);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);

            // get
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            Assert.assertNull(ossObject.getClientCRC());
            Assert.assertNotNull(ossObject.getServerCRC());
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            
            Assert.assertTrue(IOUtils.getCRCValue(ossObject.getObjectContent()) == 0L);
            
            InputStream content = ossObject.getObjectContent();
            while (content.read() != -1) {
            }

            ossObject.setClientCRC(IOUtils.getCRCValue(content));
            Assert.assertTrue(putObjectResult.getClientCRC() == 0L);
            Assert.assertTrue(putObjectResult.getServerCRC() == 0L);
            content.close();
            
            ossClient.deleteObject(bucketName, key);
            TestUtils.removeFile(localFile);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    private static <Result extends GenericResult>  void checkCRC(final Result result) {
        Assert.assertNotNull(result.getClientCRC());
        Assert.assertNotNull(result.getServerCRC());
        Assert.assertTrue(result.getClientCRC() != 0L);
        Assert.assertTrue(result.getServerCRC() != 0L);
        Assert.assertEquals(result.getClientCRC(), result.getServerCRC());
    }
    
}
