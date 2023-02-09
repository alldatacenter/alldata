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
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.DeleteObjectsResult;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.SimplifiedObjectMeta;
import com.aliyun.oss.model.UploadPartCopyRequest;
import com.aliyun.oss.model.UploadPartCopyResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

public class GetRequestIdTest extends TestBase {
    
    @Test
    public void testNormalGetRequestId() {
        final String key = "normal-get-request-id";
        final long inputStreamLength = 1024;
        final int requestIdLength = "572BF2F2207FB3397648E9F1".length();
        
        try {            
            // put object
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, key, 
                    genFixedLengthInputStream(inputStreamLength));
            Assert.assertEquals(putObjectResult.getRequestId().length(), requestIdLength);
            
            // get object
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            ossObject.getObjectContent().close();
            Assert.assertEquals(ossObject.getRequestId().length(), requestIdLength);
            
            File file = new File("tmp");
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ObjectMetadata objectMeta = ossClient.getObject(getObjectRequest, file);
            Assert.assertEquals(objectMeta.getRequestId().length(), requestIdLength);
            
            // delete object
            ossClient.deleteObject(bucketName, key);
            
            // append object
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, file);
            appendObjectRequest.setPosition(0L);
            AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);
            Assert.assertEquals(appendObjectResult.getRequestId().length(), requestIdLength);

            // getSimplifiedObjectMeta
            SimplifiedObjectMeta simplifiedObjectMeta = ossClient.getSimplifiedObjectMeta(bucketName, key);   
            Assert.assertEquals(simplifiedObjectMeta.getRequestId().length(), requestIdLength);
            
            // getObjectMetadata
            ObjectMetadata objectMetadata = ossClient.getObjectMetadata(bucketName, key);
            Assert.assertEquals(objectMetadata.getRequestId().length(), requestIdLength);
            
            // delete objects
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
            ArrayList<String> keys = new ArrayList<String>();
            keys.add(key);
            deleteObjectsRequest.setKeys(keys);
            DeleteObjectsResult deleteObjectsResult = ossClient.deleteObjects(deleteObjectsRequest);
            Assert.assertEquals(deleteObjectsResult.getRequestId().length(), requestIdLength);
            
            // initiate multipart upload
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = 
                    new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult initiateMultipartUploadResult = 
                    ossClient.initiateMultipartUpload(initiateMultipartUploadRequest);
            Assert.assertEquals(initiateMultipartUploadResult.getRequestId().length(), requestIdLength);
            
            // upload part
            UploadPartRequest uploadPartRequest = new UploadPartRequest(bucketName, key, 
                    initiateMultipartUploadResult.getUploadId(), 1, new FileInputStream(file),
                    inputStreamLength);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            Assert.assertEquals(uploadPartResult.getRequestId().length(), requestIdLength);
            
            // complete multipart upload
            List<PartETag> partETags = new ArrayList<PartETag>();
            partETags.add(new PartETag(1, uploadPartResult.getETag()));
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new 
                    CompleteMultipartUploadRequest(bucketName, key, 
                            initiateMultipartUploadResult.getUploadId(), partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = 
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), requestIdLength);
            
            // copy object
            CopyObjectResult CopyObjectResult = ossClient.copyObject(bucketName, key, bucketName, key);
            Assert.assertEquals(CopyObjectResult.getRequestId().length(), requestIdLength);
            
            // initiate multipart copy
            InitiateMultipartUploadRequest initiateMultipartCopyRequest = 
                    new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult initiateMultipartCopyResult = 
                    ossClient.initiateMultipartUpload(initiateMultipartCopyRequest);
            Assert.assertEquals(initiateMultipartCopyResult.getRequestId().length(), requestIdLength);
            
            // upload part copy
            UploadPartCopyRequest uploadPartCopyRequest = new UploadPartCopyRequest(bucketName, key,
                    bucketName, key, initiateMultipartCopyResult.getUploadId(), 1, 0L, inputStreamLength);
            UploadPartCopyResult uploadPartCopyResult = ossClient.uploadPartCopy(uploadPartCopyRequest);
            Assert.assertEquals(uploadPartCopyResult.getRequestId().length(), requestIdLength);
            
            // abort multipart upload 
            AbortMultipartUploadRequest AbortMultipartUploadRequest = 
                    new AbortMultipartUploadRequest(bucketName, key, initiateMultipartCopyResult.getUploadId());
            ossClient.abortMultipartUpload(AbortMultipartUploadRequest);
            
            ossClient.deleteObject(bucketName, key);
            file.delete();
            
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
}
