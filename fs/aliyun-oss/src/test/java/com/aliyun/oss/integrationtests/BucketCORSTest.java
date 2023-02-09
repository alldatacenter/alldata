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

import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_CORS_CONFIGURATION_ERR;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.model.CORSConfiguration;
import com.aliyun.oss.model.GenericRequest;
import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.SetBucketCORSRequest;
import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;

public class BucketCORSTest extends TestBase {

    private static int MAX_CORS_RULE_LIMIT = 10;

    @Test
    public void testNormalSetBucketCORS() {
        final String bucketName = "normal-set-bucket-cors";

        try {
            ossClient.createBucket(bucketName);

            // Set bucket cors
            SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);

            CORSRule r0 = new CORSRule();
            r0.addAllowdOrigin("http://www.a.com");
            r0.addAllowdOrigin("http://www.b.com");
            r0.addAllowedMethod("GET");
            r0.addAllowedHeader("Authorization");
            r0.addExposeHeader("x-oss-test");
            r0.addExposeHeader("x-oss-test1");
            r0.setMaxAgeSeconds(100);
            request.addCorsRule(r0);

            ossClient.setBucketCORS(request);

            // Get bucket cors
            List<CORSRule> rules = ossClient.getBucketCORSRules(bucketName);
            r0 = rules.get(0);
            Assert.assertEquals(1, rules.size());
            Assert.assertEquals(2, r0.getAllowedOrigins().size());
            Assert.assertEquals(1, r0.getAllowedMethods().size());
            Assert.assertEquals(1, r0.getAllowedHeaders().size());
            Assert.assertEquals(2, r0.getExposeHeaders().size());
            Assert.assertEquals(100, r0.getMaxAgeSeconds().intValue());

            // Override existing bucket cors
            CORSRule r1 = new CORSRule();
            r1.addAllowdOrigin("*");
            r1.addAllowedMethod("GET");
            r1.addAllowedMethod("PUT");
            r1.addAllowedHeader("Authorization");
            request.clearCorsRules();
            request.addCorsRule(r1);

            ossClient.setBucketCORS(request);

            rules = ossClient.getBucketCORSRules(bucketName);
            r1 = rules.get(0);
            Assert.assertEquals(1, rules.size());
            Assert.assertEquals(1, r1.getAllowedOrigins().size());
            Assert.assertEquals(2, r1.getAllowedMethods().size());
            Assert.assertEquals(1, r1.getAllowedHeaders().size());

            // Delete bucket cors
            ossClient.deleteBucketCORSRules(bucketName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormalSetBucketCORS() {
        final String bucketName = "unormal-set-bucket-cors";

        try {
            ossClient.createBucket(bucketName);

            // Set count of cors rules exceed MAX_CORS_RULE_LIMIT
            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                for (int i = 0; i < MAX_CORS_RULE_LIMIT; i++) {
                    request.addCorsRule(r);
                }
                request.addCorsRule(r);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                List<CORSRule> rules = new ArrayList<CORSRule>();
                for (int i = 0; i < MAX_CORS_RULE_LIMIT; i++) {
                    rules.add(r);
                }
                rules.add(r);
                request.setCorsRules(rules);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Miss required field 'AllowedOrigins'
            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                r.addAllowedMethod("GET");
                r.addAllowedMethod("PUT");
                r.addAllowedHeader("Authorization");
                request.addCorsRule(r);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Miss required field 'AllowedMethods'
            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                r.addAllowdOrigin("*");
                r.addAllowedHeader("Authorization");
                request.addCorsRule(r);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Fill more one asterisk wildcards in allowed origins
            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                r.addAllowdOrigin("www.*.abc.*.com");
                r.addAllowedMethod("GET");
                r.addAllowedMethod("PUT");
                r.addAllowedHeader("Authorization");
                request.addCorsRule(r);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Unsupported method
            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                r.addAllowdOrigin("*");
                r.addAllowedMethod("OPTIONS");
                r.addAllowedHeader("Authorization");
                request.addCorsRule(r);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

            // Fill one asterisk wildcard in allowed origins
            try {
                SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
                CORSRule r = new CORSRule();
                r.addAllowdOrigin("*");
                r.addAllowedMethod("GET");
                r.addAllowedHeader("Authorization");
                r.addExposeHeader("x-oss-*");
                request.addCorsRule(r);
                Assert.fail("Set bucket cors should not be successful");
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testUnormalGetBucketCORS() {
        // Get non-existent bucket
        final String nonexistentBucket = "unormal-get-bucket-cors";
        try {
            ossClient.getBucketCORSRules(nonexistentBucket);
            Assert.fail("Get bucket cors should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.getBucketCORSRules(bucketWithoutOwnership);
            Assert.fail("Get bucket cors should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

        // Get bucket without setting cors rules
        final String bucketWithoutCORSRules = "bucket-without-cors-rules";
        try {
            ossClient.createBucket(bucketWithoutCORSRules);

            ossClient.getBucketCORSRules(bucketWithoutCORSRules);
            Assert.fail("Get bucket cors should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_CORS_CONFIGURATION, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_CORS_CONFIGURATION_ERR));
        } finally {
            ossClient.deleteBucket(bucketWithoutCORSRules);
        }
    }

    @Test
    public void testUnormalDeleteBucketCORS() {
        // Delete non-existent bucket
        final String nonexistentBucket = "unormal-delete-bucket-cors";
        try {
            ossClient.getBucketCORSRules(nonexistentBucket);
            Assert.fail("Delete bucket cors should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Delete bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.getBucketCORSRules(bucketWithoutOwnership);
            Assert.fail("Delete bucket cors should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }
    }

    @Test
    public void testSetBucketCORSWithResponseVary() {
        final String bucketName = "set-bucket-cors-vary";

        try {
            ossClient.createBucket(bucketName);

            // Set bucket cors ResponseVary true
            SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);

            CORSRule r0 = new CORSRule();
            r0.addAllowdOrigin("http://www.a.com");
            r0.addAllowdOrigin("http://www.b.com");
            r0.addAllowedMethod("GET");
            r0.addAllowedHeader("Authorization");
            r0.addExposeHeader("x-oss-test");
            r0.addExposeHeader("x-oss-test1");
            r0.setMaxAgeSeconds(100);
            request.addCorsRule(r0);

            request.setResponseVary(true);
            ossClient.setBucketCORS(request);

            CORSConfiguration config = ossClient.getBucketCORS(new GenericRequest(bucketName));
            List<CORSRule> rules = config.getCorsRules();
            r0 = rules.get(0);
            Assert.assertEquals(1, rules.size());
            Assert.assertEquals(2, r0.getAllowedOrigins().size());
            Assert.assertEquals(1, r0.getAllowedMethods().size());
            Assert.assertEquals(1, r0.getAllowedHeaders().size());
            Assert.assertEquals(2, r0.getExposeHeaders().size());
            Assert.assertEquals(100, r0.getMaxAgeSeconds().intValue());

            Assert.assertEquals(true, config.getResponseVary().booleanValue());

            // Override existing bucket cors
            CORSRule r1 = new CORSRule();
            r1.addAllowdOrigin("*");
            r1.addAllowedMethod("GET");
            r1.addAllowedMethod("PUT");
            r1.addAllowedHeader("Authorization");
            request.clearCorsRules();
            request.addCorsRule(r1);

            ossClient.setBucketCORS(request);

            rules = ossClient.getBucketCORSRules(bucketName);
            r1 = rules.get(0);
            Assert.assertEquals(1, rules.size());
            Assert.assertEquals(1, r1.getAllowedOrigins().size());
            Assert.assertEquals(2, r1.getAllowedMethods().size());
            Assert.assertEquals(1, r1.getAllowedHeaders().size());

            ossClient.deleteBucketCORSRules(bucketName);

            // Set bucket cors ResponseVary false
            request = new SetBucketCORSRequest(bucketName);

            r0 = new CORSRule();
            r0.addAllowdOrigin("http://www.a.com");
            r0.addAllowedMethod("PUT");
            r0.addAllowedHeader("Authorization");
            r0.addExposeHeader("x-oss-abc");
            r0.addExposeHeader("x-oss-abc1");
            r0.setMaxAgeSeconds(110);
            request.addCorsRule(r0);

            request.setResponseVary(false);
            ossClient.setBucketCORS(request);

            config = ossClient.getBucketCORS(new GenericRequest(bucketName));
            rules = config.getCorsRules();
            r0 = rules.get(0);
            Assert.assertEquals(1, rules.size());
            Assert.assertEquals(1, r0.getAllowedOrigins().size());
            Assert.assertEquals(1, r0.getAllowedMethods().size());
            Assert.assertEquals(1, r0.getAllowedHeaders().size());
            Assert.assertEquals(2, r0.getExposeHeaders().size());
            Assert.assertEquals(110, r0.getMaxAgeSeconds().intValue());

            Assert.assertEquals(false, config.getResponseVary().booleanValue());

            ossClient.deleteBucketCORSRules(bucketName);

            //set without setResponseVary
            request = new SetBucketCORSRequest(bucketName);
            r0 = new CORSRule();
            r0.addAllowdOrigin("http://www.a.com");
            r0.addAllowedMethod("PUT");
            r0.addAllowedHeader("Authorization");
            r0.addExposeHeader("x-oss-abc");
            r0.addExposeHeader("x-oss-abc1");
            r0.setMaxAgeSeconds(120);
            request.addCorsRule(r0);
            ossClient.setBucketCORS(request);

            config = ossClient.getBucketCORS(new GenericRequest(bucketName));
            rules = config.getCorsRules();
            r0 = rules.get(0);
            Assert.assertEquals(1, rules.size());
            Assert.assertEquals(1, r0.getAllowedOrigins().size());
            Assert.assertEquals(1, r0.getAllowedMethods().size());
            Assert.assertEquals(1, r0.getAllowedHeaders().size());
            Assert.assertEquals(2, r0.getExposeHeaders().size());
            Assert.assertEquals(120, r0.getMaxAgeSeconds().intValue());

            Assert.assertEquals(false, config.getResponseVary().booleanValue());

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
}
