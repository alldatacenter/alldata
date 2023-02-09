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

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.*;
import junit.framework.Assert;

import org.junit.Test;

import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

public class AsyncFetchTaskTest extends TestBase {
    private final String objectName = "test-async-fetch-task-object";
    private String contentMd5;
    private String url;

    private OSSClient ossClient;
    private String bucketName;
    private String endpoint;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        bucketName = super.bucketName + "-aysnc-fetch-task";
        endpoint = TestConfig.OSS_TEST_ENDPOINT;

        //create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        ossClient.createBucket(bucketName);
        waitForCacheExpiration(2);

        ossClient.putObject(bucketName, objectName, new ByteArrayInputStream("123".getBytes()));

        ObjectMetadata meta = ossClient.getObjectMetadata(bucketName, objectName);
        contentMd5 = meta.getContentMD5();

        Date expiration = new Date(new Date().getTime() + 3600 * 1000);
        URL signedUrl = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
        url = signedUrl.toString();
    }

    @Override
    public void tearDown() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
        super.tearDown();
    }

    @Test
    public void testNormalAsyncFetchTask() {
        try {
            final String destObject = objectName + "-destination";
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url).withContentMd5(contentMd5).withIgnoreSameKey(false)
                    .withObjectName(destObject);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            String taskId = setTaskResult.getTaskId();

            Thread.sleep(1000 * 5);

            GetAsyncFetchTaskResult getTaskResult = ossClient.getAsyncFetchTask(bucketName, taskId);
            Assert.assertEquals(taskId, getTaskResult.getTaskId());
            Assert.assertEquals(AsyncFetchTaskState.Success, getTaskResult.getAsyncFetchTaskState());
            Assert.assertTrue(getTaskResult.getErrorMsg().isEmpty());

            AsyncFetchTaskConfiguration taskInfo = getTaskResult.getAsyncFetchTaskConfiguration();
            Assert.assertEquals(url, taskInfo.getUrl());
            Assert.assertEquals(contentMd5, taskInfo.getContentMd5());
            Assert.assertFalse(taskInfo.getIgnoreSameKey());
            Assert.assertEquals(destObject, taskInfo.getObjectName());
            Assert.assertTrue(taskInfo.getHost().isEmpty());
            Assert.assertTrue(taskInfo.getCallback().isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testArgumentNull() {
        try {
            final String destObject = objectName + "-destination";
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url)
                    .withObjectName(destObject);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            String taskId = setTaskResult.getTaskId();

            Thread.sleep(1000 * 5);

            GetAsyncFetchTaskResult getTaskResult = ossClient.getAsyncFetchTask(bucketName, taskId);
            Assert.assertEquals(taskId, getTaskResult.getTaskId());
            Assert.assertEquals(AsyncFetchTaskState.Success, getTaskResult.getAsyncFetchTaskState());
            Assert.assertTrue(getTaskResult.getErrorMsg().isEmpty());

            AsyncFetchTaskConfiguration taskInfo = getTaskResult.getAsyncFetchTaskConfiguration();
            Assert.assertEquals(url, taskInfo.getUrl());
            Assert.assertEquals(destObject, taskInfo.getObjectName());
            Assert.assertTrue(taskInfo.getContentMd5().isEmpty());
            Assert.assertTrue(taskInfo.getHost().isEmpty());
            Assert.assertTrue(taskInfo.getCallback().isEmpty());
            Assert.assertTrue(taskInfo.getIgnoreSameKey());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


    @Test
    public void testArgumentEmpty() {
        try {
            final String destObject = objectName + "-destination";
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url)
                    .withObjectName(destObject)
                    .withHost("")
                    .withContentMd5("")
                    .withCallback("");

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            String taskId = setTaskResult.getTaskId();

            Thread.sleep(1000 * 5);

            GetAsyncFetchTaskResult getTaskResult = ossClient.getAsyncFetchTask(bucketName, taskId);
            Assert.assertEquals(taskId, getTaskResult.getTaskId());
            Assert.assertEquals(AsyncFetchTaskState.Success, getTaskResult.getAsyncFetchTaskState());
            Assert.assertTrue(getTaskResult.getErrorMsg().isEmpty());

            AsyncFetchTaskConfiguration taskInfo = getTaskResult.getAsyncFetchTaskConfiguration();
            Assert.assertEquals(url, taskInfo.getUrl());
            Assert.assertEquals(destObject, taskInfo.getObjectName());
            Assert.assertTrue(taskInfo.getContentMd5().isEmpty());
            Assert.assertTrue(taskInfo.getHost().isEmpty());
            Assert.assertTrue(taskInfo.getCallback().isEmpty());
            Assert.assertTrue(taskInfo.getIgnoreSameKey());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFetchSuccessCallbackFailedState() {
        try {
            final String destObject = objectName + "-destination";
            String callbackContent = "{\"callbackUrl\":\"www.abc.com/callback\",\"callbackBody\":\"${etag}\"}";
            String callback = BinaryUtil.toBase64String(callbackContent.getBytes());
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url).withContentMd5(contentMd5).withIgnoreSameKey(false)
                    .withObjectName(destObject).withCallback(callback);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            String taskId = setTaskResult.getTaskId();

            Thread.sleep(1000 * 5);

            GetAsyncFetchTaskResult getTaskResult = ossClient.getAsyncFetchTask(bucketName, taskId);
            Assert.assertEquals(taskId, getTaskResult.getTaskId());
            Assert.assertEquals(AsyncFetchTaskState.FetchSuccessCallbackFailed, getTaskResult.getAsyncFetchTaskState());
            Assert.assertFalse(getTaskResult.getErrorMsg().isEmpty());

            AsyncFetchTaskConfiguration taskInfo = getTaskResult.getAsyncFetchTaskConfiguration();
            Assert.assertEquals(callback, taskInfo.getCallback());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailedState() {
        try {
            final String destObject = objectName + "-destination";

            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl("invalidUrl").withContentMd5(contentMd5).withIgnoreSameKey(false)
                    .withObjectName(destObject);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            String taskId = setTaskResult.getTaskId();

            Thread.sleep(1000 * 5);

            GetAsyncFetchTaskResult getTaskResult = ossClient.getAsyncFetchTask(bucketName, taskId);
            Assert.assertEquals(taskId, getTaskResult.getTaskId());
            Assert.assertEquals(AsyncFetchTaskState.Failed, getTaskResult.getAsyncFetchTaskState());
            Assert.assertFalse(getTaskResult.getErrorMsg().isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testIgnoreSameKey() {
        final String destObject = objectName + "-destination";

        try {
            ossClient.putObject(bucketName, destObject, new ByteArrayInputStream("123".getBytes()));
        } catch (ClientException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url)
                    .withObjectName(destObject)
                    .withIgnoreSameKey(false);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            Assert.assertNotNull(setTaskResult.getTaskId());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url)
                    .withObjectName(destObject)
                    .withIgnoreSameKey(true);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            Assert.fail("dest object has already exist, fetch task failed.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.OBJECT_ALREADY_EXISTS, e.getErrorCode());
        }

        try {
            AsyncFetchTaskConfiguration configuration = new AsyncFetchTaskConfiguration()
                    .withUrl(url)
                    .withObjectName(destObject);

            SetAsyncFetchTaskResult setTaskResult = ossClient.setAsyncFetchTask(bucketName, configuration);
            Assert.fail("dest object has already exist, fetch task failed.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.OBJECT_ALREADY_EXISTS, e.getErrorCode());
        }
    }

}
