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

import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;
import static com.aliyun.oss.model.DeleteObjectsRequest.DELETE_OBJECTS_ONETIME_LIMIT;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;

public class DeleteObjectsTest extends TestBase {
    
    @Test
    public void testDeleleExistingObjects() {
        List<String> existingKeys = new ArrayList<String>();
        final int keyCount = 100;
        final String keyPrefix = "delete-existing-objects";
        for (int i = 0; i < keyCount; i++) {
            existingKeys.add(keyPrefix + i);
        }
        
        if (!batchPutObject(ossClient, bucketName, existingKeys)) {
            Assert.fail("batch put object failed");
        }
        
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        request.setKeys(existingKeys);
        try {
            DeleteObjectsResult result = ossClient.deleteObjects(request);
            List<String> deletedObjects = result.getDeletedObjects();
            Assert.assertEquals(keyCount, deletedObjects.size());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testDeleleNonexistentObjects() {
        List<String> nonexistentKeys = new ArrayList<String>();
        final int keyCount = 100;
        final String keyPrefix = "delete-nonexistent-objects";
        for (int i = 0; i < keyCount; i++) {
            nonexistentKeys.add(keyPrefix + i);
        }
        
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        request.setKeys(nonexistentKeys);
        try {
            DeleteObjectsResult result = ossClient.deleteObjects(request);
            List<String> deletedObjects = result.getDeletedObjects();
            Assert.assertEquals(keyCount, deletedObjects.size());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testDeleleNullOrEmptyObjects() {
        List<String> emptyKeys = new ArrayList<String>();
        
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        try {
            request.setKeys(emptyKeys);
            ossClient.deleteObjects(request);
            Assert.fail("Delete objects should not be successfully");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
        
        List<String> withNullKeys = new ArrayList<String>();
        withNullKeys.add("dummykey");
        withNullKeys.add(null);
        try {
            request.setKeys(withNullKeys);
            ossClient.deleteObjects(request);
            Assert.fail("Delete objects should not be successfully");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }
    
    @Test
    public void testDeleleObjectsExceedLimit() {
        List<String> existingKeys = new ArrayList<String>();
        final int keyCount = DELETE_OBJECTS_ONETIME_LIMIT + 1;
        final String keyPrefix = "delete-objects-exceed-limit";
        for (int i = 0; i < keyCount; i++) {
            existingKeys.add(keyPrefix + i);
        }
        
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        try {
            request.setKeys(existingKeys);
            ossClient.deleteObjects(request);
            Assert.fail("Delete objects should not be successfully");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
        }
    }
    
    @Test
    public void testDeleleObjectsQuietly() {
        List<String> existingKeys = new ArrayList<String>();
        final int keyCount = 100;
        final String keyPrefix = "delete-objects-quietly";
        for (int i = 0; i < keyCount; i++) {
            existingKeys.add(keyPrefix + i);
        }
        
        if (!batchPutObject(ossClient, bucketName, existingKeys)) {
            Assert.fail("batch put object failed");
        }
        
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        request.setQuiet(true);
        request.setKeys(existingKeys);
        try {
            DeleteObjectsResult result = ossClient.deleteObjects(request);
            List<String> deletedObjects = result.getDeletedObjects();
            Assert.assertEquals(0, deletedObjects.size());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Ignore
    public void testDeleteObjectsWithEncodingType() {
        final String objectPrefix = "object-with-special-characters-";
        
        try {
            // Add several objects with special characters into bucket
            List<String> existingKeys = new ArrayList<String>();
            existingKeys.add(objectPrefix + "\001\007");
            existingKeys.add(objectPrefix + "\002\007");
            
            if (!batchPutObject(ossClient, bucketName, existingKeys)) {
                Assert.fail("batch put object failed");
            }
            
            // List objects under nonempty bucket
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setEncodingType(DEFAULT_ENCODING_TYPE);
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            List<String> returnedKeys = new ArrayList<String>();
            for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                String decodedKey = URLDecoder.decode(s.getKey(), "UTF-8");
                returnedKeys.add(decodedKey);
                Assert.assertTrue(existingKeys.contains(decodedKey));
            }
            Assert.assertEquals(existingKeys.size(), objectListing.getObjectSummaries().size());
            
            // Delete multiple objects
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
            deleteObjectsRequest.setEncodingType(DEFAULT_ENCODING_TYPE);
            deleteObjectsRequest.setKeys(returnedKeys);
            deleteObjectsRequest.setQuiet(false);
            DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(deleteObjectsRequest);
            Assert.assertEquals(DEFAULT_ENCODING_TYPE, deleteObjectsResult.getEncodingType());
            Assert.assertEquals(existingKeys.size(), deleteObjectsResult.getDeletedObjects().size());
            for (String o : deleteObjectsResult.getDeletedObjects()) {
                String decodedKey = URLDecoder.decode(o, "UTF-8");
                Assert.assertTrue(existingKeys.contains(decodedKey));
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
