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

import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ACCESS_KEY_ID;
import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ACCESS_KEY_SECRET;
import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ENDPOINT;

import java.io.InputStream;

import org.junit.Test;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;

public class MultiInstanceClientTest extends TestBase {

    @Test
    public void testMulitInstanceClient() throws Exception {
        
        Thread thrd0 = new Thread(new Runnable() {
            final String keyPrefix = "thread0-";
            
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    OSS client0 = new OSSClientBuilder().build(OSS_TEST_ENDPOINT, OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
                    InputStream content = TestUtils.genFixedLengthInputStream(128 * 1024);
                    String key = TestUtils.buildObjectKey(keyPrefix, i);
                    
                    try {
                        PutObjectResult result = client0.putObject(bucketName, key, content, null);
                        System.out.println(key + ": " + result.getETag());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (client0 != null) {
                            System.out.println("shutdown");
                            client0.shutdown();
                        }
                    }
                }
            }
                
        });
    
        Thread thrd1 = new Thread(new Runnable() {
            final String keyPrefix = "thread1-";
            
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    OSS client1 = new OSSClientBuilder().build(OSS_TEST_ENDPOINT, OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
                    InputStream content = TestUtils.genFixedLengthInputStream(1 * 1024 * 1024);
                    String key = TestUtils.buildObjectKey(keyPrefix, i);
                    
                    try {
                        PutObjectResult result = client1.putObject(bucketName, key, content, null);
                        System.out.println(key + ": " + result.getETag());
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (client1 != null) {
                            System.out.println("shutdown");
                            client1.shutdown();
                        }
                    }
                }
            }
                
        });
        
        thrd0.start();
        thrd1.start();
        thrd0.join();
        thrd1.join();

    }
    
    @Test
    public void keepUsingAfterClose() {
        final String key = "key0";
        OSS client = new OSSClientBuilder().build(OSS_TEST_ENDPOINT, OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
        InputStream content = TestUtils.genFixedLengthInputStream(128 * 1024);
        client.putObject(bucketName, key, content, null);
        
        client.shutdown();
        
        try {
            content = TestUtils.genFixedLengthInputStream(128 * 1024);
            client.putObject(bucketName, key, content, null);
        } catch (ClientException ce) {
            System.out.println(ce.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
}
