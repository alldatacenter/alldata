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

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Test proxy
 */
public class HttpsTest extends TestBase {

    private static class MyHostnameVerifier implements HostnameVerifier {
        public boolean testFlag = false;

        public MyHostnameVerifier() {

        }

        public boolean verify(String s, SSLSession sslSession) {
            return testFlag;
        }

        public final String toString() {
            return "NO_OP";
        }
    }

    private static class MyX509TrustManager implements X509TrustManager
    {
        public boolean traceFlag = false;

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            traceFlag = true;
            throw  new CertificateException("just for test.");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    @Test
    public void testHttpsDefault() {
        String key = "test/test.txt";
        String content = "Hello OSS.";

        try {
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            OSS ossClient = new OSSClientBuilder().build(
                    TestUtils.getHttpsEndpoint(TestConfig.OSS_TEST_ENDPOINT),
                    TestConfig.OSS_TEST_ACCESS_KEY_ID, 
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET, 
                    conf);

            BucketInfo info = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.getBytes().length);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()), metadata);
            
            OSSObject ossObject = ossClient.getObject(bucketName, key);
            InputStream inputStream = ossObject.getObjectContent(); 
            inputStream.close();
            
            ossClient.deleteObject(bucketName, key);

            Assert.assertEquals(conf.isVerifySSLEnable(), true);
            Assert.assertEquals(conf.getX509TrustManagers(), null);
            Assert.assertEquals(conf.getHostnameVerifier(), null);
            Assert.assertEquals(conf.getSecureRandom(), null);
            Assert.assertEquals(conf.getKeyManagers(), null);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testHttpsWithTrustManager() {
        String key = "test/test.txt";
        String content = "Hello OSS.";

        try {
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();

            MyX509TrustManager x509Manager = new MyX509TrustManager();
            Assert.assertEquals(x509Manager.traceFlag, false);

            X509TrustManager[] trustManagers = new X509TrustManager[]{x509Manager};
            conf.setX509TrustManagers(trustManagers);

            OSS ossClient = new OSSClientBuilder().build(
                    TestUtils.getHttpsEndpoint(TestConfig.OSS_TEST_ENDPOINT),
                    TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET,
                    conf);

            BucketInfo info = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.getBytes().length);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()), metadata);

            OSSObject ossObject = ossClient.getObject(bucketName, key);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossClient.deleteObject(bucketName, key);

            Assert.assertEquals(conf.getX509TrustManagers().length, 1);
            Assert.assertEquals(x509Manager.traceFlag, true);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testHttpsWithHostnameVerifier() {
        String key = "test/test.txt";
        String content = "Hello OSS.";

        try {
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            MyHostnameVerifier hostNameVerfify = new MyHostnameVerifier();
            hostNameVerfify.testFlag = true;
            conf.setHostnameVerifier(hostNameVerfify);

            OSS ossClient = new OSSClientBuilder().build(
                    TestUtils.getHttpsEndpoint(TestConfig.OSS_TEST_ENDPOINT),
                    TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET,
                    conf);

            BucketInfo info = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.getBytes().length);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()), metadata);

            OSSObject ossObject = ossClient.getObject(bucketName, key);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossClient.deleteObject(bucketName, key);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            MyHostnameVerifier hostNameVerfify = new MyHostnameVerifier();
            hostNameVerfify.testFlag = false;
            conf.setHostnameVerifier(hostNameVerfify);

            OSS ossClient = new OSSClientBuilder().build(
                    TestUtils.getHttpsEndpoint(TestConfig.OSS_TEST_ENDPOINT),
                    TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET,
                    conf);

            BucketInfo info = ossClient.getBucketInfo(bucketName);
            Assert.fail("can not be here");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testHttpsWithVerifySSLFlag() {
        String key = "test/test.txt";
        String content = "Hello OSS.";

        try {
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            MyHostnameVerifier hostNameVerfify = new MyHostnameVerifier();
            hostNameVerfify.testFlag = false;
            conf.setHostnameVerifier(hostNameVerfify);

            MyX509TrustManager x509Manager = new MyX509TrustManager();
            X509TrustManager[] trustManagers = new X509TrustManager[]{x509Manager};
            conf.setX509TrustManagers(trustManagers);

            conf.setVerifySSLEnable(false);

            OSS ossClient = new OSSClientBuilder().build(
                    TestUtils.getHttpsEndpoint(TestConfig.OSS_TEST_ENDPOINT),
                    TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET,
                    conf);

            BucketInfo info = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.getBytes().length);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()), metadata);

            OSSObject ossObject = ossClient.getObject(bucketName, key);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossClient.deleteObject(bucketName, key);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
