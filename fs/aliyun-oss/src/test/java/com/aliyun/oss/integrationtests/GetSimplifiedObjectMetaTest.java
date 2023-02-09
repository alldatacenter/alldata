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

import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_KEY_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import junit.framework.Assert;

import org.junit.Test;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.SimplifiedObjectMeta;

public class GetSimplifiedObjectMetaTest extends TestBase {
    
    @Test
    public void testNormalGetSimplifiedObjectMeta() {
        final String key = "normal-get-simplified-object-meta";
        final long inputStreamLength = 1024;
        
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, 
                    genFixedLengthInputStream(inputStreamLength), null);
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject o = ossClient.getObject(getObjectRequest);
            Assert.assertEquals(bucketName, o.getBucketName());
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            o.getObjectContent().close();
            
            SimplifiedObjectMeta objectMeta = ossClient.getSimplifiedObjectMeta(bucketName, key);
            Assert.assertEquals(inputStreamLength, objectMeta.getSize());
            Assert.assertEquals(putObjectResult.getETag(), objectMeta.getETag());
            Assert.assertNotNull(objectMeta.getLastModified());
            Assert.assertNotNull(objectMeta.toString());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testUnormalGetSimplifiedObjectMeta() throws Exception {
        // Try to get simplified object meta under nonexistent bucket
        final String key = "unormal-get-simplified-object-meta";
        final String nonexistentBucket = "nonexistent-bukcet";
        try {
            ossClient.getSimplifiedObjectMeta(nonexistentBucket, key);
            Assert.fail("Get simplified object meta should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, ex.getErrorCode());
        }

        // Try to get nonexistent object
        final String nonexistentKey = "nonexistent-object";
        try {
            ossClient.getSimplifiedObjectMeta(bucketName, nonexistentKey);
            Assert.fail("Get simplified object meta should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, ex.getErrorCode());
        }
        
        // Forbidden
        OSS client = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, 
                TestConfig.OSS_TEST_ACCESS_KEY_SECRET + " ");
        try {
            client.getSimplifiedObjectMeta(bucketName, nonexistentKey);
            Assert.fail("Get simplified object meta should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, ex.getErrorCode());
        } finally {
            client.shutdown();
        }

    }
}
