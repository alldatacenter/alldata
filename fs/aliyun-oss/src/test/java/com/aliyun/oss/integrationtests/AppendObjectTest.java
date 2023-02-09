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

import static com.aliyun.oss.integrationtests.TestConstants.MISSING_ARGUMENT_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.OBJECT_NOT_APPENDABLE_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.POSITION_NOT_EQUAL_TO_LENGTH_ERROR;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;

import java.io.File;
import java.io.InputStream;

import com.aliyun.oss.InconsistentException;
import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.OSSObject;

public class AppendObjectTest extends TestBase {
    
    @Test
    public void testNormalAppendObject() throws Exception {        
        String key = "normal-append-object";
        final long instreamLength = 128 * 1024;
        
        for (int i = 0; i < 10; i++) {
            try {
                // Usage style  1
                InputStream instream = genFixedLengthInputStream(instreamLength);
                AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
                appendObjectRequest.setPosition(2 * i * instreamLength);
                AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);
                OSSObject o = ossClient.getObject(bucketName, key);
                Assert.assertEquals(key, o.getKey());
                Assert.assertEquals((2 * i + 1) * instreamLength, o.getObjectMetadata().getContentLength());
                Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
                Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
                if (appendObjectResult.getNextPosition() != null) {
                    Assert.assertEquals((2 * i + 1) * instreamLength, appendObjectResult.getNextPosition().longValue());
                }
                
                // Usage style 2
                final String filePath = genFixedLengthFile(instreamLength);
                appendObjectRequest = new AppendObjectRequest(bucketName, key, new File(filePath));
                appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
                appendObjectResult = ossClient.appendObject(appendObjectRequest);
                o = ossClient.getObject(bucketName, key);
                Assert.assertEquals(instreamLength * 2 * (i + 1), o.getObjectMetadata().getContentLength());
                Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
                if (appendObjectResult.getNextPosition() != null) {                
                    Assert.assertEquals(instreamLength * 2 * (i + 1), appendObjectResult.getNextPosition().longValue());
                }
                Assert.assertEquals(appendObjectResult.getRequestId().length(), REQUEST_ID_LEN);
                Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            } catch (Exception ex) {
                Assert.fail(ex.getMessage());
            }
        }
    }
    
    @Test
    public void testAppendExistingNormalObject() throws Exception {        
        String key = "append-existing-normal-object";
        final long instreamLength = 128 * 1024;
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            ossClient.putObject(bucketName, key, instream, null);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
            o.getObjectContent().close();
            
            try {
                instream = genFixedLengthInputStream(instreamLength);
                AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
                appendObjectRequest.setPosition(instreamLength);
                ossClient.appendObject(appendObjectRequest);
            } catch (OSSException ex) {
                Assert.assertEquals(OSSErrorCode.OBJECT_NOT_APPENDALBE, ex.getErrorCode());
                Assert.assertTrue(ex.getMessage().startsWith(OBJECT_NOT_APPENDABLE_ERR));
            }
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Test
    public void testAppendObjectAtIllegalPosition() throws Exception {        
        String key = "append-object-at-illlegal-position";
        final long instreamLength = 128 * 1024;
        
        try {
            InputStream instream = genFixedLengthInputStream(instreamLength);
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
            appendObjectRequest.setPosition(0L);
            AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(instreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
            if (appendObjectResult.getNextPosition() != null) {
                Assert.assertEquals(instreamLength, appendObjectResult.getNextPosition().longValue());
            }
            Assert.assertEquals(appendObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            o.getObjectContent().close();
            
            try {
                instream = genFixedLengthInputStream(instreamLength);
                appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
                // Set illegal postion to append, here should be 'instreamLength' rather than other positions.
                appendObjectRequest.setPosition(instreamLength - 1);        
                ossClient.appendObject(appendObjectRequest);
            } catch (OSSException ex) {
                Assert.assertEquals(OSSErrorCode.POSITION_NOT_EQUAL_TO_LENGTH, ex.getErrorCode());
                Assert.assertTrue(ex.getMessage().startsWith(POSITION_NOT_EQUAL_TO_LENGTH_ERROR));
            }
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
    @Test
    public void testAppendObjectMissingArguments() throws Exception {        
        String key = "append-object-missing-arguments";
        final long instreamLength = 128 * 1024;
        
        try {
            Assert.assertEquals(true, ossClient.doesBucketExist(bucketName));
            InputStream instream = genFixedLengthInputStream(instreamLength);
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
            // Missing required parameter 'postition'
            ossClient.appendObject(appendObjectRequest);
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.MISSING_ARGUMENT, ex.getErrorCode());
            Assert.assertTrue(ex.getMessage().startsWith(MISSING_ARGUMENT_ERR));
        }
    }

    @Test
    public void testAppendObjectWithInvalidCRC() throws Exception {
        String key = "append-object-with-invalid-crc";
        final long instreamLength = 128 * 1024;

        try {
            Assert.assertEquals(true, ossClient.doesBucketExist(bucketName));
            InputStream instream = genFixedLengthInputStream(instreamLength);
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, null);
            appendObjectRequest.setPosition((long) 0);
            appendObjectRequest.setInitCRC((long) 123);
            ossClient.appendObject(appendObjectRequest);
            Assert.assertTrue(false);
        } catch (Exception ex) {
            Assert.assertTrue(ex instanceof InconsistentException);
        }
    }
}
