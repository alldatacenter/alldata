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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import org.apache.http.protocol.HTTP;
import org.junit.Test;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.crypto.CryptoHeaders;
import com.aliyun.oss.crypto.EncryptionMaterials;
import com.aliyun.oss.crypto.MultipartUploadCryptoContext;
import com.aliyun.oss.crypto.SimpleRSAEncryptionMaterials;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.utils.ResourceUtils;
import junit.framework.Assert;

public class EncryptionClientRsaTest extends TestBase {
    private OSSEncryptionClient ossEncryptionClient;
    private Map<String, String> matDesc;
    private KeyPair keyPair;

    public void setUp() throws Exception {
        super.setUp();
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024);
        keyPair = keyGenerator.generateKeyPair();
        matDesc = new HashMap<String, String>();
        matDesc.put("Desc1-Key1", "Desc1-Value1");
        EncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair, matDesc);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);

        ossEncryptionClient = new OSSEncryptionClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT,
                new DefaultCredentialProvider(credentials), encryptionMaterials);
    }

    public void tearDown() throws Exception {
        if (ossEncryptionClient != null) {
            ossEncryptionClient.shutdown();
            ossEncryptionClient = null;
        }
        super.tearDown();
    }

    @Test
    public void testPutAndGetFromInputStream() {
        try {
            String key = "encryption-client-test-put-get-inputstream";
            final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";

            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossEncryptionClient.putObject(req);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            String readContent = readOSSObject(ossObject);
            Assert.assertEquals(content, readContent.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutObjectWithContentMD5() {
        String key = "encryption-client-test-put-with-md5";
        final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";

        try {
            String virtualMd5 = "1234";
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentMD5(virtualMd5);
            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()), metadata);

            Assert.assertEquals(req.getMetadata().getContentMD5(), virtualMd5);
            ossEncryptionClient.putObject(req);
            Assert.assertEquals(req.getMetadata().getContentMD5(), null);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            String unencryptedMd5 = (String)ossObject.getObjectMetadata().getUserMetadata().get(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_MD5);
            Assert.assertEquals(virtualMd5, unencryptedMd5);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }


        try {
            String virtualMd5 = "1234";
            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            req.getHeaders().put(OSSHeaders.CONTENT_MD5, virtualMd5);

            Assert.assertTrue(req.getHeaders().containsKey(OSSHeaders.CONTENT_MD5));
            ossEncryptionClient.putObject(req);
            Assert.assertFalse(req.getHeaders().containsKey(OSSHeaders.CONTENT_MD5));

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            String unencryptedMd5 = (String) ossObject.getObjectMetadata().getUserMetadata()
                    .get(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_MD5);
            Assert.assertEquals(virtualMd5, unencryptedMd5);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutObjectWithContentLength() {
        String key = "encryption-client-test-put-with-content-length";
        final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";

        try {
            long contentLength = content.length();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()),
                    metadata);
            ossEncryptionClient.putObject(req);
            Assert.assertEquals(req.getMetadata().getContentLength(), contentLength);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            String unencryptedContentLengh = (String) ossObject.getObjectMetadata().getUserMetadata()
                    .get(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_LENGTH);
            Assert.assertEquals(String.valueOf(contentLength), unencryptedContentLengh);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            long contentLength = content.length();
            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            req.getHeaders().put(OSSHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            ossEncryptionClient.putObject(req);
            Assert.assertEquals(String.valueOf(contentLength), req.getHeaders().get(OSSHeaders.CONTENT_LENGTH));

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            long resContentLength = ossObject.getObjectMetadata().getContentLength();
            String unencryptedContentLengh = (String) ossObject.getObjectMetadata().getUserMetadata()
                    .get(CryptoHeaders.CRYPTO_UNENCRYPTION_CONTENT_LENGTH);
            Assert.assertEquals(String.valueOf(contentLength), unencryptedContentLengh);
            Assert.assertEquals(contentLength, resContentLength);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetUnEncryptedObject() {
        try {
            String key = "test-get-unencryted-object";
            final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";

            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.putObject(req);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            String readContent = readOSSObject(ossObject);
            Assert.assertEquals(content, readContent.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutGetObjectWithFile() {
        try {
            String key = "encryption-client-test-put-get-file.txt";
            String filePathNew = key + "-new.txt";
            File file = createSampleFile(key, 1024 * 500);

            PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
            ossEncryptionClient.putObject(req);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossEncryptionClient.getObject(getObjectRequest, new File(filePathNew));

            File fileNew = new File(filePathNew);
            Assert.assertTrue("comparte file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetObjectToFileWithRangeGet() {
        try {
            String key = "encryption-client-test-get-file-range-get";
            String filePathNew = key + "-new.txt";
            File file = createSampleFile(key, 1024 * 500);

            PutObjectRequest req = new PutObjectRequest(bucketName, key, file);
            ossEncryptionClient.putObject(req);
            int rangeLower = 1321;
            int rangeUpper = 1500;
            int range = rangeUpper - rangeLower + 1;
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            getObjectRequest.setRange(rangeLower, rangeUpper);
            ossEncryptionClient.getObject(getObjectRequest, new File(filePathNew));

            InputStream is1 = new FileInputStream(file);
            byte[] buffer1 = new byte[1024];
            is1.skip(rangeLower);
            int len1 = is1.read(buffer1, 0, range);
            is1.close();
            Assert.assertEquals(range, len1);

            File fileNew = new File(filePathNew);
            InputStream is2 = new FileInputStream(fileNew);
            byte[] buffer2 = new byte[1024];
            int len2 = is2.read(buffer2);
            is2.close();
            Assert.assertEquals(range, len2);
            Assert.assertTrue(Arrays.equals(buffer1, buffer2));
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetObjectWithRangeGet() {
        try {
            final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875vydrdrrdrwdfdfds"
                    + "qwertyuiopdfghjklcvbnm,rertyufghcvb";
            Assert.assertEquals(content.length(), 117);
            int maxRange = content.length() - 1;

            try {
                checkRangeGet(content, 20, 10);
                Assert.fail("range[0] > range[1] is not allowed.");
            } catch (ClientException e) {
                // Excepted excption.
            }

            try {
                checkRangeGet(content, -1, 3);
                Assert.fail("range[0] < 0 is not allowed");
            } catch (ClientException e) {
                // Excepted excption.
            }

            try {
                checkRangeGet(content, 20, -1);
                Assert.fail("range[1] < 0 is not allowed");
            } catch (ClientException e) {
                // Excepted excption.
            }

            checkRangeGet(content, 40, maxRange - 20);
            checkRangeGet(content, 40, maxRange);
            checkRangeGet(content, 40, maxRange + 100);
            checkRangeGet(content, maxRange, maxRange + 100);
            checkRangeGet(content, maxRange + 100, maxRange + 200);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void checkRangeGet(final String content, final int lowRange, final int highRange) throws Throwable {
        // oss encryption client do range-get
        String key = "encrytion-client-test-range-get-" + String.valueOf(lowRange) + "~" + String.valueOf(highRange);
        PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
        ossEncryptionClient.putObject(req);
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        getObjectRequest.setRange(lowRange, highRange);
        OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
        String result1 = readOSSObject(ossObject);

        // oss client do range-get
        key = "normal-client-test-range-get-" + String.valueOf(lowRange) + "~" + String.valueOf(highRange);
        req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
        ossClient.putObject(req);
        getObjectRequest = new GetObjectRequest(bucketName, key);
        getObjectRequest.setRange(lowRange, highRange);
        ossObject = ossClient.getObject(getObjectRequest);
        String result2 = readOSSObject(ossObject);

        Assert.assertEquals(result2, result1);
    }

    private String readOSSObject(OSSObject ossObject) throws Throwable {
        BufferedReader reader = new BufferedReader(new InputStreamReader(ossObject.getObjectContent()));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
        return buffer.toString();
    }

    @Test
    public void testUploadPart() throws Throwable {
        String key = "encryption-client-rsa-test-upload-part-object";
        File file = createSampleFile(key, 500 * 1024);

        long fileLength = file.length();
        final long partSize = 100 * 1024L; // 100K
        MultipartUploadCryptoContext context = new MultipartUploadCryptoContext();
        context.setPartSize(partSize);
        context.setDataSize(fileLength);
        // Upload part by ossEncryptionClient
        try { 
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
            InitiateMultipartUploadResult upresult = ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);
            String uploadId = upresult.getUploadId();

            Assert.assertEquals(context.getUploadId(), uploadId);
            Assert.assertEquals(context.getPartSize(), partSize);
            Assert.assertNotNull(context.getUploadId());
            Assert.assertNotNull(context.getContentCryptoMaterial());

            // Create partETags
            List<PartETag> partETags = new ArrayList<PartETag>();
            int partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
            }
            // Upload part
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                InputStream instream = new FileInputStream(file);
                instream.skip(startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber(i + 1);
                UploadPartResult uploadPartResult = ossEncryptionClient.uploadPart(uploadPartRequest, context);
                partETags.add(uploadPartResult.getPartETag());
            }
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            ossEncryptionClient.completeMultipartUpload(completeMultipartUploadRequest);

            String filePathNew = key + "-new.txt";
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ossEncryptionClient.getObject(getObjectRequest, new File(filePathNew));
            File fileNew = new File(filePathNew);
            Assert.assertTrue("compare file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            fileNew.delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testSetMultipartContextUnnormal() {
       try {
           String key = "encryption-client-rsa-test-context-unnormals";
           // Correct context set.
           try {
               MultipartUploadCryptoContext context = new MultipartUploadCryptoContext();
               context.setPartSize(100*1024);
               context.setDataSize(500*1024);
               InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
               ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);  
           } catch (Exception e) {
               Assert.fail(e.getMessage());
           }

           // context is null
           try {
               MultipartUploadCryptoContext context = null;
               InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
               ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);
               Assert.fail("context is null ,should be failed.");
           } catch (Exception e) {
               // Expected exception.
           }

           // part size is not set.
           try {
               MultipartUploadCryptoContext context = new MultipartUploadCryptoContext();
               context.setDataSize(500 * 1024);
               InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
               ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);
               Assert.fail("part size is not set, should be failed.");
           } catch (Exception e) {
               // Expected exception.
           }

           // part size is not 16 bytes alignment.
           try {
               MultipartUploadCryptoContext context = new MultipartUploadCryptoContext();
               context.setPartSize(100*1024 + 1);
               context.setDataSize(500 * 1024);
               InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
               ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);
               Assert.fail("part size is not 16 bytes alignmen, should be failed.");
           } catch (Exception e) {
               // Expected exception.
           }
       } catch (Exception e) {
           Assert.fail(e.getMessage());
       }
    }

    @Test
    public void testFindKeyByDescirtion() throws Throwable {
        try {
            String key = "encryption-client-test-put-get-inputstream";
            final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";

            // put get object by default encryption client
            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossEncryptionClient.putObject(req);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossEncryptionClient.getObject(getObjectRequest);
            String readContent = readOSSObject(ossObject);
            Assert.assertEquals(content, readContent.toString());

            // create new encryption materials
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024);
            KeyPair newKeyPair = keyGenerator.generateKeyPair();
            Map<String, String> newDesc = new HashMap<String, String>();
            newDesc.put("Desc2-Key1", "Desc2-Value1");
            SimpleRSAEncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(newKeyPair, newDesc);
            Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);

            // new encryption client has no object key, should be get object faild.
            OSSEncryptionClient ossEncryptionClient2 = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(credentials), encryptionMaterials);
            try {
                getObjectRequest = new GetObjectRequest(bucketName, key);
                ossEncryptionClient2.getObject(getObjectRequest);
                Assert.fail("Has no correct key pair, should be failed.");
            } catch (Exception e) {
                // Ignore exception.
            } finally {
                ossEncryptionClient2.shutdown();
            }

            // new encryption client has object key, should be get object success.
            encryptionMaterials.addKeyPairDescMaterial(keyPair, matDesc);
            ossEncryptionClient2 = new OSSEncryptionClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT,
                    new DefaultCredentialProvider(credentials), encryptionMaterials);

            getObjectRequest = new GetObjectRequest(bucketName, key);
            ossObject = ossEncryptionClient2.getObject(getObjectRequest);
            readContent = readOSSObject(ossObject);
            ossEncryptionClient2.shutdown();
            Assert.assertEquals(content, readContent.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMatDescNull() {
        String key = "encryption-client-test-desc-null";
        final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";
        KeyPair keyPair2 = null;
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        
        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024);
            keyPair2 = keyGenerator.generateKeyPair();
            SimpleRSAEncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair2, null);

            OSSEncryptionClient encryptionClient2 = new OSSEncryptionClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT,
                    new DefaultCredentialProvider(credentials), encryptionMaterials);

            PutObjectRequest req = new PutObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            encryptionClient2.putObject(req);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = encryptionClient2.getObject(getObjectRequest);
            String readContent = readOSSObject(ossObject);
            Assert.assertEquals(content, readContent.toString());
        } catch (Throwable e) {
            Assert.fail(e.getMessage());
        }

        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(1024);
            KeyPair keyPair3 = keyGenerator.generateKeyPair();
            Map<String, String> desc = new HashMap<String, String>();
            desc.put("desc", "test-key");
            SimpleRSAEncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair3, desc);

            OSSEncryptionClient encryptionClient3 = new OSSEncryptionClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT,
                    new DefaultCredentialProvider(credentials), encryptionMaterials);

            try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
                encryptionClient3.getObject(getObjectRequest);
                Assert.fail("Has no correct key pair, should be failed.");
            } catch (Exception e) {
                // Expected exception.
            } finally {
                encryptionClient3.shutdown();
            }

            // new encryption client has the correct key pair, should be get object success.
            try {
                encryptionMaterials.addKeyPairDescMaterial(keyPair2, null);
                encryptionClient3 = new OSSEncryptionClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT,
                        new DefaultCredentialProvider(credentials), encryptionMaterials);
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
                OSSObject ossObject = encryptionClient3.getObject(getObjectRequest);
                String readContent = readOSSObject(ossObject);
                Assert.assertEquals(content, readContent.toString());
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            } finally {
                encryptionClient3.shutdown();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDisabledMethod() {
        final String content = "qwertyuuihonkffttdctgvbkhiijoji";

        // Append object is not allowed.
        try {
            final String key = "encryption-test-disabled-append-object";
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossEncryptionClient.appendObject(appendObjectRequest);
            Assert.fail("Apend object not allowed.");
        } catch (ClientException e) {
            // Expected exception, ignore it.
        }

        // Put object with url is not allowed.
        try {
            final String key = "encryption-test-disabled-put-object-url";
            Date date = new Date();
            date.setTime(date.getTime() + 60 * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
            request.setExpiration(date);
            URL signedUrl = ossEncryptionClient.generatePresignedUrl(request);
            InputStream instream = genFixedLengthInputStream(10);
            ossEncryptionClient.putObject(signedUrl, instream, -1, null, true);
            Assert.fail("put object  with url is not allowed.");
        } catch (ClientException e) {
            // Expected exception.
        }

        // Get object with url is not allowed.
        try {
            final String key = "encryption-test-disabled-get-object-url";
            InputStream instream = genFixedLengthInputStream(10);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossEncryptionClient.putObject(putObjectRequest);

            Date date = new Date();
            date.setTime(date.getTime() + 60 * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
            request.setExpiration(date);
            URL signedUrl = ossEncryptionClient.generatePresignedUrl(request);
            GetObjectRequest getObjectRequest = new GetObjectRequest(signedUrl, null);

            try {
                ossEncryptionClient.getObject(getObjectRequest);
                Assert.fail("Get object with url by encryption client should be failed.");
            } catch (ClientException e) {
                // Expected exception.
            }

            ossClient.getObject(getObjectRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDisabledMethodUploadPart() {
        try {
            final String key = "encryption-client-test-upload-not-with-context";
            File file = createSampleFile(key, 500 * 1024);

            final long partSize = 100 * 1024L; // 100K
            final long fileLength = file.length();
            MultipartUploadCryptoContext context = new MultipartUploadCryptoContext();
            context.setPartSize(partSize);
            context.setDataSize(fileLength);
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);

            // Build init request without crypto context is not allowed.
            try {
                InitiateMultipartUploadResult upresult = ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest);
                Assert.fail("init multi upload withou context should be failed.");
            } catch (ClientException e) {
                // Expected exception.
            }

            InitiateMultipartUploadResult upresult = ossEncryptionClient.initiateMultipartUpload(initiateMultipartUploadRequest, context);
            String uploadId = upresult.getUploadId();

            InputStream instream = new FileInputStream(file);
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartSize(partSize);
            uploadPartRequest.setPartNumber(1);

            // Build init and upload part request without context is not allowed.
            try {
                UploadPartResult uploadPartResult = ossEncryptionClient.uploadPart(uploadPartRequest);
                Assert.fail("upload part without context should be failed.");
            } catch (ClientException e) {
                // Expected exception.
            }

            List<PartETag> partETags = new ArrayList<PartETag>();
            UploadPartResult uploadPartResult = ossEncryptionClient.uploadPart(uploadPartRequest, context);
            partETags.add(uploadPartResult.getPartETag());

            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                    bucketName, key, uploadId, partETags);
            try {
                ossEncryptionClient.completeMultipartUpload(completeMultipartUploadRequest);
                Assert.fail("total size is not equal.");
            } catch (OSSException e) {
                // Expected exception.
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDisabledMethodUploadPartCopy() {
        try {
            String sourceObjectName = "encryptionclient-test-upload-part-copy-src-object";
            String destinationObjectName = "encryptionclient-test-upload-part-copy-dest-object";
            InputStream instream = genFixedLengthInputStream(300 * 1024);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, sourceObjectName, instream);
            ossClient.putObject(putObjectRequest);

            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(
                    bucketName, destinationObjectName);
            InitiateMultipartUploadResult initiateMultipartUploadResult = ossClient
                    .initiateMultipartUpload(initiateMultipartUploadRequest);
            String uploadId = initiateMultipartUploadResult.getUploadId();

            UploadPartCopyRequest uploadPartCopyRequest = new UploadPartCopyRequest(bucketName, sourceObjectName,
                    bucketName, destinationObjectName);
            uploadPartCopyRequest.setUploadId(uploadId);
            uploadPartCopyRequest.setPartSize(100 * 1024L);
            uploadPartCopyRequest.setPartNumber(1);

            // Upload part copy is not allowed.
            try {
                UploadPartCopyResult uploadPartCopyResult = ossEncryptionClient.uploadPartCopy(uploadPartCopyRequest);
                Assert.fail("upload part copy by encryption client is not allowed.");
            } catch (ClientException e) {
                // Expected exception.
            }

            UploadPartCopyResult uploadPartCopyResult = ossClient.uploadPartCopy(uploadPartCopyRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCompareWithOtherSdkRsa() {
        final String PRIVATE_PKCS1_PEM = "-----BEGIN RSA PRIVATE KEY-----\n"
                + "MIICWwIBAAKBgQCokfiAVXXf5ImFzKDw+XO/UByW6mse2QsIgz3ZwBtMNu59fR5z\n"
                + "ttSx+8fB7vR4CN3bTztrP9A6bjoN0FFnhlQ3vNJC5MFO1PByrE/MNd5AAfSVba93\n"
                + "I6sx8NSk5MzUCA4NJzAUqYOEWGtGBcom6kEF6MmR1EKib1Id8hpooY5xaQIDAQAB\n"
                + "AoGAOPUZgkNeEMinrw31U3b2JS5sepG6oDG2CKpPu8OtdZMaAkzEfVTJiVoJpP2Y\n"
                + "nPZiADhFW3e0ZAnak9BPsSsySRaSNmR465cG9tbqpXFKh9Rp/sCPo4Jq2n65yood\n"
                + "JBrnGr6/xhYvNa14sQ6xjjfSgRNBSXD1XXNF4kALwgZyCAECQQDV7t4bTx9FbEs5\n"
                + "36nAxPsPM6aACXaOkv6d9LXI7A0J8Zf42FeBV6RK0q7QG5iNNd1WJHSXIITUizVF\n"
                + "6aX5NnvFAkEAybeXNOwUvYtkgxF4s28s6gn11c5HZw4/a8vZm2tXXK/QfTQrJVXp\n"
                + "VwxmSr0FAajWAlcYN/fGkX1pWA041CKFVQJAG08ozzekeEpAuByTIOaEXgZr5MBQ\n"
                + "gBbHpgZNBl8Lsw9CJSQI15wGfv6yDiLXsH8FyC9TKs+d5Tv4Cvquk0efOQJAd9OC\n"
                + "lCKFs48hdyaiz9yEDsc57PdrvRFepVdj/gpGzD14mVerJbOiOF6aSV19ot27u4on\n"
                + "Td/3aifYs0CveHzFPQJAWb4LCDwqLctfzziG7/S7Z74gyq5qZF4FUElOAZkz718E\n"
                + "yZvADwuz/4aK0od0lX9c4Jp7Mo5vQ4TvdoBnPuGoyw==\n" + "-----END RSA PRIVATE KEY-----";

        final String PUBLIC_X509_PEM = "-----BEGIN PUBLIC KEY-----\n"
                + "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCokfiAVXXf5ImFzKDw+XO/UByW\n"
                + "6mse2QsIgz3ZwBtMNu59fR5zttSx+8fB7vR4CN3bTztrP9A6bjoN0FFnhlQ3vNJC\n"
                + "5MFO1PByrE/MNd5AAfSVba93I6sx8NSk5MzUCA4NJzAUqYOEWGtGBcom6kEF6MmR\n" + "1EKib1Id8hpooY5xaQIDAQAB\n"
                + "-----END PUBLIC KEY-----";

        final ArrayList<String> encSourceList = new ArrayList<String>();
        final ArrayList<Map<String, String>> userMetaList = new ArrayList<Map<String, String>>();

        encSourceList.add("oss/cpp-rsa-enc-example.jpg");
        encSourceList.add("oss/go-rsa-enc-example.jpg");

        final Map<String, String> userMetaCpp = new HashMap<String, String>();
        userMetaCpp.put("x-oss-meta-client-side-encryption-cek-alg", "AES/CTR/NoPadding");
        userMetaCpp.put("x-oss-meta-client-side-encryption-key",
                "nyXOp7delQ/MQLjKQMhHLaT0w7u2yQo"
                        + "DLkSnK8MFg/MwYdh4na4/LS8LLbLcM18m8I/ObWUHU775I50sJCpdv+f4e0jLeVRRiDFWe+uo7Pu"
                        + "c9j4xHj8YB3QlcIOFQiTxHIB6q+C+RA6lGwqqYVa+n3aV5uWhygyv1MWmESurppg=");
        userMetaCpp.put("x-oss-meta-client-side-encryption-start",
                "De/S3T8wFjx7QPxAAFl7h7TeI2EsZl"
                        + "fCwox4WhLGng5DK2vNXxULmulMUUpYkdc9umqmDilgSy5Z3Foafw+v4JJThfw68T/9G2gxZLrQTbA"
                        + "lvFPFfPM9Ehk6cY4+8WpY32uN8w5vrHyoSZGr343NxCUGIp6fQ9sSuOLMoJg7hNw=");
        userMetaCpp.put("x-oss-meta-client-side-encryption-wrap-alg", "RSA/NONE/PKCS1Padding");

        final Map<String, String> userMetaGo = new HashMap<String, String>();
        userMetaGo.put("X-Oss-Meta-Client-Side-Encryption-Cek-Alg", "AES/CTR/NoPadding");
        userMetaGo.put("X-Oss-Meta-Client-Side-Encryption-Key",
                "F2L5QjyA2s85tPvaGdQ5EKnU/XN5dUW"
                        + "qZfgwcM4gfzPMcDWR93AZGSpeB9VSJBYPdIqhy1cevKEJv+Dv2ckDuDJ7nzijwcBnO5tPl5jXYl"
                        + "Wxgzj6t1gMqQr/LENbB5iC8hzGkkoVWjWtSPDB+uE3+qf4V1A0308OqSM3OKxV0VI=");
        userMetaGo.put("X-Oss-Meta-Client-Side-Encryption-Start",
                "D+3z6ftLp500eVnvsat5awYdYI/"
                        + "jTeSRlGlmHNrhTm3l1bonYP1v72vGqZhvOpT++9ZXOhdePu82gjhqVfh8Qv2HZsVGeJLzQJRU8"
                        + "kIKc7PRI4SoqpHZh2VYsASvnDtxVy2MQmpJzvG8xr4j3I29EgsEha7NV+2hGq/dolxLHNc=");
        userMetaGo.put("x-oss-meta-client-side-encryption-wrap-alg", "RSA/NONE/PKCS1Padding");
        userMetaGo.put("X-Oss-Meta-Client-Side-Encryption-Matdesc", "{\"desc\":\"wangtw test client encryption\"}");

        userMetaList.add(userMetaCpp);
        userMetaList.add(userMetaGo);

        Assert.assertEquals(encSourceList.size(), userMetaList.size());

        final String key = "example-test-compare-other-sdk-rsa.jpg";
        final String unEncryptedSource = "oss/example.jpg";
        try {
            for (int index = 0; index < encSourceList.size(); index++) {
                RSAPrivateKey privateKey = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS1(PRIVATE_PKCS1_PEM);
                RSAPublicKey publicKey = SimpleRSAEncryptionMaterials.getPublicKeyFromPemX509(PUBLIC_X509_PEM);
                KeyPair keyPair = new KeyPair(publicKey, privateKey);

                EncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair);
                Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
                OSSEncryptionClient tmpOssEncryptionClient = new OSSEncryptionClientBuilder().
                        build(TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(credentials), encryptionMaterials);

                File originFile = new File(ResourceUtils.getTestFilename(encSourceList.get(index)));
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, originFile);
                putObjectRequest.setHeaders(userMetaList.get(index));

                ossClient.putObject(putObjectRequest);
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);

                File getFile = new File(key);
                tmpOssEncryptionClient.getObject(getObjectRequest, getFile);
                File unEncryptedFile = new File(ResourceUtils.getTestFilename(unEncryptedSource));
                Assert.assertTrue("compare file", compareFile(unEncryptedFile.getAbsolutePath(), getFile.getAbsolutePath()));
                getFile.delete();
                tmpOssEncryptionClient.shutdown();
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetFileWithWrongAlgo() {
        final String PRIVATE_PKCS1_PEM = "-----BEGIN RSA PRIVATE KEY-----\n"
                + "MIICWwIBAAKBgQCokfiAVXXf5ImFzKDw+XO/UByW6mse2QsIgz3ZwBtMNu59fR5z\n"
                + "ttSx+8fB7vR4CN3bTztrP9A6bjoN0FFnhlQ3vNJC5MFO1PByrE/MNd5AAfSVba93\n"
                + "I6sx8NSk5MzUCA4NJzAUqYOEWGtGBcom6kEF6MmR1EKib1Id8hpooY5xaQIDAQAB\n"
                + "AoGAOPUZgkNeEMinrw31U3b2JS5sepG6oDG2CKpPu8OtdZMaAkzEfVTJiVoJpP2Y\n"
                + "nPZiADhFW3e0ZAnak9BPsSsySRaSNmR465cG9tbqpXFKh9Rp/sCPo4Jq2n65yood\n"
                + "JBrnGr6/xhYvNa14sQ6xjjfSgRNBSXD1XXNF4kALwgZyCAECQQDV7t4bTx9FbEs5\n"
                + "36nAxPsPM6aACXaOkv6d9LXI7A0J8Zf42FeBV6RK0q7QG5iNNd1WJHSXIITUizVF\n"
                + "6aX5NnvFAkEAybeXNOwUvYtkgxF4s28s6gn11c5HZw4/a8vZm2tXXK/QfTQrJVXp\n"
                + "VwxmSr0FAajWAlcYN/fGkX1pWA041CKFVQJAG08ozzekeEpAuByTIOaEXgZr5MBQ\n"
                + "gBbHpgZNBl8Lsw9CJSQI15wGfv6yDiLXsH8FyC9TKs+d5Tv4Cvquk0efOQJAd9OC\n"
                + "lCKFs48hdyaiz9yEDsc57PdrvRFepVdj/gpGzD14mVerJbOiOF6aSV19ot27u4on\n"
                + "Td/3aifYs0CveHzFPQJAWb4LCDwqLctfzziG7/S7Z74gyq5qZF4FUElOAZkz718E\n"
                + "yZvADwuz/4aK0od0lX9c4Jp7Mo5vQ4TvdoBnPuGoyw==\n" + "-----END RSA PRIVATE KEY-----";

        final String PUBLIC_X509_PEM = "-----BEGIN PUBLIC KEY-----\n"
                + "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCokfiAVXXf5ImFzKDw+XO/UByW\n"
                + "6mse2QsIgz3ZwBtMNu59fR5zttSx+8fB7vR4CN3bTztrP9A6bjoN0FFnhlQ3vNJC\n"
                + "5MFO1PByrE/MNd5AAfSVba93I6sx8NSk5MzUCA4NJzAUqYOEWGtGBcom6kEF6MmR\n" + "1EKib1Id8hpooY5xaQIDAQAB\n"
                + "-----END PUBLIC KEY-----";

        String sourceFile = "oss/cpp-rsa-enc-example.jpg";

        final Map<String, String> userMetaCpp = new HashMap<String, String>();
        userMetaCpp.put("x-oss-meta-client-side-encryption-cek-alg", "AES/CTR/NoPadding");
        userMetaCpp.put("x-oss-meta-client-side-encryption-key",
                "nyXOp7delQ/MQLjKQMhHLaT0w7u2yQo"
                        + "DLkSnK8MFg/MwYdh4na4/LS8LLbLcM18m8I/ObWUHU775I50sJCpdv+f4e0jLeVRRiDFWe+uo7Pu"
                        + "c9j4xHj8YB3QlcIOFQiTxHIB6q+C+RA6lGwqqYVa+n3aV5uWhygyv1MWmESurppg=");
        userMetaCpp.put("x-oss-meta-client-side-encryption-start",
                "De/S3T8wFjx7QPxAAFl7h7TeI2EsZl"
                        + "fCwox4WhLGng5DK2vNXxULmulMUUpYkdc9umqmDilgSy5Z3Foafw+v4JJThfw68T/9G2gxZLrQTbA"
                        + "lvFPFfPM9Ehk6cY4+8WpY32uN8w5vrHyoSZGr343NxCUGIp6fQ9sSuOLMoJg7hNw=");

        // The key wrap algorithm is unrecognized.
        userMetaCpp.put("x-oss-meta-client-side-encryption-wrap-alg", "RSA-wrong-algo");

        final String key = "example-test-compare-other-sdk-rsa.jpg";
        try {
            RSAPrivateKey privateKey = SimpleRSAEncryptionMaterials.getPrivateKeyFromPemPKCS1(PRIVATE_PKCS1_PEM);
            RSAPublicKey publicKey = SimpleRSAEncryptionMaterials.getPublicKeyFromPemX509(PUBLIC_X509_PEM);
            KeyPair keyPair = new KeyPair(publicKey, privateKey);

            EncryptionMaterials encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair);
            Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
            OSSEncryptionClient tmpOssEncryptionClient = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(credentials), encryptionMaterials);

            File originFile = new File(ResourceUtils.getTestFilename(sourceFile));
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, originFile);
            putObjectRequest.setHeaders(userMetaCpp);

            ossClient.putObject(putObjectRequest);
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);

            try {
                tmpOssEncryptionClient.getObject(getObjectRequest);
                Assert.fail("The key wrap algoritm is unrecognized, should be failed.");
            } catch (ClientException e) {
                // Expected exception.
            }
            tmpOssEncryptionClient.shutdown();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUploadFile() {
        try {
            String key = "encryption-client-uploadfile.txt";
            String filePathNew = key + "-new.txt";
            File file = createSampleFile(key, 1024 * 500);

            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setPartSize(1 * 100 * 1024);
            uploadFileRequest.setEnableCheckpoint(true);
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            uploadFileRequest.setObjectMetadata(objMetadata);

            ossEncryptionClient.uploadFile(uploadFileRequest);

            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            ObjectMetadata objectMetadata = ossEncryptionClient.getObject(getObjectRequest, new File(filePathNew));
            Assert.assertEquals(file.length(), objectMetadata.getContentLength());
            Assert.assertEquals("propval", objectMetadata.getUserMetadata().get("prop"));

            File fileNew = new File(filePathNew);
            Assert.assertTrue("comparte file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadBigFile() {
        try {
            String key = "encryption-client-downloadfile.txt";
            String filePathNew = key + "-new.txt";
            File file = createSampleFile(key, 2 * 1024 * 1024);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
            ObjectMetadata objMetadata = new ObjectMetadata();
            objMetadata.addUserMetadata("prop", "propval");
            putObjectRequest.setMetadata(objMetadata);
            ossEncryptionClient.putObject(putObjectRequest);

            String downloadFile = key + "-down.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downloadFile);
            downloadFileRequest.setPartSize(1 * 100 * 1024);
            downloadFileRequest.setTaskNum(5);
            downloadFileRequest.setEnableCheckpoint(true);

            DownloadFileResult result = ossEncryptionClient.downloadFile(downloadFileRequest);
            Assert.assertEquals("propval", result.getObjectMetadata().getUserMetadata().get("prop"));

            File fileNew = new File(downloadFile);
            Assert.assertTrue("compare file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            fileNew.delete();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadSmallFile() throws Throwable {
        try {
            String key = "encryption-client-downloadfile-small.txt";
            String filePathNew = key + "-new.txt";
            File file = createSampleFile(key, 200 * 1024);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
            ossEncryptionClient.putObject(putObjectRequest);

            String downloadFile = key + "-down.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downloadFile);
            downloadFileRequest.setPartSize(500 * 1024);
            downloadFileRequest.setTaskNum(5);
            downloadFileRequest.setEnableCheckpoint(true);

            DownloadFileResult result = ossEncryptionClient.downloadFile(downloadFileRequest);
            Assert.assertEquals(file.length(), result.getObjectMetadata().getContentLength());

            File fileNew = new File(downloadFile);
            Assert.assertTrue("compare file", compareFile(file.getAbsolutePath(), fileNew.getAbsolutePath()));
            fileNew.delete();
        } catch (Throwable e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadFileWithWrongPartSize() throws Throwable {
        try {
            String key = "encryption-client-downloadfile.txt";
            String filePathNew = key + "-new.txt";
            File file = createSampleFile(key, 2 * 1024 * 1024);

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
            ossEncryptionClient.putObject(putObjectRequest);

            String downloadFile = key + "-down.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downloadFile);
            downloadFileRequest.setPartSize(1 * 100 * 1024 + 1);
            downloadFileRequest.setTaskNum(5);
            downloadFileRequest.setEnableCheckpoint(true);

            ossEncryptionClient.downloadFile(downloadFileRequest);
            Assert.fail("the part size is not 16 bytes alignment. should be failed.");
        } catch (IllegalArgumentException e) {
            // Expected exception.
        } catch (Throwable e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadBigFileWithRangeGet() throws Throwable {
        try {
            String key = "encryption-client-download-big-file-range-get.txt";
            File file = createSampleFile(key, 5 * 500 * 1024);

            int rangeLower = 500;
            int rangeUpper = rangeLower + 500 * 1024;
            int range = rangeUpper - rangeLower + 1;

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
            ossEncryptionClient.putObject(putObjectRequest);

            String downloadFile = key + "-down.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downloadFile);
            downloadFileRequest.setPartSize(500 * 1024);
            downloadFileRequest.setTaskNum(5);
            downloadFileRequest.setEnableCheckpoint(true);
            downloadFileRequest.setRange(rangeLower, rangeUpper);

            DownloadFileResult result = ossEncryptionClient.downloadFile(downloadFileRequest);

            InputStream is1 = new FileInputStream(file);
            byte[] buffer1 = new byte[1024 * 600];
            is1.skip(rangeLower);
            int len1 = is1.read(buffer1, 0, range);
            is1.close();
            Assert.assertEquals(range, len1);

            File fileNew = new File(downloadFile);
            InputStream is2 = new FileInputStream(fileNew);
            byte[] buffer2 = new byte[1024 * 600];
            int len2 = is2.read(buffer2);
            is2.close();
            Assert.assertEquals(range, len2);
            Assert.assertTrue(Arrays.equals(buffer1, buffer2));
            fileNew.delete();
        } catch (Throwable e){
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDownloadSmallFileWithRangeGet() throws Throwable {
        try {
            String key = "encryption-client-download-smal-file-range-get.txt";
            File file = createSampleFile(key, 200 * 1024);

            int rangeLower = 18;
            int rangeUpper = 100;
            int range = rangeUpper - rangeLower + 1;

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
            ossEncryptionClient.putObject(putObjectRequest);

            String downloadFile = key + "-down.txt";
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downloadFile);
            downloadFileRequest.setPartSize(500 * 1024);
            downloadFileRequest.setTaskNum(5);
            downloadFileRequest.setEnableCheckpoint(true);
            downloadFileRequest.setRange(rangeLower, rangeUpper);

            DownloadFileResult result = ossEncryptionClient.downloadFile(downloadFileRequest);

            InputStream is1 = new FileInputStream(file);
            byte[] buffer1 = new byte[1024];
            is1.skip(rangeLower);
            int len1 = is1.read(buffer1, 0, range);
            is1.close();
            Assert.assertEquals(range, len1);

            File fileNew = new File(downloadFile);
            InputStream is2 = new FileInputStream(fileNew);
            byte[] buffer2 = new byte[1024];
            int len2 = is2.read(buffer2);
            is2.close();
            Assert.assertEquals(range, len2);
            Assert.assertTrue(Arrays.equals(buffer1, buffer2));
            fileNew.delete();
        } catch (Throwable e){
            Assert.fail(e.getMessage());
        }
    }

}
