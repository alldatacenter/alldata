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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Ignore;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;

public class ConcurrencyTest {

    private static final String endpoint = "<valid endpoint>";
    private static final String accessId = "<your access id>";
    private static final String accessKey = "<your access key>";
    
    private OSS client = new OSSClientBuilder().build(endpoint, accessId, accessKey);
    
    private static final String bucketName = "<your bucket name>";
    private static final String key = "<object name>";
    private static final String fileLocation = "<root directory>";
    private static final String filePrefix = "get_object_";
    
    private AtomicInteger fileNumber = new AtomicInteger(0);
    private AtomicInteger completedCount = new AtomicInteger(0);
    
    @Ignore
    public void testGetObjectConcurrently() {
        final int threadCount = 100;
        try {    
            Thread[] ts = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        OSSObject o = client.getObject(bucketName, key);
                        File f = new File(buildFilePath());
                        OutputStream fout = null;
                        InputStream fin = null;
                        
                        try {
                            fout = new FileOutputStream(f);
                            fin = o.getObjectContent();
                            byte[] buf = new byte[1024];
                            int len = 0;
                            while ((len = fin.read(buf)) != -1) {
                                fout.write(buf, 0, len);
                            }
                            fout.flush();
                            // Marks this thread has done.
                            completedCount.incrementAndGet();
                            System.out.println(f.getName() + " done.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (fout != null) {
                                try {
                                    fout.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            
                            if (fin != null) {
                                try {
                                    fin.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
                
                ts[i] = new Thread(r);
            }
            
            for (int i = 0; i < threadCount; i++) {
                ts[i].start();
            }
            
            for (int i = 0; i < threadCount; i++) {
                ts[i].join();
            }
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            int totalCompleted = completedCount.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    @Ignore
    public void testPutObjectConcurrently() {
        final int threadCount = 100;
        final String filePath = "D:\\software\\aliw.exe";
        try {    
            Thread[] ts = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int seqNum = i;
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        try {
                            PutObjectResult result = client.putObject(bucketName, buildObjectKey(key, seqNum), new File(filePath));
                            // Marks this thread has done.
                            completedCount.incrementAndGet();
                            System.out.println(result.getETag());
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        } 
                    }
                };
                
                ts[i] = new Thread(r);
            }
            
            for (int i = 0; i < threadCount; i++) {
                ts[i].start();
            }
            
            for (int i = 0; i < threadCount; i++) {
                ts[i].join();
            }
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        } finally {
            int totalCompleted = completedCount.get();
            Assert.assertEquals(threadCount, totalCompleted);
        }
    }
    
    private String buildFilePath() {
        int value = fileNumber.incrementAndGet();
        return fileLocation + filePrefix + value + "_" + key + ".exe";
    }
    
    private String buildObjectKey(String keyPrefix, int seqNum) {
        return keyPrefix + seqNum;
    }
}
