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
import com.aliyun.oss.model.BucketProcess;
import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.ImageProcess;
import com.aliyun.oss.model.SetBucketProcessRequest;

import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

public class BucketProcesTest extends TestBase {

    @Test
    public void testBucketImageProcessConf() {
        try {      
            // get default
            BucketProcess bucketProcess = ossClient.getBucketProcess(bucketName);
            Assert.assertEquals(bucketProcess.getImageProcess().getCompliedHost(), "Both");
            Assert.assertFalse(bucketProcess.getImageProcess().isSourceFileProtect());
            Assert.assertEquals(bucketProcess.getImageProcess().getSourceFileProtectSuffix(), "");
            Assert.assertEquals(bucketProcess.getImageProcess().getStyleDelimiters(), "");
            Assert.assertEquals(bucketProcess.getImageProcess().getVersion().intValue(), 2);
            Assert.assertEquals(bucketProcess.getImageProcess().isSupportAtStyle(), null);
            Assert.assertEquals(bucketProcess.getRequestId().length(), REQUEST_ID_LEN);
            
            // put 1
            ImageProcess imageProcess = new ImageProcess("Img", true, "jpg,png", "/,-");
            SetBucketProcessRequest request = new SetBucketProcessRequest(bucketName, imageProcess);
            request.setImageProcess(imageProcess);
            ossClient.setBucketProcess(request);

            waitForCacheExpiration(2);

            // get 1
            bucketProcess = ossClient.getBucketProcess(new GenericRequest(bucketName));
            Assert.assertEquals(bucketProcess.getImageProcess().getCompliedHost(), "Img");
            Assert.assertTrue(bucketProcess.getImageProcess().isSourceFileProtect());
            Assert.assertEquals(bucketProcess.getImageProcess().getSourceFileProtectSuffix(), "jpg,png");
            Assert.assertEquals(bucketProcess.getImageProcess().getStyleDelimiters(), "-,/");
            Assert.assertEquals(bucketProcess.getImageProcess().getVersion().intValue(), 2);
            Assert.assertEquals(bucketProcess.getImageProcess().isSupportAtStyle(), null);
            Assert.assertEquals(bucketProcess.getRequestId().length(), REQUEST_ID_LEN);
            
            // put 2
            imageProcess = new ImageProcess("Both", false, "gif", "-");
            request = new SetBucketProcessRequest(bucketName, imageProcess);
            ossClient.setBucketProcess(request);

            waitForCacheExpiration(2);

            // get 2
            bucketProcess = ossClient.getBucketProcess(new GenericRequest(bucketName));
            Assert.assertEquals(bucketProcess.getImageProcess().getCompliedHost(), "Both");
            Assert.assertFalse(bucketProcess.getImageProcess().isSourceFileProtect());
            Assert.assertEquals(bucketProcess.getImageProcess().getSourceFileProtectSuffix(), "");
            Assert.assertEquals(bucketProcess.getImageProcess().getStyleDelimiters(), "-");
            Assert.assertEquals(bucketProcess.getImageProcess().getVersion().intValue(), 2);
            Assert.assertEquals(bucketProcess.getImageProcess().isSupportAtStyle(), null);
            Assert.assertEquals(bucketProcess.getRequestId().length(), REQUEST_ID_LEN);
            
            // put 3
            imageProcess = new ImageProcess("Img", true, "*", "/", true);
            request = new SetBucketProcessRequest(bucketName, imageProcess);
            ossClient.setBucketProcess(request);

            waitForCacheExpiration(2);

            // get 3
            bucketProcess = ossClient.getBucketProcess(new GenericRequest(bucketName));
            Assert.assertEquals(bucketProcess.getImageProcess().getCompliedHost(), "Img");
            Assert.assertTrue(bucketProcess.getImageProcess().isSourceFileProtect());
            Assert.assertEquals(bucketProcess.getImageProcess().getSourceFileProtectSuffix(), "*");
            Assert.assertEquals(bucketProcess.getImageProcess().getStyleDelimiters(), "/");
            Assert.assertEquals(bucketProcess.getImageProcess().getVersion().intValue(), 2);
            Assert.assertEquals(bucketProcess.getImageProcess().isSupportAtStyle(), null);
            Assert.assertEquals(bucketProcess.getRequestId().length(), REQUEST_ID_LEN);
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testBucketImageProcessConfNegative() {
        // bucket not exist
        try {
            ossClient.getBucketProcess("bucket-not-exist");
            Assert.fail("GetBucketImageProcessConf should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }
        
        try {
            ImageProcess conf = new ImageProcess("img", false, "*", "/,-");
            SetBucketProcessRequest request = new SetBucketProcessRequest("bucket-not-exist", conf);
            ossClient.setBucketProcess(request);
            Assert.fail("PutBucketImageProcessConf should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }
        
        // parameter invalid
        try {
            ImageProcess conf = null;
            SetBucketProcessRequest request = new SetBucketProcessRequest(bucketName, conf);
            ossClient.setBucketProcess(request);
            Assert.fail("PutBucketImageProcessConf should not be successful.");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
        
        try {
            ImageProcess conf = new ImageProcess(null, false, "*", "/,-");
            SetBucketProcessRequest request = new SetBucketProcessRequest(bucketName, conf);
            ossClient.setBucketProcess(request);
            Assert.fail("PutBucketImageProcessConf should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
        
        try {
            ImageProcess conf = new ImageProcess("xxx", false, "*", "/,-");
            SetBucketProcessRequest request = new SetBucketProcessRequest(bucketName, conf);
            ossClient.setBucketProcess(request);
            Assert.fail("PutBucketImageProcessConf should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        }
    }

    @Test
    public void setImageProcessClassSetter() {
        ImageProcess imageProcess = new ImageProcess(null, false, null, "/,-");
        imageProcess.setCompliedHost("123");
        imageProcess.setSourceFileProtectSuffix("*");
        imageProcess.setStyleDelimiters("_");

        Assert.assertEquals("123", imageProcess.getCompliedHost());
        Assert.assertEquals("*", imageProcess.getSourceFileProtectSuffix());
        Assert.assertEquals("_", imageProcess.getStyleDelimiters());
    }

}
