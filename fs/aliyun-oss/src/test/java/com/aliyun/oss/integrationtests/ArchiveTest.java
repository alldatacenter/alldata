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

import com.aliyun.oss.model.RestoreObjectResult;
import junit.framework.Assert;

import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.removeFile;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.StorageClass;

public class ArchiveTest extends TestBase {

    @Test
    public void testNormalCreateArchiveBucket() {
        String bucketName = "create-archive-test-bucket";
        String key = "normal-create-archive.txt";
        String filePath = null;
        
        try {
            // create archive bucket
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setStorageClass(StorageClass.Archive);
            ossClient.createBucket(createBucketRequest);
            
            // put archive object
            filePath = genFixedLengthFile(1024);
            ossClient.putObject(bucketName, key, new File(filePath));
            
            // delete object
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            removeFile(filePath);
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Ignore
    public void testNormalRestoreObject() {
        String bucketName = "restore-object-test-bucket";
        String key = "normal-restore-object.txt";
        String filePath = null;
        
        try {
            // create archive bucket
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setStorageClass(StorageClass.Archive);
            ossClient.createBucket(createBucketRequest);
            
            // put archive object
            filePath = genFixedLengthFile(1024);
            ossClient.putObject(bucketName, key, new File(filePath));
            
            ObjectMetadata objectMetadata = ossClient.getObjectMetadata(bucketName, key);
            
            // check whether the object is archive class
            StorageClass storageClass = objectMetadata.getObjectStorageClass();
            if (storageClass == StorageClass.Archive) {
                // restore object
                ossClient.restoreObject(bucketName, key);
                
                // wait for restore completed
                do {
                    Thread.sleep(1000);
                    objectMetadata = ossClient.getObjectMetadata(bucketName, key);
                    System.out.println("x-oss-restore:" + objectMetadata.getObjectRawRestore());
                } while (!objectMetadata.isRestoreCompleted());
            }
            
            // get restored object
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            ossObject.getObjectContent().close();
            
            // restore repeatedly
            ossClient.restoreObject(bucketName, key);
            
            // delete object
            ossClient.deleteObject(bucketName, key);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            removeFile(filePath);
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalOutofRestore() {
        String bucketName = "unnormal-restore-test-bucket";
        String key = "unnormal-restore-object.txt";
        String filePath = null;
        
        try {
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setStorageClass(StorageClass.Archive);
            ossClient.createBucket(createBucketRequest);
            
            filePath = genFixedLengthFile(1024);
            ossClient.putObject(bucketName, key, new File(filePath));
            
            try {
                ossClient.getObject(bucketName, key);
                Assert.fail("Restore object should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.INVALID_OBJECT_STATE, e.getErrorCode());
            }    
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            removeFile(filePath);
            ossClient.deleteObject(bucketName, key);
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testRestoreResultClassSetter() {
        // update coverage.
        RestoreObjectResult result = new RestoreObjectResult(200);
        result.setStatusCode(400);
        Assert.assertEquals(400, result.getStatusCode());
    }

}
