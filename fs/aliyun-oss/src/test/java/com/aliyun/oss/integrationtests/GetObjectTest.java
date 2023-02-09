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

import static com.aliyun.oss.integrationtests.TestConstants.NOT_MODIFIED_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_KEY_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.buildObjectKey;
import static com.aliyun.oss.integrationtests.TestUtils.ensureDirExist;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.integrationtests.TestUtils.genRandomLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.removeFile;
import static com.aliyun.oss.integrationtests.TestUtils.removeFiles;
import static com.aliyun.oss.integrationtests.TestUtils.waitAll;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_OBJECT_CONTENT_TYPE;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;

public class GetObjectTest extends TestBase {

    @Ignore
    public void testGetSmallFileConcurrently() throws Exception {
        final int threadCount = 100;
        final String keyPrefix = "get-small-file-concurrently-";
        final String filePath = genFixedLengthFile(1 * 1024); // 1KB
        final AtomicInteger completedCounter = new AtomicInteger(0);
        
        // Put small files concurrently
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
                            completedCounter.incrementAndGet();
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
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
        
        // Reset completed counter.
        completedCounter.set(0);
        
        // Get small files concurrently
        ensureDirExist(DOWNLOAD_DIR);
        final List<File> downloadFiles = new ArrayList<File>();
        final ReentrantLock lock = new ReentrantLock();
        try {    
            Thread[] getThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            String key = buildObjectKey(keyPrefix, seqNum);
                            GetObjectRequest request = new GetObjectRequest(bucketName, key);
                            File file = new File(DOWNLOAD_DIR + key);
                            file.createNewFile();
                            try {
                                lock.lock();
                                downloadFiles.add(file);
                            } finally {
                                lock.unlock();
                            }
                            ossClient.getObject(request, file);
                            completedCounter.incrementAndGet();
                        } catch (Exception ex) {
                            Assert.fail(ex.getMessage());
                        } 
                    }
                };
                
                getThreads[i] = new Thread(r);
            }
            
            waitAll(getThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFiles(downloadFiles);
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    @Ignore
    public void testGetMediumFileConcurrently() throws Exception {
        final int threadCount = 100;
        final String keyPrefix = "get-medium-file-concurrently-";
        final String filePath = genFixedLengthFile(256 * 1024 * 1024); // 256MB
        final AtomicInteger completedCounter = new AtomicInteger(0);
        
        // Put medium files concurrently
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
                            completedCounter.incrementAndGet();
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
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
        
        // Reset completed counter.
        completedCounter.set(0);
        
        // Get medium files concurrently
        ensureDirExist(DOWNLOAD_DIR);
        final List<File> downloadFiles = new ArrayList<File>();
        final ReentrantLock lock = new ReentrantLock();
        try {    
            Thread[] getThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            String key = buildObjectKey(keyPrefix, seqNum);
                            GetObjectRequest request = new GetObjectRequest(bucketName, key);
                            File file = new File(DOWNLOAD_DIR + key);
                            file.createNewFile();
                            try {
                                lock.lock();
                                downloadFiles.add(file);
                            } finally {
                                lock.unlock();
                            }
                            ossClient.getObject(request, file);
                            completedCounter.incrementAndGet();
                        } catch (Exception ex) {
                            Assert.fail(ex.getMessage());
                        } 
                    }
                };
                
                getThreads[i] = new Thread(r);
            }
            
            waitAll(getThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFiles(downloadFiles);
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    @Ignore
    public void testGetLargeFileConcurrently() throws Exception {
        final int threadCount = 10;
        final String keyPrefix = "get-large-file-concurrently-";
        final String filePath = genFixedLengthFile(2 * 1024 * 1024 * 1024); // 2GB
        final AtomicInteger completedCounter = new AtomicInteger(0);
        
        // Put large files concurrently
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
                            completedCounter.incrementAndGet();
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
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
        
        // Reset completed counter
        completedCounter.set(0);
        
        // Get large files concurrently
        ensureDirExist(DOWNLOAD_DIR);
        final List<File> downloadFiles = new ArrayList<File>();
        final ReentrantLock lock = new ReentrantLock();
        try {    
            Thread[] getThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            String key = buildObjectKey(keyPrefix, seqNum);
                            GetObjectRequest request = new GetObjectRequest(bucketName, key);
                            File file = new File(DOWNLOAD_DIR + key);
                            file.createNewFile();
                            try {
                                lock.lock();
                                downloadFiles.add(file);
                            } finally {
                                lock.unlock();
                            }
                            ossClient.getObject(request, file);
                            completedCounter.incrementAndGet();
                        } catch (Exception ex) {
                            Assert.fail(ex.getMessage());
                        } 
                    }
                };
                
                getThreads[i] = new Thread(r);
            }
            
            waitAll(getThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFiles(downloadFiles);
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    @Ignore
    public void testGetRandomScaleFileConcurrently() throws Exception {
        final int threadCount = 100;
        final String keyPrefix = "get-random-scale-file-concurrently-";
        final AtomicInteger completedCounter = new AtomicInteger(0);
        List<File> fileList = new ArrayList<File>();
        
        // Put random scale files concurrently
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
                            completedCounter.incrementAndGet();
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
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
            removeFiles(fileList);
        }
        
        // Reset completed counter.
        completedCounter.set(0);
        
        // Get random scale files concurrently
        ensureDirExist(DOWNLOAD_DIR);
        final List<File> downloadFiles = new ArrayList<File>();
        final ReentrantLock lock = new ReentrantLock();
        try {    
            Thread[] getThreads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            String key = buildObjectKey(keyPrefix, seqNum);
                            GetObjectRequest request = new GetObjectRequest(bucketName, key);
                            File file = new File(DOWNLOAD_DIR + key);
                            file.createNewFile();
                            try {
                                lock.lock();
                                downloadFiles.add(file);
                            } finally {
                                lock.unlock();
                            }
                            ossClient.getObject(request, file);
                            completedCounter.incrementAndGet();
                        } catch (Exception ex) {
                            Assert.fail(ex.getMessage());
                        } 
                    }
                };
                
                getThreads[i] = new Thread(r);
            }
            
            waitAll(getThreads);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            removeFiles(downloadFiles);
            int totalCompleted = completedCounter.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    @Test
    public void testGetObjectByRange() {
        final String key = "get-object-by-range";
        final long inputStreamLength = 128 * 1024; //128KB
        
        try {
            ossClient.putObject(bucketName, key, genFixedLengthInputStream(inputStreamLength), null);
            
            // Normal range [a-b]
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            getObjectRequest.setRange(0, inputStreamLength / 2 - 1);
            OSSObject o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(inputStreamLength / 2, o.getObjectMetadata().getContentLength());
            
            // Start to [a-]
            getObjectRequest.setRange(inputStreamLength / 2, -1);
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(inputStreamLength / 2, o.getObjectMetadata().getContentLength());
            
            // To end [-b]
            getObjectRequest.setRange(-1, inputStreamLength / 4);
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(inputStreamLength / 4, o.getObjectMetadata().getContentLength());
            
            // To end [-b] (b = 0)
            try {
                getObjectRequest.setRange(-1, 0);
                o = ossClient.getObject(getObjectRequest);
                Assert.fail("Get object should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.INVALID_RANGE, e.getErrorCode());
            }
            
            // Invalid range [-1, -1]
            getObjectRequest.setRange(-1, -1);
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            
            // Invalid range start > end, ignore it and just get entire object
            getObjectRequest.setRange(inputStreamLength / 2, inputStreamLength / 4);
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            
            // Invalid range exceeding object's max length, ignore it and just get entire object
            getObjectRequest.setRange(0, inputStreamLength * 2 - 1);
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testOverridedGetObject() {
        final String key = "overrided-get-object";
        final long inputStreamLength = 128 * 1024; //128KB
        
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, 
                    genFixedLengthInputStream(inputStreamLength), null);
            ossClient.putObject(putObjectRequest);
            
            // Override 1
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(bucketName, o.getBucketName());
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            
            // Override 2
            o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(bucketName, o.getBucketName());
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            
            // Override 3
            final String filePath = genFixedLengthFile(0);
            ObjectMetadata metadata = ossClient.getObject(getObjectRequest, new File(filePath));
            Assert.assertEquals(inputStreamLength, metadata.getContentLength());
            Assert.assertEquals(inputStreamLength, new File(filePath).length());
            
            metadata = ossClient.getObjectMetadata(bucketName, key);
            Assert.assertEquals(inputStreamLength, metadata.getContentLength());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetObjectWithSpecialChars() {
        final String key = "测\\r试-中.~,+\"'*&￥#@%！（文）+字符|？/.zip";
        final long inputStreamLength = 128 * 1024; //128KB
        //TODO: With chinese characters will be failed. 
        final String metaKey0 = "tag";
        final String metaValue0 = "元值0";
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            metadata.addUserMetadata(metaKey0, metaValue0);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, 
                    genFixedLengthInputStream(inputStreamLength), metadata);
            ossClient.putObject(putObjectRequest);
            
            // Override 1
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(bucketName, o.getBucketName());
            Assert.assertEquals(key, o.getKey());
            metadata = o.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, metadata.getContentType());
            Assert.assertEquals(metaValue0, metadata.getUserMetadata().get(metaKey0));
            Assert.assertEquals(inputStreamLength, metadata.getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testGetObjectByUrlsignature() {    
        final String key = "put-object-by-urlsignature";
        final String expirationString = "Sun, 12 Apr 2025 12:00:00 GMT";
        final long inputStreamLength = 128 * 1024; //128KB
        final long firstByte= inputStreamLength / 2;
        final long lastByte = inputStreamLength - 1;
        
        try {
            ossClient.putObject(bucketName, key, genFixedLengthInputStream(inputStreamLength), null);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
            Date expiration = DateUtil.parseRfc822Date(expirationString);
            request.setExpiration(expiration);
            request.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            URL signedUrl = ossClient.generatePresignedUrl(request);
            
            Map<String, String> requestHeaders = new HashMap<String, String>();
            requestHeaders.put(HttpHeaders.RANGE, String.format("bytes=%d-%d", firstByte, lastByte));
            requestHeaders.put(HttpHeaders.CONTENT_TYPE, DEFAULT_OBJECT_CONTENT_TYPE);
            OSSObject o = ossClient.getObject(signedUrl, requestHeaders);
            
            try {
                int bytesRead = -1;
                int totalBytes = 0;
                byte[] buffer = new byte[4096];
                while ((bytesRead = o.getObjectContent().read(buffer)) != -1) {
                    totalBytes += bytesRead;
                }
              
                Assert.assertEquals((lastByte - firstByte + 1), totalBytes);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            } finally {
                IOUtils.safeClose(o.getObjectContent());
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testUnormalGetObject() throws Exception {
        final Date beforeModifiedTime = new Date();
        Thread.sleep(1000);
        
        // Try to get object under nonexistent bucket
        final String key = "unormal-get-object";
        final String nonexistentBucket = "nonexistent-bukcet";
        try {
            ossClient.getObject(nonexistentBucket, key);
            Assert.fail("Get object should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }
        
        // Try to get nonexistent object
        final String nonexistentKey = "nonexistent-object";
        try {
            ossClient.getObject(bucketName, nonexistentKey);
            Assert.fail("Get object should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(NO_SUCH_KEY_ERR));
        }
        
        String eTag = null;
        try {
            PutObjectResult result = ossClient.putObject(bucketName, key, genFixedLengthInputStream(1024), null);
            eTag = result.getETag();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        Thread.sleep(2000);

        // Matching ETag Constraints
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        List<String> matchingETagConstraints = new ArrayList<String>();
        OSSObject o = null;
        matchingETagConstraints.add(eTag);
        getObjectRequest.setMatchingETagConstraints(matchingETagConstraints);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(eTag, o.getObjectMetadata().getETag());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            getObjectRequest.setMatchingETagConstraints(null);
        }
        
        matchingETagConstraints.clear();
        matchingETagConstraints.add("nonmatching-etag");
        getObjectRequest.setMatchingETagConstraints(matchingETagConstraints);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.fail("Get object should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.PRECONDITION_FAILED, e.getErrorCode());
        } finally {
            getObjectRequest.setMatchingETagConstraints(null);
        }
        
        // Non-Matching ETag Constraints
        List<String> nonmatchingETagConstraints = new ArrayList<String>();
        nonmatchingETagConstraints.add("nonmatching-etag");
        getObjectRequest.setNonmatchingETagConstraints(nonmatchingETagConstraints);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(eTag, o.getObjectMetadata().getETag());
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            getObjectRequest.setNonmatchingETagConstraints(null);
        }
        
        nonmatchingETagConstraints.clear();
        nonmatchingETagConstraints.add(eTag);
        getObjectRequest.setNonmatchingETagConstraints(nonmatchingETagConstraints);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.fail("Get object should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NOT_MODIFIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NOT_MODIFIED_ERR));
        } finally {
            getObjectRequest.setNonmatchingETagConstraints(null);
        }

        // Unmodified Since Constraint
        Date unmodifiedSinceConstraint = new Date();
        getObjectRequest.setUnmodifiedSinceConstraint(unmodifiedSinceConstraint);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(eTag, o.getObjectMetadata().getETag());
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            getObjectRequest.setUnmodifiedSinceConstraint(null);
        }

        unmodifiedSinceConstraint = beforeModifiedTime;
        getObjectRequest.setUnmodifiedSinceConstraint(unmodifiedSinceConstraint);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.fail("Get object should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.PRECONDITION_FAILED, e.getErrorCode());
        } finally {
            getObjectRequest.setUnmodifiedSinceConstraint(null);
        }

        // Modified Since Constraint
        Date modifiedSinceConstraint = beforeModifiedTime;
        getObjectRequest.setModifiedSinceConstraint(modifiedSinceConstraint);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(eTag, o.getObjectMetadata().getETag());
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            getObjectRequest.setModifiedSinceConstraint(null);
        }
        
        modifiedSinceConstraint = new Date();
        getObjectRequest.setModifiedSinceConstraint(modifiedSinceConstraint);
        try {
            o = ossClient.getObject(getObjectRequest);
            Assert.fail("Get object should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NOT_MODIFIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NOT_MODIFIED_ERR));
        } finally {
            getObjectRequest.setModifiedSinceConstraint(null);
        }
    }
    
    @Test
    public void testGetObjectMetadataWithIllegalExpires() {
        final String key = "get-object-with-illegal-expires";
        final long inputStreamLength = 128 * 1024; //128KB
        final String illegalExpires = "2015-10-01 00:00:00";
        
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setHeader(OSSHeaders.EXPIRES, illegalExpires);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, 
                    genFixedLengthInputStream(inputStreamLength), metadata);
            ossClient.putObject(putObjectRequest);
            
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject o = ossClient.getObject(getObjectRequest);
            try {
                o.getObjectMetadata().getExpirationTime();
                Assert.fail("Get expiration time should not be successful.");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof ParseException);
                Assert.assertEquals("Unparseable date: \"2015-10-01 00:00:00\"", e.getMessage());
            }
            
            String rawExpiresValue = o.getObjectMetadata().getRawExpiresValue();
            Assert.assertEquals(illegalExpires, rawExpiresValue);
            
            metadata = ossClient.getObjectMetadata(bucketName, key);
            try {
                metadata.getExpirationTime();
                Assert.fail("Get expiration time should not be successful.");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof ParseException);
                Assert.assertEquals("Unparseable date: \"2015-10-01 00:00:00\"", e.getMessage());
            }
            
            rawExpiresValue = o.getObjectMetadata().getRawExpiresValue();
            Assert.assertEquals(illegalExpires, rawExpiresValue);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
