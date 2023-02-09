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

import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.Test;

public class BucketTransferAccelerationTest extends TestBase {

    @Test
    public void testBucketTransferAcceleration() {

        try {
            TransferAcceleration config = ossClient.getBucketTransferAcceleration(bucketName);
            Assert.fail("should not here");
        } catch (OSSException e) {
            Assert.assertEquals("NoSuchTransferAccelerationConfiguration", e.getErrorCode());
        } catch (Exception e1) {
            Assert.fail(e1.getMessage());
        }

        try {
            ossClient.setBucketTransferAcceleration(bucketName, true);
            TransferAcceleration config = ossClient.getBucketTransferAcceleration(bucketName);
            Assert.assertEquals(true, config.isEnabled());

            ossClient.setBucketTransferAcceleration(bucketName, false);
            config = ossClient.getBucketTransferAcceleration(bucketName);
            Assert.assertEquals(false, config.isEnabled());

        } catch (Exception e1) {
            Assert.fail(e1.getMessage());
        }

        try {
            ossClient.deleteBucketTransferAcceleration(bucketName);
            TransferAcceleration config = ossClient.getBucketTransferAcceleration(bucketName);
            Assert.fail("should not here");
        } catch (OSSException e) {
            Assert.assertEquals("NoSuchTransferAccelerationConfiguration", e.getErrorCode());
        } catch (Exception e1) {
            Assert.fail(e1.getMessage());
        }

    }
}
