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

package com.aliyun.oss.testing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Ignore;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;

public class UrlSignatureTest {

    static final String endpoint = "<valid endpoint>";
    static final String accessId = "<your access id>";
    static final String accessKey = "<your access key>";
        
    static OSS client = new OSSClientBuilder().build(endpoint, accessId, accessKey);
    
    static final String bucketName = "<your bucket name>";
    static final String key = "<object key>";
    static final String filePath = "<file to upload>";
    
    @Ignore
    public void testGetObject() {    
        
        try {
            Date expiration = DateUtil.parseRfc822Date("Thu, 19 Mar 2015 18:00:00 GMT");
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
            request.setExpiration(expiration);
            request.setContentType("application/octet-stream");
            URL signedUrl = client.generatePresignedUrl(request);
            System.out.println("signed url for getObject: " + signedUrl);
            
            Map<String, String> customHeaders = new HashMap<String, String>();
            customHeaders.put("Range", "bytes=100-1000");
            customHeaders.put("Content-Type", "application/octet-stream");
            OSSObject object = client.getObject(signedUrl, customHeaders);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String expectedETag = null;
            try {
                int bufSize = 1024 * 4;
                byte[] buffer = new byte[bufSize];
                int bytesRead;
                while ((bytesRead = object.getObjectContent().read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                expectedETag = BinaryUtil.encodeMD5(outputStream.toByteArray());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            } finally {
                IOUtils.safeClose(outputStream);
                IOUtils.safeClose(object.getObjectContent());
            }
            
            ObjectMetadata metadata = client.getObjectMetadata(bucketName, key);
            String actualETag = metadata.getETag();
            
            Assert.assertEquals(expectedETag, actualETag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Ignore
    public void testPutObject() {    
        OSS client = new OSSClientBuilder().build(endpoint, accessId, accessKey);
        try {
            Date expiration = DateUtil.parseRfc822Date("Thu, 19 Mar 2015 18:00:00 GMT");
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
            request.setExpiration(expiration);
            request.setContentType("application/octet-stream");
            request.addUserMetadata("author", "aliy");
            URL signedUrl = client.generatePresignedUrl(request);
            System.out.println("signed url for putObject: " + signedUrl);
            
            File f = new File(filePath);
            FileInputStream fin = new FileInputStream(f);
            Map<String, String> customHeaders = new HashMap<String, String>();
            customHeaders.put("Content-Type", "application/octet-stream");
            customHeaders.put("x-oss-meta-author", "aliy");
            PutObjectResult result = client.putObject(signedUrl, fin, f.length(), customHeaders);
            
            fin = new FileInputStream(f);
            byte[] binaryData = IOUtils.readStreamAsByteArray(fin);
            String expectedETag = BinaryUtil.encodeMD5(binaryData);
            String actualETag = result.getETag();
            Assert.assertEquals(expectedETag, actualETag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } 
    }
}
