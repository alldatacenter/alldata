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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CreateSymlinkRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSSymlink;
import com.aliyun.oss.model.ObjectMetadata;

/**
 * Test symlink Link
 */
public class SymlinkTest extends TestBase {
    
    final private static String targetObject = "oss/+< >[]/世界/中国.txt";
    final private static String content = "Hello OSS";

    @Test
    public void testNormalCreateSymlink() {
        final String symLink = "normal-create-sym-link";

        try {
            ossClient.putObject(bucketName, targetObject,
                    new ByteArrayInputStream(content.getBytes()));
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("text/plain");
            metadata.addUserMetadata("property", "property-value");
            
            CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, symLink, targetObject);
            createSymlinkRequest.setMetadata(metadata);
            ossClient.createSymlink(createSymlinkRequest);

            OSSSymlink symbolicLink = ossClient.getSymlink(bucketName, symLink);
            Assert.assertEquals(symbolicLink.getSymlink(), symLink);
            Assert.assertEquals(symbolicLink.getTarget(), targetObject);
            Assert.assertEquals(symbolicLink.getMetadata().getContentType(), "text/plain");
            Assert.assertEquals(symbolicLink.getMetadata().getUserMetadata().get("property"), "property-value");   
            Assert.assertEquals(symbolicLink.getRequestId().length(), REQUEST_ID_LEN);
                        
            ossClient.deleteObject(bucketName, symLink);
            ossClient.deleteObject(bucketName, targetObject);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testNormalCreateSymlinkChar() {
        final String symLink = "normal-create-sym-link-[]< >=-?/世界/中国.txt";

        try {
            ossClient.putObject(bucketName, targetObject,
                    new ByteArrayInputStream(content.getBytes()));

            ossClient.createSymlink(bucketName, symLink, targetObject);

            OSSSymlink symbolicLink = ossClient.getSymlink(bucketName, symLink);
            Assert.assertEquals(symbolicLink.getSymlink(), symLink);
            Assert.assertEquals(symbolicLink.getTarget(), targetObject);
            Assert.assertEquals(symbolicLink.getRequestId().length(), REQUEST_ID_LEN);
                        
            ossClient.deleteObject(bucketName, symLink);
            ossClient.deleteObject(bucketName, targetObject);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testUnnormalCreateSymlink() {
        final String symLink = "unnormal-create-sym-link";

        try {
            ossClient.createSymlink(bucketName, symLink, symLink);
            
            OSSSymlink symbolicLink = ossClient.getSymlink(
                    bucketName, symLink);
            Assert.assertEquals(symbolicLink.getSymlink(), symLink);
            Assert.assertEquals(symbolicLink.getTarget(), symLink);
            
            try {
                ossClient.getObject(bucketName, symLink);
            } catch (OSSException e) {
                Assert.assertEquals("InvalidTargetType", e.getErrorCode());
            }

            ossClient.deleteObject(bucketName, symLink);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testUnnormalgetSymlink() {
        final String symLink = "unnormal-get-sym-link";

        try {
            OSSSymlink symbolicLink = ossClient.getSymlink(bucketName, symLink);
            Assert.assertNull(symbolicLink.getSymlink());
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, e.getErrorCode());
        }
        
        try {
            ossClient.createSymlink(bucketName, symLink, targetObject);
            ossClient.getObject(bucketName, symLink);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_SYM_LINK_TARGET, e.getErrorCode());
        }
    }
    
    @Test
    public void testNormalgetSymlinkContent() {
        final String symLink = "normal-create-sym-link-content";

        try {
            ossClient.putObject(bucketName, targetObject,
                    new ByteArrayInputStream(content.getBytes()));

            ossClient.createSymlink(bucketName, symLink, targetObject);

            OSSSymlink symbolicLink = ossClient.getSymlink(
                    bucketName, symLink);
            Assert.assertEquals(symbolicLink.getSymlink(), symLink);
            Assert.assertEquals(symbolicLink.getTarget(), targetObject);
            Assert.assertEquals(symbolicLink.getRequestId().length(), REQUEST_ID_LEN);

            // content
            OSSObject ossObject = ossClient.getObject(bucketName, symLink);

            StringBuilder contentBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ossObject.getObjectContent()));
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                contentBuilder.append(line);
            }
            reader.close();
            
            Assert.assertEquals(contentBuilder.toString(), content);
            
            // size
            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, symLink);
            Assert.assertEquals(meta.getContentLength(), content.length());

            ossClient.deleteObject(bucketName, symLink);
            ossClient.deleteObject(bucketName, targetObject);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testNormalHeaderSymlink() {
        final String symLink = "normal-create-sym-link-content";

        try {
            Map<String, String> userMeta = new HashMap<String, String>();
            userMeta.put("meta", "my");
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setUserMetadata(userMeta);
            
            ossClient.putObject(bucketName, targetObject,
                    new ByteArrayInputStream(content.getBytes()), metadata);

            ossClient.createSymlink(bucketName, symLink, targetObject);

            ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, symLink);
            Assert.assertNull(meta.getUserMetadata().get("meta"));

            ossClient.deleteObject(bucketName, symLink);
            ossClient.deleteObject(bucketName, targetObject);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
}
