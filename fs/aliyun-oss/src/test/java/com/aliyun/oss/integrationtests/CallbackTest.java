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

import static com.aliyun.oss.integrationtests.TestUtils.claimUploadId;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.internal.OSSUtils;
import com.aliyun.oss.model.Callback;
import com.aliyun.oss.model.Callback.CalbackBodyType;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

/**
 * Test callBack of PutObject and MultipartUpload
 * 
 */
@SuppressWarnings("deprecation")
public class CallbackTest extends TestBase {
    
    private static final String callbackUrl = TestConfig.CALLBACK_URL;

    private static final int instreamLength = 1024;
    private static final int bufferLength = 1024;
    private static final String callbackResponse = "{\"Status\":\"OK\"}";
    
    /**
     * Testing default value settings. Only url and body are specified, others use default values.
     */
    @Test
    public void testPutObjectCallbackDefault() throws Exception {        
        String key = "put-callback-default";
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackBody("put-object-callback");
            putObjectRequest.setCallback(callback);
            
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            byte[] buffer = new byte[bufferLength];
            int nRead = putObjectResult.getCallbackResponseBody().read(buffer);
            putObjectResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());
            obj.forcedClose();

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * test variable in callback body，type of callback body is url.
     */
    @Test
    public void testPutObjectCallbackBody() throws Exception {        
        String key = "put-callback-body";
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("bucket=${bucket}&object=${object}&etag=${etag}&size=${size}&mimeType=${mimeType}&imageInfo.height=${imageInfo.height}&imageInfo.width=${imageInfo.width}&imageInfo.format=${imageInfo.format}&my_var=${x:my_var}");
            callback.setCalbackBodyType(CalbackBodyType.URL);
            putObjectRequest.setCallback(callback);
            
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            byte[] buffer = new byte[bufferLength];
            int nRead = putObjectResult.getCallbackResponseBody().read(buffer);
            putObjectResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
            
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());
            obj.close();

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body type，type of callback body is json
     */
    @Test
    public void testPutObjectCallbackBodyType() throws Exception {        
        String key = "put-callback-body-type";
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("{\\\"mimeType\\\":${mimeType},\\\"size\\\":${size}}");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            putObjectRequest.setCallback(callback);
            
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            byte[] buffer = new byte[bufferLength];
            int nRead = putObjectResult.getCallbackResponseBody().read(buffer);
            putObjectResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
            
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body type，type of callback body is json
     */
    @Test
    public void testPutObjectCallbackVar() throws Exception {        
        String key = "put-callback-var";
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("{\\\"mimeType\\\":${mimeType},\\\"size\\\":${size}}");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            callback.addCallbackVar("x:var1", "value1");
            callback.addCallbackVar("x:var2", "value2");
            putObjectRequest.setCallback(callback);
            
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            byte[] buffer = new byte[bufferLength];
            int nRead = putObjectResult.getResponse().getContent().read(buffer);
            putObjectResult.getResponse().getContent().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
            
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    
    /**
     * callback body var has special characters, type is json.
     */
    @Test
    public void testPutObjectCallbacURLChar() throws Exception {        
        String key = "put-callback-url-char";
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("春水碧于天，画船听雨眠。垆边人似月，皓腕凝霜雪。");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            callback.addCallbackVar("x:键值1", "值1：凌波不过横塘路，但目送，芳尘去。");
            callback.addCallbackVar("x:键值2", "值2：长记曾携手处，千树压、西湖寒碧。");
            putObjectRequest.setCallback(callback);
            
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            byte[] buffer = new byte[bufferLength];
            int nRead = putObjectResult.getCallbackResponseBody().read(buffer);
            putObjectResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
            
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body var has special characters, type is json.
     */
    @Test
    public void testPutObjectCallbacJsonChar() throws Exception {        
        String key = "put-callback-json-char";
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("{\\\"上片\\\":\\\"夏日好，月色白如雪。\\\" ,\\\"下片\\\":\\\"东山照欢会，西山照离别。 夏日好，花月有清阴。\\\"}");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            callback.addCallbackVar("x:键值1", "值1：凌波不过横塘路，但目送，芳尘去。");
            callback.addCallbackVar("x:键值2", "值2：长记曾携手处，千树压、西湖寒碧。");
            putObjectRequest.setCallback(callback);
            
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            byte[] buffer = new byte[bufferLength];
            int nRead = putObjectResult.getCallbackResponseBody().read(buffer);
            putObjectResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
            
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * PutObject callback negative case，Invalid argument.
     */
    @Test
    public void testPutObjectCallbackParamInvalid() {
        String key = "put-callback-negative";
        
        // callbackUrl不合法，地址不合法、port不合法、超过5个
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl("my");
            callback.setCallbackBody("put-object-callback");
            putObjectRequest.setCallback(callback);
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl("my.com:test");
            callback.setCallbackBody("put-object-callback");
            putObjectRequest.setCallback(callback);
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl("my1.com;my2.com;my3.com;my4.com;my5.com;my6.com;");
            callback.setCallbackBody("put-object-callback");
            putObjectRequest.setCallback(callback);
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        // callbackBody is empty
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackBody("");
            putObjectRequest.setCallback(callback);
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        // callbackBody var format is invalid.
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackBody("bucket=${bucket}&object=$(object)");
            putObjectRequest.setCallback(callback);
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        // callback-var parameter's length is more than 5K
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackBody("bucket=${bucket}&object=${object}");
            putObjectRequest.setCallback(callback);
            
            char[] bigArr = new char[1024];
            Arrays.fill(bigArr, 0, 1024, 'A');
            for (int i = 0; i < 10; i++) {
                callback.addCallbackVar("x:var" + i, new String(bigArr));
            }
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        // Callback failed. Expect return 203 (CallbackFailed).
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream); 
            
            Callback callback = new Callback();
            callback.setCallbackUrl("http://www.ifeng.com/");
            callback.setCallbackBody("put-object-callback");
            putObjectRequest.setCallback(callback);
            
            ossClient.putObject(putObjectRequest);
            
            Assert.fail("PutObject callback should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.CALLBACK_FAILED, e.getErrorCode());
        }
    }
    

    /**
     * Tests default values with specified url and body.
     */
    @Test
    public void testMultipartUploadCallbackDefault() {        
        String key = "multipart-upload-callback-default";
        
        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(instreamLength);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(instreamLength);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackBody("upload-object-callback");
            
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setCallback(callback);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                        
            byte[] buffer = new byte[bufferLength];
            int nRead = completeMultipartUploadResult.getCallbackResponseBody().read(buffer);
            completeMultipartUploadResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body has variables, type is url
     */
    @Test  
    public void testMultipartUploadCallbackBody() {  
      String key = "multipart-upload-callback-body";
        
        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(instreamLength);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(instreamLength);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("bucket=${bucket}&object=${object}&etag=${etag}&size=${size}&mimeType=${mimeType}&imageInfo.height=${imageInfo.height}&imageInfo.width=${imageInfo.width}&imageInfo.format=${imageInfo.format}&my_var=${x:my_var}");
            callback.setCalbackBodyType(CalbackBodyType.URL);
            
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setCallback(callback);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                        
            byte[] buffer = new byte[bufferLength];
            int nRead = completeMultipartUploadResult.getResponse().getContent().read(buffer);
            completeMultipartUploadResult.getResponse().getContent().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());
            obj.forcedClose();

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body type，type is json
     */
    @Test  
    public void testMultipartUploadCallbackBodyType() {  
      String key = "multipart-upload-callback-body-type";
        
        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(instreamLength);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(instreamLength);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("{\\\"mimeType\\\":${mimeType},\\\"size\\\":${size}}");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setCallback(callback);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                        
            byte[] buffer = new byte[bufferLength];
            int nRead = completeMultipartUploadResult.getCallbackResponseBody().read(buffer);
            completeMultipartUploadResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());
            obj.close();

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback var
     */
    @Test  
    public void testMultipartUploadCallbackVar() {  
      String key = "multipart-upload-callback-var";
        
        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(instreamLength);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(instreamLength);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("{\\\"mimeType\\\":${mimeType},\\\"size\\\":${size}}");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            callback.addCallbackVar("x:var1", "value1");
            callback.addCallbackVar("x:var2", "value2");
            
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setCallback(callback);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                        
            byte[] buffer = new byte[bufferLength];
            int nRead = completeMultipartUploadResult.getCallbackResponseBody().read(buffer);
            completeMultipartUploadResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body/var has special characters, type is url
     */
    @Test  
    public void testMultipartUploadCallbackURLChar() {  
      String key = "multipart-upload-callback-url-char";
        
        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(instreamLength);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(instreamLength);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("春水碧于天，画船听雨眠。垆边人似月，皓腕凝霜雪。");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            callback.addCallbackVar("x:键值1", "值1：凌波不过横塘路，但目送，芳尘去。");
            callback.addCallbackVar("x:键值2", "值2：长记曾携手处，千树压、西湖寒碧。");
            
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setCallback(callback);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                        
            byte[] buffer = new byte[bufferLength];
            int nRead = completeMultipartUploadResult.getCallbackResponseBody().read(buffer);
            completeMultipartUploadResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    /**
     * callback body/var has special characters, type is json.
     */
    @Test  
    public void testMultipartUploadCallbackJsonChar() {  
      String key = "multipart-upload-callback-json-char";
        
        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(instreamLength);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(instreamLength);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
            
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("{\\\"上片\\\":\\\"夏日好，月色白如雪。\\\" ,\\\"下片\\\":\\\"东山照欢会，西山照离别。 夏日好，花月有清阴。\\\"}");
            callback.setCalbackBodyType(CalbackBodyType.JSON);
            callback.addCallbackVar("x:键值1", "值1：凌波不过横塘路，但目送，芳尘去。");
            callback.addCallbackVar("x:键值2", "值2：长记曾携手处，千树压、西湖寒碧。");
            
            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setCallback(callback);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                        
            byte[] buffer = new byte[bufferLength];
            int nRead = completeMultipartUploadResult.getCallbackResponseBody().read(buffer);
            completeMultipartUploadResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
                    
            OSSObject obj = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, obj.getKey());
            Assert.assertEquals(instreamLength, obj.getObjectMetadata().getContentLength());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Test  
    public void testGeneratePresignedUrlWithCallback() {  
      String key = "generate-presigned-url-callback";
        
        try {
        	// callback 
            Callback callback = new Callback();
            callback.setCallbackUrl(callbackUrl);
            callback.setCallbackHost("oss-cn-hangzhou.aliyuncs.com");
            callback.setCallbackBody("bucket=${bucket}&object=${object}&etag=${etag}&size=${size}&"
            		+ "mimeType=${mimeType}&my_var1=${x:var1}&my_var2=${x:var2}");
            callback.addCallbackVar("x:var1", "value1");
            callback.addCallbackVar("x:var2", "value2");
            callback.setCalbackBodyType(CalbackBodyType.URL);
        	
            // generate put url
            Map<String, String> cbHeaders = new HashMap<String, String>();  
            OSSUtils.populateRequestCallback(cbHeaders, callback);
            
            Date expiration = new Date(new Date().getTime() + 3600 * 1000);
			GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
			request.setExpiration(expiration);
			request.setContentType("text/plain");
			request.setHeaders(cbHeaders);
			request.addHeader("x-oss-meta-author", "mingdi");
			
			URL signedUrl = ossClient.generatePresignedUrl(request);
			System.out.println("SignedUrl:" + signedUrl);
            
        	// put with url
			Map<String, String> customHeaders = new HashMap<String, String>(cbHeaders);
			customHeaders.put("Content-Type", "text/plain");
			customHeaders.put("x-oss-meta-author", "mingdi");
			
        	InputStream instream = genFixedLengthInputStream(instreamLength);
        	PutObjectResult putResult = ossClient.putObject(signedUrl, instream, instreamLength, customHeaders);
        	
        	// check callback body
            byte[] buffer = new byte[bufferLength];
            int nRead = putResult.getCallbackResponseBody().read(buffer);
            putResult.getCallbackResponseBody().close();
            Assert.assertEquals(callbackResponse, new String(buffer, 0, nRead));
            
            // get object and check
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, ossObject.getKey());
            Assert.assertEquals(instreamLength, ossObject.getObjectMetadata().getContentLength());
            Assert.assertEquals("mingdi", ossObject.getObjectMetadata().getUserMetadata().get("author"));
            ossObject.getObjectContent().close();
            
        } catch (Exception ex) {
        	ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }
    
}
