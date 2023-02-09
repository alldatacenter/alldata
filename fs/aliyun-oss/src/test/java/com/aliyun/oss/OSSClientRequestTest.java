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

package com.aliyun.oss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.comm.ExecutionContext;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.comm.RetryStrategy;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.common.utils.IOUtils;
import com.aliyun.oss.internal.OSSBucketOperation;
import com.aliyun.oss.internal.OSSConstants;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.internal.OSSMultipartOperation;
import com.aliyun.oss.internal.OSSObjectOperation;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.ListMultipartUploadsRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.ListPartsRequest;
import com.aliyun.oss.model.LocationConstraint;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.aliyun.oss.model.SetBucketAclRequest;
import com.aliyun.oss.model.UploadPartRequest;

public class OSSClientRequestTest {
    private static class RequestTestServiceClient extends ServiceClient {
        
        public RequestTestServiceClient(ClientConfiguration conf) {
            super(conf);
        }

        @Override
        protected ResponseMessage sendRequestCore(ServiceClient.Request data, ExecutionContext context)
                throws IOException {
            throw new RequestReceivedException(data);
        }

        @Override
        protected RetryStrategy getDefaultRetryStrategy() {
            return new RetryStrategy() {
                
                @Override
                public boolean shouldRetry(Exception ex, RequestMessage request,
                        ResponseMessage response, int retries) {
                    return false;
                }
            };
        }

        @Override
        public void shutdown() {
            // TODO Auto-generated method stub
        }
    }

    private static class RequestReceivedException extends OSSException{
        private static final long serialVersionUID = 6769291383900741720L;

        private ServiceClient.Request request;

        public ServiceClient.Request getRequest(){
            return this.request;
        }

        public RequestReceivedException(ServiceClient.Request request){
            this.request = request;
        }
    }

    private static interface TestAction{
        void run() throws Exception;
    }

    private static URI endpoint;
    private String accessId = "test";
    private String accessKey = "test";
    private String bucketName = accessId + "bucket";
    private String objectKey = "object";
    private static ClientConfiguration conf;
    
    static {
        conf = new ClientConfiguration();
        List<String> cnameExcludeList = new ArrayList<String>();
        cnameExcludeList.add("localhost");
        conf.setCnameExcludeList(cnameExcludeList);
    }
    
    final OSSBucketOperation bucketOp =
            new OSSBucketOperation(
                    new RequestTestServiceClient(conf),
                    new DefaultCredentialProvider(new DefaultCredentials(accessId, accessKey)));
    final OSSObjectOperation objectOp =
            new OSSObjectOperation(
                    new RequestTestServiceClient(conf),
                    new DefaultCredentialProvider(new DefaultCredentials(accessId, accessKey)));
    final OSSMultipartOperation multipartOp =
            new OSSMultipartOperation(
                    new RequestTestServiceClient(conf),
                    new DefaultCredentialProvider(new DefaultCredentials(accessId, accessKey)));

    static{
        try {
            endpoint = new URI("http://localhost/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Before
    public void setUp() {
        bucketOp.setEndpoint(endpoint);
        objectOp.setEndpoint(endpoint);
        multipartOp.setEndpoint(endpoint);
    }

    @SuppressWarnings("serial")
    @Test
    public void testBucketAclRequests() {
        // Put Bucket ACL
        // expected acl: private
        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                bucketOp.setBucketAcl(new SetBucketAclRequest(bucketName, null));
            }
        };
        executeTest(test1, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), "?acl",
                new HashMap<String, String>(){
            {
            }
        });
        // expected acl: public-read
        TestAction test2 = new TestAction(){
            public void run() throws Exception{
                bucketOp.setBucketAcl(new SetBucketAclRequest(bucketName, CannedAccessControlList.PublicRead));
            }
        };
        executeTest(test2, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), "?acl",
                new HashMap<String, String>(){
            {
                put("x-oss-acl", "public-read");
            }
        });
        // expected acl: public-read-write
        TestAction test3 = new TestAction(){
            public void run() throws Exception{
                bucketOp.setBucketAcl(new SetBucketAclRequest(bucketName, CannedAccessControlList.PublicReadWrite));
            }
        };
        executeTest(test3, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), "?acl",
                new HashMap<String, String>(){
            {
                put("x-oss-acl", "public-read-write");
            }
        });
     // expected acl: private
        TestAction test4 = new TestAction(){
            public void run() throws Exception{
                bucketOp.setBucketAcl(new SetBucketAclRequest(bucketName, CannedAccessControlList.Private));
            }
        };
        executeTest(test4, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), "?acl",
                new HashMap<String, String>(){
            {
                put("x-oss-acl", "private");
            }
        });


        // Get Bucket ACL
        // expected acl: private
        TestAction test5 = new TestAction(){
            public void run() throws Exception{
                bucketOp.getBucketAcl(new GenericRequest(bucketName));
            }
        };
        executeTest(test5, HttpMethod.GET, bucketName + "." + endpoint.getHost(), "?acl", null);

    }
    
    @Test
    public void testCreateBucketRequest() {
        TestAction test = new TestAction() {
            
            @Override
            public void run() throws Exception {
                CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
                createBucketRequest.setLocationConstraint(LocationConstraint.OSS_CN_QINGDAO);
                bucketOp.createBucket(createBucketRequest);
            }
        };
        String requestXml = "<CreateBucketConfiguration><LocationConstraint>oss-cn-qingdao</LocationConstraint></CreateBucketConfiguration>";
        executeTest(test, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), "", null, requestXml, requestXml.length());
    }
    
    @Test
    public void testBucketLocationRequest() {
        TestAction test = new TestAction() {
            
            @Override
            public void run() throws Exception {
                bucketOp.getBucketLocation(new GenericRequest(bucketName));
            }
        };
        executeTest(test, HttpMethod.GET, bucketName + "." + endpoint.getHost(), "?location", null);
    }

    @Test
    public void testListObjectsRequest(){
        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                bucketOp.listObjects(new ListObjectsRequest(bucketName));
            }
        };
        executeTest(test1, HttpMethod.GET, bucketName + "." + endpoint.getHost(), "", null);

        TestAction test2 = new TestAction(){
            public void run() throws Exception{
                String prefix = "p";
                String marker = "m";
                String delimiter = "d";
                int maxKeys = 99;

                bucketOp.listObjects(
                        new ListObjectsRequest(
                                bucketName, prefix, marker, delimiter, maxKeys));
            }
        };

        executeTest(test2, HttpMethod.GET,
                bucketName + "." + endpoint.getHost(), "?prefix=p&marker=m&delimiter=d&max-keys=99", null);
    }

    @SuppressWarnings("serial")
    @Test
    public void testPutObjectRequest(){
        String content = "中English混合的Content。\n" + "This is the 2nd line.";
        byte[] contentBuffer = null;
        try {
            contentBuffer = content.getBytes(OSSConstants.DEFAULT_CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            fail(e.getMessage());
        }
        final ByteArrayInputStream input =  new ByteArrayInputStream(contentBuffer);

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentBuffer.length);
        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                objectOp.putObject(new PutObjectRequest(bucketName, objectKey, input, metadata));
            }
        };
        executeTest(test1, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), objectKey,
                new HashMap<String, String>(){
            {
                put("Content-Type", "application/octet-stream");
                put("Content-Length", Long.toString(metadata.getContentLength()));
            }
        },
        content, contentBuffer.length);

        metadata.setContentType("text/plain");
        metadata.setContentEncoding(OSSConstants.DEFAULT_CHARSET_NAME);
        metadata.setCacheControl("no-cache");
        metadata.setServerSideEncryption(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        metadata.setUserMetadata(new HashMap<String, String>() {{ put("my", "my"); }});

        final ByteArrayInputStream input2 =  new ByteArrayInputStream(contentBuffer);
        TestAction test2 = new TestAction(){
            public void run() throws Exception{
                objectOp.putObject(new PutObjectRequest(bucketName, objectKey, input2, metadata));
            }
        };
        executeTest(test2, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), objectKey,
                new HashMap<String, String>(){
            {
                put("Content-Type", metadata.getContentType());
                put("Content-Length", Long.toString(metadata.getContentLength()));
                put("Content-Encoding", metadata.getContentEncoding());
                put("Cache-Control", metadata.getCacheControl());
                put("x-oss-server-side-encryption", metadata.getServerSideEncryption());
                put("x-oss-meta-my", "my");
            }
        },
        content, contentBuffer.length);
    }

    @SuppressWarnings("serial")
    @Test
    public void testCopyObjectRequest(){
        final String sourceBucketName = "src_bucket";
        final String sourceKey = "src_key";

        final CopyObjectRequest request =
                new CopyObjectRequest(sourceBucketName, sourceKey, bucketName, objectKey);

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                objectOp.copyObject(request);
            }
        };
        executeTest(test1, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), objectKey,
                new HashMap<String, String>(){
            {
                put("x-oss-copy-source", "/" + sourceBucketName + "/" + sourceKey);
            }
        },
        null, 0);

        final List<String> matchingETags = new LinkedList<String>();
        matchingETags.add("matching");
        matchingETags.add("m2");
        request.setMatchingETagConstraints(matchingETags);
        final List<String> unmatchingETags = new LinkedList<String>();
        unmatchingETags.add("unmatching");
        unmatchingETags.add("u2");
        request.setNonmatchingETagConstraints(unmatchingETags);
        request.setModifiedSinceConstraint(new Date());
        request.setUnmodifiedSinceConstraint(new Date());
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentEncoding(OSSConstants.DEFAULT_CHARSET_NAME);
        metadata.setCacheControl("no-cache");
        metadata.setServerSideEncryption(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        metadata.setUserMetadata(new HashMap<String, String>() {{ put("my", "my"); }});
        request.setNewObjectMetadata(metadata);

        final Map<String, String> expectedHeaders = new HashMap<String, String>(){
            {
                put("x-oss-copy-source-if-match", matchingETags.get(0) + ", " + matchingETags.get(1));
                put("x-oss-copy-source-if-none-match", unmatchingETags.get(0) + ", " + unmatchingETags.get(1));
                put("x-oss-copy-source-if-modified-since", DateUtil.formatRfc822Date(request.getModifiedSinceConstraint()));
                put("x-oss-copy-source-if-unmodified-since", DateUtil.formatRfc822Date(request.getUnmodifiedSinceConstraint()));
                put("x-oss-metadata-directive", "REPLACE");
                put("Content-Type", metadata.getContentType());
                put("Content-Encoding", metadata.getContentEncoding());
                put("Cache-Control", metadata.getCacheControl());
                put("x-oss-server-side-encryption", metadata.getServerSideEncryption());
                put("x-oss-meta-my", "my");
            }
        };

        TestAction test2 = new TestAction(){
            public void run() throws Exception{
                objectOp.copyObject(request);
            }
        };
        executeTest(test2, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), objectKey,
                expectedHeaders, null, 0);
    }

    @SuppressWarnings("serial")
    @Test
    public void testGetObjectRequest(){
        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                objectOp.getObject(new GetObjectRequest(bucketName, objectKey));
            }
        };
        executeTest(test1, HttpMethod.GET, bucketName + "." + endpoint.getHost(), objectKey, null);

        final GetObjectRequest request = new GetObjectRequest(bucketName, objectKey);
        final List<String> matchingETags = new LinkedList<String>();
        matchingETags.add("matching");
        request.setMatchingETagConstraints(matchingETags);
        final List<String> unmatchingETags = new LinkedList<String>();
        unmatchingETags.add("unmatching");
        request.setNonmatchingETagConstraints(unmatchingETags);
        request.setModifiedSinceConstraint(new Date());
        request.setUnmodifiedSinceConstraint(new Date());

        final Map<String, String> expectedHeaders = new HashMap<String, String>(){
            {
                put("If-Match", matchingETags.get(0));
                put("If-None-Match", unmatchingETags.get(0));
                put("If-Modified-Since", DateUtil.formatRfc822Date(request.getModifiedSinceConstraint()));
                put("If-Unmodified-Since", DateUtil.formatRfc822Date(request.getUnmodifiedSinceConstraint()));
            }
        };

        TestAction test2 = new TestAction(){
            public void run() throws Exception{
                objectOp.getObject(request);
            }
        };
        executeTest(test2, HttpMethod.GET, bucketName + "." + endpoint.getHost(), objectKey, expectedHeaders);

        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
        responseHeaders.setCacheControl("no-cache");
        request.setResponseHeaders(responseHeaders);
        TestAction test3 = new TestAction(){
            public void run() throws Exception{
                objectOp.getObject(request);
            }
        };
        executeTest(test3, HttpMethod.GET, bucketName + "." + endpoint.getHost(), objectKey +
                "?response-cache-control=no-cache", expectedHeaders);
    }

    @SuppressWarnings("serial")
    @Test
    public void testGetObjectByRangeRequest(){
        final String RANGE_HEADER = "Range";

        final GetObjectRequest request = new GetObjectRequest(bucketName, objectKey);
        request.setRange(0, 9);

        final Map<String, String> expectedHeaders = new HashMap<String, String>(){
            {
                put(RANGE_HEADER, "bytes=0-9");
            }
        };

        TestAction test = new TestAction(){
            public void run() throws Exception{
                objectOp.getObject(request);
            }
        };
        executeTest(test, HttpMethod.GET, bucketName + "." + endpoint.getHost(), objectKey, expectedHeaders);

        /* All following ranges regarded as valid input.
        try {
            request.setRange(100, -1);
            expectedHeaders.put(RANGE_HEADER, "bytes=100-");
            executeTest(test, HttpMethod.GET, bucketName + "." + endpoint.getHost(), objectKey, expectedHeaders);
            fail("Get object should not be successful.");
        } catch (IllegalArgumentException e) {
            // success
        }

        try {
            request.setRange(-1, 100);
            expectedHeaders.put(RANGE_HEADER, "bytes=-100");
            executeTest(test, HttpMethod.GET, bucketName + "." + endpoint.getHost(), objectKey, expectedHeaders);
            fail("Get object should not be successful.");
        } catch (IllegalArgumentException e) {
            // success
        }

        try {
            request.setRange(-1, -1);
            fail("Expected exception is not thrown.");
        } catch (IllegalArgumentException e) {
            // success
        } */
    }

    @SuppressWarnings("serial")
    @Test
    public void testInitiateMultipartUploadRequest(){
        final InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                new InitiateMultipartUploadRequest(bucketName, objectKey);

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                multipartOp.initiateMultipartUpload(initiateMultipartUploadRequest);
            }
        };
        executeTest(test1, HttpMethod.POST, bucketName + "." + endpoint.getHost(), objectKey + "?uploads", null);

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setCacheControl("no-cache");
        metadata.setServerSideEncryption(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
        metadata.addUserMetadata("zdata", "data");
        initiateMultipartUploadRequest.setObjectMetadata(metadata);

        executeTest(test1, HttpMethod.POST, bucketName + "." + endpoint.getHost(), objectKey + "?uploads", 
                new HashMap<String, String>(){
            {
                put ("Cache-Control", metadata.getCacheControl());
                put (OSSHeaders.OSS_USER_METADATA_PREFIX + "zdata", "data");
                put ("x-oss-server-side-encryption", metadata.getServerSideEncryption());
            }
        });
    }

    @SuppressWarnings("serial")
    @Test
    public void testUploadPartRequest() throws Exception{
        String uploadId = "upload123";
        final UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(bucketName);
        request.setKey(objectKey);
        request.setUploadId(uploadId);
        request.setPartNumber(1);

        byte[] inputBytes = uploadId.getBytes(OSSConstants.DEFAULT_CHARSET_NAME);
        ByteArrayInputStream input = new ByteArrayInputStream(inputBytes);
        request.setInputStream(input);
        request.setPartSize(inputBytes.length);

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                multipartOp.uploadPart(request);
            }
        };
        executeTest(test1, HttpMethod.PUT, bucketName + "." + endpoint.getHost(), objectKey
                + "?partNumber=" + request.getPartNumber() + "&uploadId=" + uploadId, 
                new HashMap<String, String>(){
            {
                put("Content-Length", Long.toString(request.getPartSize()));
            }
        });
    }

    @Test
    public void testCompleteMultipartUploadRequest(){
        String uploadId = "upload123";
        List<PartETag> partETags = new LinkedList<PartETag>();
        partETags.add(new PartETag(2, "PART2ABCD"));
        partETags.add(new PartETag(1, "PART1ABCD"));
        partETags.add(new PartETag(3, "PART3ABCD"));
        final CompleteMultipartUploadRequest request =
                new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);
        String requestXml = "<CompleteMultipartUpload>"
                + "<Part><PartNumber>1</PartNumber><ETag>&quot;PART1ABCD&quot;</ETag></Part>"
                + "<Part><PartNumber>2</PartNumber><ETag>&quot;PART2ABCD&quot;</ETag></Part>"
                + "<Part><PartNumber>3</PartNumber><ETag>&quot;PART3ABCD&quot;</ETag></Part>"
                + "</CompleteMultipartUpload>";

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                multipartOp.completeMultipartUpload(request);
            }
        };
        executeTest(test1, HttpMethod.POST,
                bucketName + "." + endpoint.getHost(), objectKey + "?uploadId=" + uploadId, null,
                requestXml, -1);
    }

    @Test
    public void testAbortMultipartUploadRequest(){
        String uploadId = "upload123";
        final AbortMultipartUploadRequest request =
                new AbortMultipartUploadRequest(bucketName, objectKey, uploadId);

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                multipartOp.abortMultipartUpload(request);
            }
        };
        executeTest(test1, HttpMethod.DELETE,
                bucketName + "." + endpoint.getHost(), objectKey + "?uploadId=" + uploadId, null);
    }

    @Test
    public void testListMutilpartUploadsRequest() throws Exception{
        final ListMultipartUploadsRequest request =
                new ListMultipartUploadsRequest(bucketName);

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                multipartOp.listMultipartUploads(request);
            }
        };
        executeTest(test1, HttpMethod.GET, bucketName + "." + endpoint.getHost(), "?uploads", null);

        request.setMaxUploads(100);
        request.setDelimiter(";");
        request.setKeyMarker("KEYMARKER1");
        request.setPrefix("PREFIX-");
        request.setUploadIdMarker("UPLOAD123");

        String expectedPath = "?uploads"
                + "&delimiter=" + HttpUtil.urlEncode(request.getDelimiter(), OSSConstants.DEFAULT_CHARSET_NAME)
                + "&key-marker=" + request.getKeyMarker()
                + "&max-uploads=" + request.getMaxUploads().toString()
                + "&prefix=" + request.getPrefix()
                + "&upload-id-marker=" + request.getUploadIdMarker();

        executeTest(test1, HttpMethod.GET, bucketName + "." + endpoint.getHost(), expectedPath, null);
    }

    @Test
    public void testGeneratePresignedUrlRequest() throws Exception {

        URI endpoint = new URI("http://localhost/");
        String accessId = "test";
        String accessKey = "test";

        String bucketName = accessId + "bucket";
        String objectKey = "object";

        String server = endpoint.toString();
        OSS ossClient = new OSSClientBuilder().build(server, accessId, accessKey);
        URL url = null;
        // Simple case
        //10 分钟过期
        Date expiration = new Date(new Date().getTime() + 1000 * 60 * 10 );

        try {
            url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
        } catch (ClientException e) {
            fail(e.getMessage());
        }

        String expectedUrlPrefix =  endpoint.getScheme() + "://" + endpoint.getHost() + "/" + bucketName + "/" + objectKey
                + "?Expires=" + Long.toString(expiration.getTime() / 1000) + "&OSSAccessKeyId=" + accessId
                + "&Signature=";
        
        Assert.assertTrue(url.toString().startsWith(expectedUrlPrefix));

        // Override response header
        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
        responseHeaders.setCacheControl("no-cache");

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectKey);
        request.setExpiration(expiration);
        request.setResponseHeaders(responseHeaders);

        expectedUrlPrefix = endpoint.getScheme() 
                + "://" + endpoint.getHost() 
                + "/" + bucketName
                + "/" + objectKey
                + "?Expires="
                + Long.toString(expiration.getTime() / 1000) 
                + "&OSSAccessKeyId=" + accessId
                + "&Signature=";
        try {
            url = ossClient.generatePresignedUrl(request);
            Assert.assertTrue(url.toString().startsWith(expectedUrlPrefix));
        } catch (ClientException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testListPartsRequest(){
        String uploadId = "upload123";
        final ListPartsRequest request = new ListPartsRequest(bucketName, objectKey, uploadId);
        String expectedUrl = objectKey + "?uploadId=" + uploadId;

        TestAction test1 = new TestAction(){
            public void run() throws Exception{
                multipartOp.listParts(request);
            }
        };
        executeTest(test1, HttpMethod.GET, bucketName + "." + endpoint.getHost(), expectedUrl, null);

        request.setMaxParts(100);
        request.setPartNumberMarker(50);

        expectedUrl += "&max-parts=" + request.getMaxParts().toString()
                + "&part-number-marker=" + request.getPartNumberMarker().toString();
        executeTest(test1, HttpMethod.GET, bucketName + "." + endpoint.getHost(), expectedUrl, null);
    }

    private void executeTest(TestAction test, HttpMethod expectedMethod, String expectedHost, String expectedPath,
            Map<String, String> expectedHeaders){
        executeTest(test, expectedMethod, expectedHost, expectedPath, expectedHeaders, null, -1);
    }

    private void executeTest(TestAction test, HttpMethod expectedMethod, String expectedHost, String expectedPath,
            Map<String, String> expectedHeaders, String expectedContent, long contentLength) {
        try {
            test.run();
        } catch (Exception e){
            assertTrue(e.getMessage(), e instanceof RequestReceivedException);
            ServiceClient.Request request = ((RequestReceivedException)e).getRequest();
            assertEquals(expectedMethod, request.getMethod());
            assertEquals(endpoint.getScheme() + "://" + expectedHost + "/" + expectedPath, request.getUri());
            if (expectedHeaders != null) {
                for(Entry<String, String> header : expectedHeaders.entrySet()) {
                    assertEquals(header.getValue(), request.getHeaders().get(header.getKey()));
                }
            }
            if (expectedContent != null){
                try {
                    assertEquals(expectedContent,
                            IOUtils.readStreamAsString(request.getContent(), "utf-8"));
                } catch (IOException e1) {
                    fail(e1.getMessage());
                }
            }
            if (contentLength >= 0){
                assertEquals(contentLength, request.getContentLength());
            }
        }
    }

}
