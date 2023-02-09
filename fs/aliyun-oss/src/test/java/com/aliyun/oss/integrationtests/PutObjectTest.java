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

import static com.aliyun.oss.integrationtests.TestConstants.INVALID_DIGEST_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.INVALID_ENCRYPTION_ALGO_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.INVALID_OBJECT_NAME_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.buildObjectKey;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.integrationtests.TestUtils.genRandomLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.removeFile;
import static com.aliyun.oss.integrationtests.TestUtils.removeFiles;
import static com.aliyun.oss.integrationtests.TestUtils.waitAll;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_FILE_SIZE_LIMIT;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_OBJECT_CONTENT_TYPE;
import static com.aliyun.oss.internal.OSSHeaders.OSS_USER_METADATA_PREFIX;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.internal.Mimetypes;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;

public class PutObjectTest extends TestBase {
    
    @Ignore
    public void putSmallFileConcurrently() throws Exception {
        final int threadCount = 100;
        final String keyPrefix = "put-small-file-concurrently-";
        final String filePath = genFixedLengthFile(1 * 1024 * 1024); //1MB
        final AtomicInteger completedCount = new AtomicInteger(0);
        try {    
            Thread[] putThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            ossClient.putObject(bucketName, buildObjectKey(keyPrefix, seqNum), 
                                    new File(filePath));
                            completedCount.incrementAndGet();
                        } catch (Exception ex) {
                            Assert.fail(ex.getMessage());
                        } 
                    }
                };
                
                putThreads[i] = new Thread(r);
            }
            
            waitAll(putThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFile(filePath);
            int totalCompleted = completedCount.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    @Ignore
    public void putMediumFileConcurrently() throws Exception {
        final int threadCount = 100;
        final String keyPrefix = "put-medium-file-concurrently-";
        final String filePath = genFixedLengthFile(256 * 1024 * 1024); // 256MB
        final AtomicInteger completedCount = new AtomicInteger(0);
        
        try {    
            Thread[] putThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            ossClient.putObject(bucketName, buildObjectKey(keyPrefix, seqNum), 
                                    new File(filePath));
                            completedCount.incrementAndGet();
                        } catch (Exception e) {
                            Assert.fail(e.getMessage());
                        } 
                    }
                };
                
                putThreads[i] = new Thread(r);
            }
            
            waitAll(putThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            int totalCompleted = completedCount.get();
            Assert.assertEquals(threadCount, totalCompleted);
            removeFile(filePath);
        }
    }
    
    @Ignore
    public void putLargeFileConcurrently() throws Exception {
        final int threadCount = 10;
        final String keyPrefix = "put-large-file-concurrently-";
        final String filePath = genFixedLengthFile(2 * 1024 * 1024 * 1024); // 2GB
        final AtomicInteger completedCount = new AtomicInteger(0);
        
        try {    
            Thread[] putThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            ossClient.putObject(bucketName, buildObjectKey(keyPrefix, seqNum), 
                                    new File(filePath));
                            completedCount.incrementAndGet();
                        } catch (Exception e) {
                            Assert.fail(e.getMessage());
                        } 
                    }
                };
                
                putThreads[i] = new Thread(r);
            }
            
            waitAll(putThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            int totalCompleted = completedCount.get();
            Assert.assertEquals(threadCount, totalCompleted);
            removeFile(filePath);
        }
    }
    
    @Ignore
    public void testPutRandomScaleFileConcurrently() throws Exception {
        final int threadCount = 100;
        final String keyPrefix = "put-random-scale-file-concurrently-";
        final AtomicInteger completedCount = new AtomicInteger(0);
        List<File> fileList = new ArrayList<File>();
        
        try {    
            Thread[] putThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                final File randomFile = new File(genRandomLengthFile());
                fileList.add(randomFile);
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            ossClient.putObject(bucketName, buildObjectKey(keyPrefix, seqNum), 
                                    randomFile);
                            completedCount.incrementAndGet();
                        } catch (Exception ex) {
                            Assert.fail(ex.getMessage());
                        } 
                    }
                };
                
                putThreads[i] = new Thread(r);
            }
            
            waitAll(putThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            int totalCompleted = completedCount.get();
            Assert.assertEquals(threadCount, totalCompleted);
            removeFiles(fileList);
        }
    }

    @Test
    public void testPutObjectWithCLRF() throws IOException {
        final String keyWithCLRF = "abc\r\ndef";
        final String filePath = genFixedLengthFile(128 * 1024); //128KB
        
        try {
            ossClient.putObject(bucketName, keyWithCLRF, new File(filePath));
            OSSObject o = ossClient.getObject(bucketName, keyWithCLRF);
            Assert.assertEquals(keyWithCLRF, o.getKey());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.INVALID_OBJECT_NAME, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(INVALID_OBJECT_NAME_ERR));
        } finally {
            ossClient.deleteObject(bucketName, keyWithCLRF);
            removeFile(filePath);
        }
    }
    
    @Test
    public void testUnormalPutObject() throws IOException {
        final String key = "unormal-put-object";
        
        // Try to put object into non-existent bucket
        final String nonexistentBucket = "nonexistent-bucket";
        try {
            ossClient.putObject(nonexistentBucket, key, genFixedLengthInputStream(128));
            Assert.fail("Put object should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        } 
        
        // Try to put object into bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.putObject(bucketWithoutOwnership, key, genFixedLengthInputStream(128));
            Assert.fail("Put object should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, ex.getErrorCode());
        } 
        
        // Try to put object with length exceeding max limit(5GB)
        final long contentLength = DEFAULT_FILE_SIZE_LIMIT + 1;
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            ossClient.putObject(bucketName, key, genFixedLengthInputStream(128), metadata);
            Assert.fail("Put object should not be successful");
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        // Set invalid server side encryption
        final String invalidServerSideEncryption = "Invalid-Server-Side-Encryption";
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setServerSideEncryption(invalidServerSideEncryption);
            ossClient.putObject(bucketName, key, genFixedLengthInputStream(128), metadata);
            Assert.fail("Put object should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.INVALID_ENCRYPTION_ALGORITHM_ERROR, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(INVALID_ENCRYPTION_ALGO_ERR));
        }
        
        // Set invalid Content-MD5
        final String invalidContentMD5 = "Invalid-Content-MD5";
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentMD5(invalidContentMD5);
            ossClient.putObject(bucketName, key, genFixedLengthInputStream(128), metadata);
            Assert.fail("Put object should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.INVALID_DIGEST, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(INVALID_DIGEST_ERR));
        }
    }
    
    @Test
    public void testPutObjectChunked() throws Exception {        
        final String key = "put-object-chunked";
        final int instreamLength = 128 * 1024;
        
        InputStream instream = null;
        try {
            instream = genFixedLengthInputStream(instreamLength);
            ossClient.putObject(bucketName, key, instream, null);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Test
    public void testOverridedPutObject() throws Exception {        
        String key = "overrided-put-object";
        final int instreamLength = 128 * 1024;
        
        InputStream instream = null;
        try {
            // Override 1
            instream = genFixedLengthInputStream(instreamLength);
            ossClient.putObject(bucketName, key, instream);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);

            // Override 2
            final String filePath = genFixedLengthFile(instreamLength);
            ossClient.putObject(bucketName, key, new File(filePath));
            Assert.assertEquals(instreamLength, new File(filePath).length());

            // Override 3
            ossClient.putObject(new PutObjectRequest(bucketName, key, new File(filePath)));
            o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);

            new File(filePath).delete();
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Test
    public void testPutObjectByUrlSignature() throws Exception {
        final String key = "put-object-by-urlsignature";
        final String metaKey0 = "author";
        final String metaValue0 = "aliy";
        final String expirationString = "Sun, 12 Apr 2025 12:00:00 GMT";
        final long inputStreamLength = 128 * 1024; //128KB
        
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
        Date expiration = DateUtil.parseRfc822Date(expirationString);
        request.setExpiration(expiration);
        request.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
        request.addUserMetadata(metaKey0, metaValue0);
        URL signedUrl = ossClient.generatePresignedUrl(request);
                
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, DEFAULT_OBJECT_CONTENT_TYPE);
        requestHeaders.put(OSS_USER_METADATA_PREFIX + metaKey0, metaValue0);
        
        // Override 1
        InputStream instream = null;
        try {
            instream = genFixedLengthInputStream(inputStreamLength);
            // Using url signature & chunked encoding to upload specified inputstream.
            ossClient.putObject(signedUrl, instream, -1, requestHeaders, true);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            
            ObjectMetadata metadata = o.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, metadata.getContentType());
            Assert.assertTrue(metadata.getUserMetadata().containsKey(metaKey0));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
        
        // Override 2
        try {
            instream = genFixedLengthInputStream(inputStreamLength);
            // Using url signature encoding to upload specified inputstream.
            ossClient.putObject(signedUrl, instream, -1, requestHeaders);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            
            ObjectMetadata metadata = o.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, metadata.getContentType());
            Assert.assertTrue(metadata.getUserMetadata().containsKey(metaKey0));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } 
        
        // Override 3
        String filePath = genFixedLengthFile(inputStreamLength);
        try {
            // Using url signature encoding to upload specified inputstream.
            ossClient.putObject(signedUrl, filePath, requestHeaders);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            
            ObjectMetadata metadata = o.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, metadata.getContentType());
            Assert.assertTrue(metadata.getUserMetadata().containsKey(metaKey0));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFile(filePath);
        }
        
        // Override 4
        filePath = genFixedLengthFile(inputStreamLength);
        try {
            // Using url signature encoding to upload specified inputstream.
            ossClient.putObject(signedUrl, filePath, requestHeaders, true);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            
            ObjectMetadata metadata = o.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, metadata.getContentType());
            Assert.assertTrue(metadata.getUserMetadata().containsKey(metaKey0));
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFile(filePath);
        }

        //with nullpath
        try {
            ossClient.putObject(signedUrl, null, requestHeaders);
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertTrue(true);
        }
    }
    
    @Test
    public void testContentTypeAutoSetting() throws Exception {        
        final String keyWithSuffix = "abc.jpg";
        final String keyWithoutSuffix = "abc";
        final int instreamLength = 128 * 1024;
        
        InputStream instream = null;
        try {
            instream = genFixedLengthInputStream(instreamLength);
            ossClient.putObject(bucketName, keyWithSuffix, instream);
            OSSObject o = ossClient.getObject(bucketName, keyWithSuffix);
            Assert.assertEquals(keyWithSuffix, o.getKey());
            Assert.assertEquals(Mimetypes.getInstance().getMimetype(keyWithSuffix), 
                    o.getObjectMetadata().getContentType());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            
            instream = genFixedLengthInputStream(instreamLength);
            ossClient.putObject(bucketName, keyWithoutSuffix, instream);
            o = ossClient.getObject(bucketName, keyWithoutSuffix);
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(keyWithoutSuffix, o.getKey());
            Assert.assertEquals(Mimetypes.getInstance().getMimetype(keyWithoutSuffix), 
                    o.getObjectMetadata().getContentType());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
            
            if (instream != null) {
                instream.close();
            }
        }
    }
    
    @Test
    public void testIncorrentSignature() throws Exception {        
        final String key = "incorrent-signature";
        final String metaKey = "mk0 ";
        final String metaVal = "  mv0";
        final String contentTypeWithBlank = "    text/html  ";
        final int instreamLength = 128 * 1024;
        
        InputStream instream = null;
        try {
            instream = genFixedLengthInputStream(instreamLength);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentTypeWithBlank);
            metadata.addUserMetadata(metaKey, metaVal);
            ossClient.putObject(bucketName, key, instream, metadata);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(contentTypeWithBlank.trim(), o.getObjectMetadata().getContentType());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            
            if (instream != null) {
                instream.close();
            }
        }
    }

    @Test
    public void testPutCSVTypeFile() throws Exception {
        final String key = "1.csv";
        final int instreamLength = 128 * 1024;

        InputStream instream = null;
        try {
            instream = genFixedLengthInputStream(instreamLength);
            ossClient.putObject(bucketName, key, instream);

            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(o.getObjectMetadata().getContentType(), "text/csv");
        } catch (Exception e) {
            Assert.fail(e.getMessage());

            if (instream != null) {
                instream.close();
            }
        }
    }


    @Test
    public void testPutObjectRequestClassSetter() {
        String key = "test-put-object-request-class-setter";
        final int instreamLength = 128 * 1024;
        try {
            final String filePath = genFixedLengthFile(instreamLength);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("text/plain");
            metadata.addUserMetadata("property", "property-value");

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, (File) null);
            putObjectRequest.setFile(new File(filePath));
            putObjectRequest.setMetadata(metadata);
            putObjectRequest.setProcess("process");

            ossClient.putObject(putObjectRequest);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(o.getObjectMetadata().getContentType(), "text/plain");
            Assert.assertEquals(o.getObjectMetadata().getUserMetadata().get("property"), "property-value");
            Assert.assertEquals("process", putObjectRequest.getProcess());

            new File(filePath).delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
