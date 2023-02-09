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
import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Ignore;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;

/**
 * Test proxy
 */
public class ProxyTest extends TestBase {

    @Ignore
    public void testProxy() {
        String key = "test/test.txt";
        String content = "Hello OSS.";
        
        try {          
            ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
            conf.setProxyHost(TestConfig.PROXY_HOST);
            conf.setProxyPort(TestConfig.PROXY_PORT);
            conf.setProxyUsername(TestConfig.PROXY_USER);
            conf.setProxyPassword(TestConfig.PROXY_PASSWORD);
            
            OSS ossClient = new OSSClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, 
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
