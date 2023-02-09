package com.aliyun.oss.integrationtests;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.model.*;
import com.aliyun.oss.utils.ResourceUtils;
import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.integrationtests.TestUtils.removeFile;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_OBJECT_CONTENT_TYPE;

public class SignTest extends  TestBase{

    @Test
    public void testSignV2() {
        String key = "test-sign-V2";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V2);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;
        ossClient.createBucket(bucket);
        String filePath = null;

        try {
            filePath = genFixedLengthFile(1 * 1024 * 1024); //1MB
            PutObjectRequest request = new PutObjectRequest(bucket, key, new File(filePath));
            request.addHeader("x-oss-head1", "value");
            request.addHeader("abc", "value");
            request.addHeader("ZAbc", "value");
            request.addHeader("XYZ", "value");
            request.addAdditionalHeaderName("ZAbc");
            request.addAdditionalHeaderName("x-oss-head1");
            request.addAdditionalHeaderName("abc");
            request.addParameter("param1", "value1");
            request.addParameter("|param1", "value2");
            request.addParameter("+param1", "value3");
            request.addParameter("|param1", "value4");
            request.addParameter("+param2", "");
            request.addParameter("|param2", null);
            request.addParameter("param2", "");

            ossClient.putObject(request);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (filePath != null) {
                removeFile(filePath);
            }
        }
    }

    @Test
    public void testGenerateSignedV2URL() {
        String key = "test-sign-v2-url";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V2);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        Date expiration = new Date(new Date().getTime() + 1000 * 60 *10);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;

        ossClient.createBucket(bucket);
        URL url;
        String filePath;

        try {
            filePath = genFixedLengthFile(100); //1MB
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, new File(filePath));

            ossClient.putObject(putObjectRequest);

            URI endpointURI = new URI(TestConfig.OSS_TEST_ENDPOINT);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
            request.setExpiration(expiration);
            url = ossClient.generatePresignedUrl(request);

            StringBuilder expectedUrlPrefix = new StringBuilder();

            expectedUrlPrefix.append(endpointURI.getScheme()).append("://").append(bucket).append(".").append(endpointURI.getHost()).append("/")
                    .append(key).append("?x-oss-");

            Assert.assertTrue(url.toString().startsWith(expectedUrlPrefix.toString()));
            Assert.assertTrue(url.toString().contains("x-oss-signature"));
            Assert.assertTrue(url.toString().contains("x-oss-signature-version"));
            Assert.assertTrue(url.toString().contains("x-oss-expires"));
            Assert.assertTrue(url.toString().contains("x-oss-access-key-id"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSwitchSignatureVersion() {
        String key = "test-switch-signature-version";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V2);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;
        ossClient.createBucket(bucket);
        String filePath;

        try {
            filePath = genFixedLengthFile(100);

            ossClient.putObject(bucket, key, new File(filePath));

            ossClient.switchSignatureVersion(SignVersion.V1);

            ossClient.putObject(bucket, key, new File(filePath));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutObjectByUrlSignature() {
        final String key = "put-object-by-urlSignature";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V2);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;
        ossClient.createBucket(bucket);

        final String expirationString = "Sun, 12 Apr 2025 12:00:00 GMT";
        final long inputStreamLength = 128 * 1024; //128KB

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
        try {
            Date expiration = DateUtil.parseRfc822Date(expirationString);
            request.setExpiration(expiration);
            request.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            request.addHeader("x-oss-head1", "value");
            request.addHeader("abc", "value");
            request.addHeader("ZAbc", "value");
            request.addHeader("XYZ", "value");
            request.addAdditionalHeaderName("ZAbc");
            request.addAdditionalHeaderName("x-oss-head1");
            request.addAdditionalHeaderName("abc");
            request.addQueryParameter("param1", "value1");
            URL signedUrl = ossClient.generatePresignedUrl(request);

            Map<String, String> requestHeaders = new HashMap<String, String>();

            requestHeaders.put("x-oss-head1", "value");
            requestHeaders.put("abc", "value");
            requestHeaders.put("ZAbc", "value");
            requestHeaders.put("XYZ", "value");
            requestHeaders.put(HttpHeaders.CONTENT_TYPE, DEFAULT_OBJECT_CONTENT_TYPE);
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            // Using url signature & chunked encoding to upload specified inputstream.
            ossClient.putObject(signedUrl, instream, -1, requestHeaders, true);
            OSSObject o = ossClient.getObject(bucket, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void testGetObjectByUrlsignature() {
        final String key = "get-object-by-urlsignature";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V2);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;
        ossClient.createBucket(bucket);

        final String expirationString = "Sun, 12 Apr 2025 12:00:00 GMT";
        final long inputStreamLength = 128 * 1024; //128KB
        final long firstByte= inputStreamLength / 2;
        final long lastByte = inputStreamLength - 1;

        try {
            ossClient.putObject(bucket, key, genFixedLengthInputStream(inputStreamLength), null);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
            Date expiration = DateUtil.parseRfc822Date(expirationString);
            request.setExpiration(expiration);
            request.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            request.addHeader(HttpHeaders.RANGE, String.format("bytes=%d-%d", firstByte, lastByte));
            request.addHeader("x-oss-head1", "value");
            request.addHeader("abc", "value");
            request.addHeader("ZAbc", "value");
            request.addHeader("XYZ", "value");
            request.addAdditionalHeaderName("ZAbc");
            request.addAdditionalHeaderName("x-oss-head1");
            request.addAdditionalHeaderName("abc");
            request.addQueryParameter("param1", "value1");
            URL signedUrl = ossClient.generatePresignedUrl(request);

            Map<String, String> requestHeaders = new HashMap<String, String>();

            requestHeaders.put("x-oss-head1", "value");
            requestHeaders.put("abc", "value");
            requestHeaders.put("ZAbc", "value");
            requestHeaders.put("XYZ", "value");
            requestHeaders.put(HttpHeaders.CONTENT_TYPE, DEFAULT_OBJECT_CONTENT_TYPE);
            requestHeaders.put(HttpHeaders.RANGE, String.format("bytes=%d-%d", firstByte, lastByte));

            OSSObject o = ossClient.getObject(signedUrl, requestHeaders);

            try {
                int bytesRead = -1;
                int totalBytes = 0;
                byte[] buffer = new byte[4096];
                while ((bytesRead = o.getObjectContent().read(buffer)) != -1) {
                    totalBytes += bytesRead;
                }

                Assert.assertEquals((lastByte - firstByte + 1), totalBytes);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            } finally {
                IOUtils.safeClose(o.getObjectContent());
            }
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void testSignedURLV2() {
        final String key = "test-signed-url-v2";
        String endpiont = TestConfig.OSS_TEST_ENDPOINT + "/";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V2);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;
        ossClient.createBucket(bucket);
        String content = "Hello OSS";
        String md5 = BinaryUtil.toBase64String(BinaryUtil.calculateMd5(content.getBytes()));

        try {
            ossClient.putObject(bucket, key, new ByteArrayInputStream(content.getBytes()), null);
            Assert.assertTrue(true);
        }catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //get without metadata ok case
        try {
            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            //default is get
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, null);
            request.setExpiration(expirationDate);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            OSSObject o = ossClient.getObject(signedUrl, headers);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());

            //set content-type to null
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setContentType(null);
            signedUrl = ossClient.generatePresignedUrl(request);

            o = ossClient.getObject(signedUrl, headers);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());

            //set content-type to ""
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setContentType("");
            signedUrl = ossClient.generatePresignedUrl(request);

            o = ossClient.getObject(signedUrl, headers);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //put with content-md5 OK case
        try {
            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            //with md5
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            request.setContentMD5(md5);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            headers.put(HttpHeaders.CONTENT_MD5, md5);
            PutObjectResult result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), headers);
            Assert.assertEquals(result.getETag().isEmpty(), false);

            //with md5 null
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            signedUrl = ossClient.generatePresignedUrl(request);

            result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), null);
            Assert.assertEquals(result.getETag().isEmpty(), false);

            //with md5 ""
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            request.setContentMD5("");
            signedUrl = ossClient.generatePresignedUrl(request);

            result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), null);
            Assert.assertEquals(result.getETag().isEmpty(), false);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //put with userMetadata
        try {
            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            //with user metadata
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            request.addUserMetadata("user", "test");
            request.setProcess("");
            URL signedUrl = ossClient.generatePresignedUrl(request);

            headers.put("x-oss-meta-user", "test");
            PutObjectResult result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), headers);
            Assert.assertEquals(result.getETag().isEmpty(), false);

            //
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentType("application/octet-stream");
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setResponseHeaders(responseHeaders);
            signedUrl = ossClient.generatePresignedUrl(request);

            OSSObject o = ossClient.getObject(signedUrl, null);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //process
        try {
            String originalImage = "oss/example.jpg";
            String newImage = "oss/new-example.jpg";
            String style = "image/resize,m_fixed,w_100,h_100";
            ossClient.putObject(bucket, originalImage, new File(ResourceUtils.getTestFilename(originalImage)));

            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, originalImage, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setProcess(style);
            URL signedUrl = ossClient.generatePresignedUrl(request);
            OSSObject o = ossClient.getObject(signedUrl, null);
            Assert.assertTrue(true);
            IOUtils.safeClose(o.getObjectContent());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //sts token
        try {
            Date  expirationDate = new Date();
            expirationDate.setTime(expirationDate.getTime() + 3600000);
            String stsEndpiont = TestConfig.OSS_TEST_ENDPOINT + "/";
            OSS stsClient = new OSSClientBuilder().build(stsEndpiont, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, "test-sts-token", conf);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            URL signedUrl = stsClient.generatePresignedUrl(request);
            Assert.assertTrue(signedUrl.toString().indexOf("security-token=test-sts-token") != -1);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        TestBase.deleteBucket(bucket);
    }

    @Test
    public void testSignedURLV1() {
        final String key = "test-signed-url-v1";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V1);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;
        ossClient.createBucket(bucket);
        String content = "Hello OSS";
        String md5 = BinaryUtil.toBase64String(BinaryUtil.calculateMd5(content.getBytes()));

        try {
            ossClient.putObject(bucket, key, new ByteArrayInputStream(content.getBytes()), null);
            Assert.assertTrue(true);
        }catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //get without metadata ok case
        try {
            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            //default is get
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, null);
            request.setExpiration(expirationDate);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            OSSObject o = ossClient.getObject(signedUrl, headers);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());

            //set content-type to null
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setContentType(null);
            signedUrl = ossClient.generatePresignedUrl(request);

            o = ossClient.getObject(signedUrl, headers);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());

            //set content-type to ""
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setContentType("");
            signedUrl = ossClient.generatePresignedUrl(request);

            o = ossClient.getObject(signedUrl, headers);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());

        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //put with content-md5 OK case
        try {
            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            //with md5
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            request.setContentMD5(md5);
            URL signedUrl = ossClient.generatePresignedUrl(request);

            headers.put(HttpHeaders.CONTENT_MD5, md5);
            PutObjectResult result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), headers);
            Assert.assertEquals(result.getETag().isEmpty(), false);

            //with md5 null
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            signedUrl = ossClient.generatePresignedUrl(request);

            result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), null);
            Assert.assertEquals(result.getETag().isEmpty(), false);

            //with md5 ""
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            request.setContentMD5("");
            signedUrl = ossClient.generatePresignedUrl(request);

            result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), null);
            Assert.assertEquals(result.getETag().isEmpty(), false);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //put with userMetadata
        try {
            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            //with user metadata
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.PUT);
            request.setExpiration(expirationDate);
            request.addUserMetadata("user", "test");
            request.setProcess("");
            URL signedUrl = ossClient.generatePresignedUrl(request);

            headers.put("x-oss-meta-user", "test");
            PutObjectResult result = ossClient.putObject(signedUrl, new ByteArrayInputStream(content.getBytes()), content.length(), headers);
            Assert.assertEquals(result.getETag().isEmpty(), false);

            //
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentType("application/octet-stream");
            request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setResponseHeaders(responseHeaders);
            signedUrl = ossClient.generatePresignedUrl(request);

            OSSObject o = ossClient.getObject(signedUrl, null);
            Assert.assertEquals(o.getObjectMetadata().getContentMD5(), md5);
            IOUtils.safeClose(o.getObjectContent());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //process
        try {
            String originalImage = "oss/example.jpg";
            String newImage = "oss/new-example.jpg";
            String style = "image/resize,m_fixed,w_100,h_100";
            ossClient.putObject(bucket, originalImage, new File(ResourceUtils.getTestFilename(originalImage)));

            Date  expirationDate = new Date();
            Map<String, String> headers = new HashMap<String, String>();
            expirationDate.setTime(expirationDate.getTime() + 3600000);

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, originalImage, HttpMethod.GET);
            request.setExpiration(expirationDate);
            request.setProcess(style);
            URL signedUrl = ossClient.generatePresignedUrl(request);
            OSSObject o = ossClient.getObject(signedUrl, null);
            Assert.assertTrue(true);
            IOUtils.safeClose(o.getObjectContent());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        //sts token
        try {
            Date  expirationDate = new Date();
            expirationDate.setTime(expirationDate.getTime() + 3600000);
            String stsEndpiont = TestConfig.OSS_TEST_ENDPOINT + "/";
            OSS stsClient = new OSSClientBuilder().build(stsEndpiont, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, "test-sts-token", conf);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key, HttpMethod.GET);
            request.setExpiration(expirationDate);
            URL signedUrl = stsClient.generatePresignedUrl(request);
            Assert.assertTrue(signedUrl.toString().indexOf("security-token=test-sts-token") != -1);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }

        TestBase.deleteBucket(bucket);
    }


    @Test
    public void testGenerateSignedURLIsKeyNotEmpty() {
        String key = "";
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V1);
        OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET, conf);
        Date expiration = new Date(new Date().getTime() + 1000 * 60 *10);
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucket = TestBase.BUCKET_NAME_PREFIX + ticks;

        ossClient.createBucket(bucket);

        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
            request.setExpiration(expiration);
            ossClient.generatePresignedUrl(request);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        key = "test";
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, key);
            request.setExpiration(expiration);
            ossClient.generatePresignedUrl(request);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
