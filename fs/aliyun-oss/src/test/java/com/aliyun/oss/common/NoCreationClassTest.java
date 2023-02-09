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

package com.aliyun.oss.common;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.DefaultServiceClient;
import com.aliyun.oss.common.comm.ExecutionContext;
import com.aliyun.oss.common.comm.NoRetryStrategy;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.VersionInfoUtils;
import com.aliyun.oss.internal.*;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.LocationConstraint;
import com.aliyun.oss.model.WebServiceRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NoCreationClassTest {
    @Test
    public void testNoCreationClass() {
        // update coverage
        RequestParameters requestParameters = new RequestParameters();
        OSSConstants ossConstants = new OSSConstants();
        LocationConstraint locationConstraint = new LocationConstraint();
        SignParameters signParameters = new SignParameters();
        AuthUtils authUtils = new AuthUtils();
        VersionInfoUtils versionInfoUtils = new VersionInfoUtils();
    }

    @Test
    public void testMimetypesClass() {
        String content = "" +
             "xdoc    application/xdoc\n" +
             "#xogg    application/xogg\n" +
             "\n" +
             "xpdf \n" +
             "";
        try {
            Mimetypes mime = Mimetypes.getInstance();
            InputStream input = new ByteArrayInputStream(content.getBytes());
            mime.loadMimetypes(input);
            Assert.assertEquals(mime.getMimetype("test.xdoc"), "application/xdoc");
            Assert.assertEquals(mime.getMimetype("test.xogg"), "application/octet-stream");
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

    }

    static class TestOSSOperation extends OSSOperation {
        public TestOSSOperation(ServiceClient client, CredentialsProvider credsProvider) {
            super(client, credsProvider);
        }

        @Override
        protected boolean isRetryablePostRequest(WebServiceRequest request) {
            return super.isRetryablePostRequest(request);
        }

        @Override
        public ExecutionContext createDefaultContext(HttpMethod method, String bucketName, String key, WebServiceRequest originalRequest) {
            return super.createDefaultContext(method, bucketName, key, originalRequest);
        }
    }

    static class TestOSSMultipartOperation extends  OSSMultipartOperation {
        public TestOSSMultipartOperation(ServiceClient client, CredentialsProvider credsProvider) {
            super(client, credsProvider);
        }

        @Override
        protected boolean isRetryablePostRequest(WebServiceRequest request) {
            return super.isRetryablePostRequest(request);
        }

        @Override
        public ExecutionContext createDefaultContext(HttpMethod method, String bucketName, String key, WebServiceRequest originalRequest) {
            return super.createDefaultContext(method, bucketName, key, originalRequest);
        }
    }

    @Test
    public void testOSSOperationClass() {
        ClientConfiguration config = new ClientConfiguration();
        ServiceClient client = new DefaultServiceClient(config);
        CredentialsProvider cred = new DefaultCredentialProvider("ak", "sk");
        TestOSSOperation operation = new TestOSSOperation(client, cred);
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest("bucket", "key");
        Assert.assertEquals(operation.isRetryablePostRequest(request), false);

        Assert.assertEquals(operation.isRetryablePostRequest(null), false);

        ExecutionContext context = null;
        context = operation.createDefaultContext(HttpMethod.POST, "bucket", "key", request);
        Assert.assertTrue(context.getRetryStrategy() instanceof NoRetryStrategy);

        context = operation.createDefaultContext(HttpMethod.GET, "bucket", "key", request);
        Assert.assertEquals(context.getRetryStrategy(), null);
        Assert.assertFalse(context.getRetryStrategy() instanceof NoRetryStrategy);
    }

    @Test
    public void testTestOSSMultipartOperationClass() {
        ClientConfiguration config = new ClientConfiguration();
        ServiceClient client = new DefaultServiceClient(config);
        CredentialsProvider cred = new DefaultCredentialProvider("ak", "sk");
        TestOSSMultipartOperation operation = new TestOSSMultipartOperation(client, cred);
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest("bucket", "key");
        Assert.assertEquals(operation.isRetryablePostRequest(request), true);

        Assert.assertEquals(operation.isRetryablePostRequest(null), false);

        ExecutionContext context = null;
        context = operation.createDefaultContext(HttpMethod.POST, "bucket", "key", request);
        Assert.assertEquals(context.getRetryStrategy(), null);
        Assert.assertFalse(context.getRetryStrategy() instanceof NoRetryStrategy);

        context = operation.createDefaultContext(HttpMethod.POST, "bucket", "key", null);
        Assert.assertNotNull(context.getRetryStrategy());
        Assert.assertTrue(context.getRetryStrategy() instanceof NoRetryStrategy);

        context = operation.createDefaultContext(HttpMethod.POST, "bucket", "key",
                new AbortMultipartUploadRequest("bucket", "key", "id"));
        Assert.assertNotNull(context.getRetryStrategy());
        Assert.assertTrue(context.getRetryStrategy() instanceof NoRetryStrategy);

        context = operation.createDefaultContext(HttpMethod.GET, "bucket", "key",
                new AbortMultipartUploadRequest("bucket", "key", "id"));
        Assert.assertEquals(context.getRetryStrategy(), null);
    }
}
