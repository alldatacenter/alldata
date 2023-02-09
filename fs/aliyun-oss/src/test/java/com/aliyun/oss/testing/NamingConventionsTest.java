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

import org.junit.Assert;
import org.junit.Ignore;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class NamingConventionsTest {
    
    static final String endpoint = "<valid endpoint>";
    static final String accessId = "<your access id>";
    static final String accessKey = "<your access key>";
    
    static OSS client = new OSSClientBuilder().build(endpoint, accessId, accessKey);
    
    @Ignore
    public void testInvalidBucketNames() {
        String[] invalidBucketNames = { "ab", "abcdefjhijklmnopqrstuvwxyz0123456789abcdefjhijklmnopqrstuvwxyza",
                "abC", "abc#", "-abc", "#abc", "-abc-", "Abcdefg", "abcdefg-" };
        for (String value : invalidBucketNames) {
            boolean created = false;
            try {
                client.createBucket(value);
                created = true;
                Assert.fail(String.format("Invalid bucket name %s should not be created successfully.", value));
            } catch (Exception ex) {
                Assert.assertTrue(ex instanceof IllegalArgumentException);
            } finally {
                if (created) {
                    client.deleteBucket(value);
                }
            }
        }
    }
    
    @Ignore
    public void testInvalidObjectNames() {
        // TODO
    }
}
