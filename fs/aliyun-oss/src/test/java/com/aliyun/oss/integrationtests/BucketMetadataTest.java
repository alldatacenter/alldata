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

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.BucketMetadata;

public class BucketMetadataTest extends TestBase {

    @Test
    public void testGetBucketMetadata() {
        try {
            BucketMetadata meta = ossClient.getBucketMetadata(bucketName);
            Assert.assertEquals(meta.getBucketRegion(), TestConfig.OSS_TEST_REGION);
            Assert.assertEquals(meta.getHttpMetadata().get(OSSHeaders.OSS_HEADER_REQUEST_ID).length(),
                    "59F2AC3B349A25FA4C44BF8A".length());
        } catch (Exception e) {
        	e.printStackTrace();
            Assert.fail(e.getMessage());
        } 
    }
    
    @Test
    public void testUnormalGetBucketMetadata() {
        final String bucketName = "unormal-get-bucket-meta";
        
        // bucket non-existent 
        try {
            ossClient.getBucketMetadata(bucketName);
            Assert.fail("Get bucket meta should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, e.getErrorCode());
            Assert.assertEquals(e.getRequestId().length(), "59F2AC3B349A25FA4C44BF8A".length());
        }

    }
    
}
