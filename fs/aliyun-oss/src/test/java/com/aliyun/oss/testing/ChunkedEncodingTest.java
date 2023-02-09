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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;

public class ChunkedEncodingTest {
    
    static final String endpoint = "<valid endpoint>";
    static final String accessId = "<your access id>";
    static final String accessKey = "<your access key>";
        
    static OSS client = new OSSClientBuilder().build(endpoint, accessId, accessKey);
    
    static final String bucketName = "<your bucket name>";
    static final String key = "<object key>";
    static final String filePath = "<file to upload>";
    
    @Ignore
    public void testPutObjectChunked() {
        try {
            File f = new File(filePath);
            FileInputStream fin = new FileInputStream(new File(filePath));
            // Without setting Content-Length of inputstream explicitly, using chunked encoding by default.
            PutObjectResult result = client.putObject(bucketName, key, fin, new ObjectMetadata());
            
            fin = new FileInputStream(f);
            byte[] binaryData = IOUtils.readStreamAsByteArray(fin);
            String actualETag = BinaryUtil.encodeMD5(binaryData);
            String expectedETag = result.getETag();
            Assert.assertEquals(expectedETag, actualETag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Ignore
    public void testPutObjectChunked2() {    
        try {
            Date expiration = DateUtil.parseRfc822Date("Wed, 12 Mar 2015 03:15:00 GMT");
            HttpMethod method = HttpMethod.PUT;
            
            URL signedUrl = client.generatePresignedUrl(bucketName, key, expiration, method);
            File f = new File(filePath);
            FileInputStream fin = new FileInputStream(f);
            Map<String, String> customHeaders = new HashMap<String, String>();
            customHeaders.put("x-oss-meta-author", "aliy");
            customHeaders.put("x-oss-tag", "byurl");
            // Using url signature & chunked encoding to upload specified inputstream.
            PutObjectResult result = client.putObject(signedUrl, fin, f.length(), customHeaders, true);
            
            fin = new FileInputStream(f);
            byte[] binaryData = IOUtils.readStreamAsByteArray(fin);
            String expectedETag = BinaryUtil.encodeMD5(binaryData);
            String actualETag = result.getETag();
            Assert.assertEquals(expectedETag, actualETag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } 
    }
    
    @Ignore
    public void testUploadPartChunked() {
        try {
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = 
                    new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult initiateMultipartUploadResult = 
                    client.initiateMultipartUpload(initiateMultipartUploadRequest);
            String uploadId = initiateMultipartUploadResult.getUploadId();
            
            File file = new File(filePath);
            final int partSize = 5 * 1024 * 1024;
            int fileSize = (int) file.length();
            final int partCount = (file.length() % partSize != 0) ? (fileSize / partSize + 1) : (fileSize / partSize);
            List<PartETag> partETags = new ArrayList<PartETag>();
            
            for (int i = 0; i < partCount; i++) {
                InputStream fin = new BufferedInputStream(new FileInputStream(file));
                fin.skip(i * partSize);
                int size = (i + 1 == partCount) ? (fileSize - i * partSize) : partSize;
                
                UploadPartRequest req = new UploadPartRequest();
                req.setBucketName(bucketName);
                req.setKey(key);
                req.setPartNumber(i + 1);
                req.setPartSize(size);
                req.setUploadId(uploadId);
                req.setInputStream(fin);
                req.setUseChunkEncoding(true);
                
                UploadPartResult result = client.uploadPart(req);
                partETags.add(result.getPartETag());
            }
            
            String expectedETag = calcMultipartETag(partETags);

            CompleteMultipartUploadRequest completeMultipartUploadRequest = 
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult = 
                    client.completeMultipartUpload(completeMultipartUploadRequest);
            
            String actualETag = completeMultipartUploadResult.getETag();
            Assert.assertEquals(expectedETag, actualETag);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } 
    }    
    
    private static String calcMultipartETag(List<PartETag> eTags) {
        StringBuffer concatedEtags = new StringBuffer();
        for (PartETag e : eTags) {
            concatedEtags.append(e.getETag());
        }
        
        String encodedMD5 = BinaryUtil.encodeMD5(concatedEtags.toString().getBytes());
        int partNumber = eTags.size();
        String finalETag = String.format("%s-%d", encodedMD5, partNumber);
        return finalETag;
    }
}
