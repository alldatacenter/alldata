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

import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import java.util.Date;
import java.util.List;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.model.*;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;


public class BucketCnameTest extends TestBase {
    private static final String[] domains = {"001du.cn", "baidu.com",
            "filehomeworks.youcase.com", "files.100km.me", "flv.fls.net.cn"};

    @Ignore
    @SuppressWarnings("deprecation")
    public void testNormalAddBucketCname() {
        final String bucketName = "normal-add-bucket-cname";
        Date curDate;

        try {
            ossClient.createBucket(bucketName);

            // set cname
            ossClient.addBucketCname(new AddBucketCnameRequest(bucketName).withDomain(domains[0]));

            curDate = new Date(System.currentTimeMillis());
            waitForCacheExpiration(5);

            List<CnameConfiguration> cnames = ossClient.getBucketCname(bucketName);

            Assert.assertEquals(cnames.size(), 1);
            Assert.assertEquals(cnames.get(0).getDomain(), domains[0]);
            Assert.assertEquals(cnames.get(0).getStatus(), CnameConfiguration.CnameStatus.Enabled);
            Assert.assertEquals(cnames.get(0).getLastMofiedTime().getYear(), curDate.getYear());
            Assert.assertEquals(cnames.get(0).getLastMofiedTime().getMonth(), curDate.getMonth());
            Assert.assertEquals(cnames.get(0).getLastMofiedTime().getDay(), curDate.getDay());
            System.out.println(cnames.get(0));

            ossClient.deleteBucketCname(bucketName, domains[0]);

            cnames = ossClient.getBucketCname(bucketName);
            Assert.assertEquals(cnames.size(), 0);

            // set multi cname
            for (String domain : domains) {
                AddBucketCnameRequest request = new AddBucketCnameRequest(bucketName);
                request.setDomain(domain);
                ossClient.addBucketCname(request);
            }

            curDate = new Date(System.currentTimeMillis());
            waitForCacheExpiration(5);

            cnames = ossClient.getBucketCname(bucketName);
            Assert.assertEquals(cnames.size(), domains.length);
            for (int i = 0; i < cnames.size(); i++) {
                System.out.println(cnames.get(i));
                Assert.assertEquals(cnames.get(i).getDomain(), domains[i]);
                Assert.assertEquals(cnames.get(0).getStatus(), CnameConfiguration.CnameStatus.Enabled);
                Assert.assertEquals(cnames.get(0).getLastMofiedTime().getYear(), curDate.getYear());
                Assert.assertEquals(cnames.get(0).getLastMofiedTime().getMonth(), curDate.getMonth());
                Assert.assertEquals(cnames.get(0).getLastMofiedTime().getDay(), curDate.getDay());
            }

            for (String domain : domains) {
                DeleteBucketCnameRequest req = new DeleteBucketCnameRequest(bucketName);
                req.setDomain(domain);
                ossClient.deleteBucketCname(req);
            }

            cnames = ossClient.getBucketCname(bucketName);
            Assert.assertEquals(cnames.size(), 0);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Ignore
    public void testNormalDeleteBucketCname() {
        final String bucketName = "normal-delete-bucket-cname";

        try {
            ossClient.createBucket(bucketName);

            // set cname
            ossClient.addBucketCname(new AddBucketCnameRequest(bucketName).withDomain(domains[0]));

            waitForCacheExpiration(5);

            List<CnameConfiguration> cnames = ossClient.getBucketCname(bucketName);
            Assert.assertEquals(cnames.size(), 1);

            ossClient.deleteBucketCname(bucketName, domains[0]);

            cnames = ossClient.getBucketCname(bucketName);
            Assert.assertEquals(cnames.size(), 0);

            // delete not exist cname
            ossClient.deleteBucketCname(bucketName, domains[0]);

            cnames = ossClient.getBucketCname(bucketName);
            Assert.assertEquals(cnames.size(), 0);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormaladdBucketCname() {
        final String bucketName = "unormal-set-bucket-cname";

        // parameter invalid
        try {
            ossClient.addBucketCname(new AddBucketCnameRequest(bucketName));
            Assert.fail("Set bucket cname should not be successful");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }

        try {
            ossClient.deleteBucketCname(new DeleteBucketCnameRequest(bucketName));
            Assert.fail("Delete bucket cname should not be successful");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }

        // bucket non-existent 
        try {
            ossClient.addBucketCname(new AddBucketCnameRequest(bucketName).withDomain(domains[0]));
            Assert.fail("Set bucket cname should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }

        try {
            ossClient.getBucketCname(bucketName);
            Assert.fail("get bucket cname should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }

        try {
            ossClient.getBucketCname(new GenericRequest(bucketName));
            Assert.fail("get bucket cname should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }

        try {
            ossClient.deleteBucketCname(new DeleteBucketCnameRequest(bucketName).withDomain(domains[0]));
            Assert.fail("Delete bucket cname should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }

        try {
            ossClient.deleteBucketCname(bucketName, domains[0]);
            Assert.fail("Delete bucket cname should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }

        // domain invalid
        try {
            ossClient.createBucket(bucketName);

            try {
                ossClient.addBucketCname(new AddBucketCnameRequest(bucketName).withDomain("your.com"));
                Assert.fail("Set bucket cname should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals("NoSuchCnameInRecord", e.getErrorCode());
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testBucketCnameToken() {
        final String endpoint = "oss-ap-southeast-2.aliyuncs.com";
        final String bucketName = super.bucketName + "-bucket-cname";

        //create client
        ClientConfiguration conf = new ClientBuilderConfiguration();
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        OSS client = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        client.createBucket(bucketName);
        CreateBucketCnameTokenResult cresult;
        try {
            CreateBucketCnameTokenRequest request = new CreateBucketCnameTokenRequest(bucketName);
            request.setDomain("www.example.com");
            cresult = client.createBucketCnameToken(request);
            Assert.assertEquals("www.example.com", cresult.getCname());
        } catch (ServiceException e) {
            Assert.assertTrue(false);
        }

        try {
            GetBucketCnameTokenRequest grequest = new GetBucketCnameTokenRequest(bucketName);
            grequest.setDomain("www.example.com");
            GetBucketCnameTokenResult gresult = client.getBucketCnameToken(grequest);
            Assert.assertEquals("www.example.com", gresult.getCname());
            Assert.assertEquals(gresult.getToken(), gresult.getToken());
        } catch (ServiceException e) {
            Assert.assertTrue(false);
        }

        try {
            GetBucketCnameTokenRequest grequest = new GetBucketCnameTokenRequest(bucketName);
            grequest.setDomain("www.example.cn");
            client.getBucketCnameToken(grequest);
            Assert.assertTrue(false);
        } catch (ServiceException e) {
            Assert.assertEquals("CnameTokenNotFound", e.getErrorCode());
        }
    }
}
