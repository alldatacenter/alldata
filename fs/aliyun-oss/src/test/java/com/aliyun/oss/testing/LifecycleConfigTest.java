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

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.LifecycleRule;
import com.aliyun.oss.model.LifecycleRule.RuleStatus;
import com.aliyun.oss.model.SetBucketLifecycleRequest;

public class LifecycleConfigTest {
    
    static final String endpoint = "<valid endpoint>";
    static final String accessId = "<your access id>";
    static final String accessKey = "<your access key>";
    
    static final String bucketName = "<your bucket name>";
    
    @Ignore
    public void testLifecycleConfig() {
        OSS client = new OSSClientBuilder().build(endpoint, accessId, accessKey);
        try {
            SetBucketLifecycleRequest req = new SetBucketLifecycleRequest(bucketName);
            req.AddLifecycleRule(new LifecycleRule("delete obsoleted files", "obsoleted/", RuleStatus.Enabled, 3));
            req.AddLifecycleRule(new LifecycleRule("delete temporary files", "temporary/", RuleStatus.Enabled, 
                    DateUtil.parseIso8601Date("2022-10-12T00:00:00.000Z")));
            // set bucket lifecycle
            client.setBucketLifecycle(req);
            
            // get bucket lifecycle
            List<LifecycleRule> rules = client.getBucketLifecycle(bucketName);
            Assert.assertEquals(rules.size(), 2);
            
            LifecycleRule r0 = rules.get(0);
            Assert.assertEquals(r0.getId(), "delete obsoleted files");
            Assert.assertEquals(r0.getPrefix(), "obsoleted/");
            Assert.assertEquals(r0.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(r0.getExpirationDays(), 3);
            
            LifecycleRule r1 = rules.get(1);
            Assert.assertEquals(r1.getId(), "delete temporary files");
            Assert.assertEquals(r1.getPrefix(), "temporary/");
            Assert.assertEquals(r1.getStatus(), RuleStatus.Enabled);
            Assert.assertEquals(DateUtil.formatIso8601Date(r1.getExpirationTime()), "2022-10-12T00:00:00.000Z");
            
            // delete bucket lifecycle
            client.deleteBucketLifecycle(bucketName);
            
            // try to get bucket lifecycle again
            try {
                client.getBucketLifecycle(bucketName);
            } catch (OSSException ex) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_LIFECYCLE, ex.getErrorCode());
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
