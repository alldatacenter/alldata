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
import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_REGION;
import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ACCESS_KEY_ID_1;
import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ACCESS_KEY_SECRET_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;

public class SwitchCredentialsAndEndpointTest extends TestBase {

    /* Indicate whether credentials switching starts prior to credentials verification */
    private volatile boolean switchStarted = false;
    
    private static final int loopTimes = 100;
    private static final int switchInterval = 50; // unit in milliseconds

    @Test
    public void testSwitchValidCredentialsAndEndpoint() {
        CredentialsProvider credsProvider = ossClient.getCredentialsProvider();
        Credentials defaultCreds = credsProvider.getCredentials();
        assertEquals(OSS_TEST_ACCESS_KEY_ID, defaultCreds.getAccessKeyId());
        assertEquals(OSS_TEST_ACCESS_KEY_SECRET, defaultCreds.getSecretAccessKey());

        // Verify default credentials under default endpoint
        try {
            String loc = ossClient.getBucketLocation(bucketName);
            assertEquals(OSS_TEST_REGION, loc);
        } catch (OSSException ex) {
            fail("Unable to get bucket location with default credentials.");
        }

        // Switch to another default credentials that belongs to the same user acount.
        Credentials defaultCreds2 = new DefaultCredentials(OSS_TEST_ACCESS_KEY_ID_1, OSS_TEST_ACCESS_KEY_SECRET_1);
        ossClient.switchCredentials(defaultCreds2);
        defaultCreds2 = credsProvider.getCredentials();
        assertEquals(OSS_TEST_ACCESS_KEY_ID_1, defaultCreds2.getAccessKeyId());
        assertEquals(OSS_TEST_ACCESS_KEY_SECRET_1, defaultCreds2.getSecretAccessKey());

        // Verify another default credentials under default endpoint
        try {
            String loc = ossClient.getBucketLocation(bucketName);
            assertEquals(OSS_TEST_REGION, loc);
        } catch (OSSException ex) {
            restoreDefaultCredentials();
            fail("Unable to get bucket location with another default credentials.");
        }

        // Switch to second credentials that belongs to another user acount,
        // Note that the default credentials are only valid under default endpoint 
        // and the second credentials are only valid under second endpoint.
        Credentials secondCreds = new DefaultCredentials(OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
        ossClient.switchCredentials(secondCreds);
        secondCreds = credsProvider.getCredentials();
        assertEquals(OSS_TEST_ACCESS_KEY_ID, secondCreds.getAccessKeyId());
        assertEquals(OSS_TEST_ACCESS_KEY_SECRET, secondCreds.getSecretAccessKey());

        // Verify second credentials under default endpoint
        //try {
        //    ossClient.getBucketLocation(bucketName);
        //    fail("Should not be able to get bucket location with second credentials.");
        //} catch (OSSException ex) {
        //    assertEquals(OSSErrorCode.INVALID_ACCESS_KEY_ID, ex.getErrorCode());
        //}
        
        // Switch to second endpoint
        ossClient.setEndpoint(OSS_TEST_ENDPOINT);
        
        // Verify second credentials under second endpoint
        try {
            assertEquals(OSS_TEST_ENDPOINT, ossClient.getEndpoint().toString());
            String loc = ossClient.getBucketLocation(bucketName);
            assertEquals(OSS_TEST_REGION, loc);
            
            // After switching both credentials and endpoint, the default OSSClient is the same
            // as the second OSSClient actually.
            assertEquals(OSS_TEST_ENDPOINT, ossClient.getEndpoint().toString());
            loc = ossClient.getBucketLocation(bucketName);
            assertEquals(OSS_TEST_REGION, loc);
        } catch (OSSException ex) {
            fail("Unable to create bucket with second credentials.");
        } finally {
            restoreDefaultCredentials();
            restoreDefaultEndpoint();
        }
    }
    
    @Test
    public void testSwitchInvalidCredentialsAndEndpoint() {
        CredentialsProvider credsProvider = ossClient.getCredentialsProvider();
        Credentials defaultCreds = credsProvider.getCredentials();
        assertEquals(OSS_TEST_ACCESS_KEY_ID_1, defaultCreds.getAccessKeyId());
        assertEquals(OSS_TEST_ACCESS_KEY_SECRET_1, defaultCreds.getSecretAccessKey());
        
        // Switch to invalid credentials
        Credentials invalidCreds = new DefaultCredentials(INVALID_ACCESS_ID, INVALID_ACCESS_KEY);
        ossClient.switchCredentials(invalidCreds);
        
        // Verify invalid credentials under default endpoint
        try {
            ossClient.getBucketLocation(bucketName);
            fail("Should not be able to get bucket location with invalid credentials.");
        } catch (OSSException ex) {
            assertEquals(OSSErrorCode.INVALID_ACCESS_KEY_ID, ex.getErrorCode());
        }
        
        // Switch to valid endpoint
        ossClient.setEndpoint(INVALID_ENDPOINT);
        
        // Verify second credentials under invalid endpoint
        try {
            ossClient.getBucketLocation(bucketName);
            fail("Should not be able to get bucket location with second credentials.");
        } catch (ClientException ex) {
            assertEquals(ClientErrorCode.UNKNOWN_HOST, ex.getErrorCode());
        } finally {
            restoreDefaultCredentials();
            restoreDefaultEndpoint();
        }
    }
    
    @Test
    public void testSwitchCredentialsSynchronously() throws Exception {
        /* Ensure credentials switching prior to credentials verification at first time */
        final Object ensureSwitchFirst = new Object();
        final Object verifySynchronizer = new Object();
        final Object switchSynchronizer = new Object();
        
        // Verify whether credentials switching work as expected
        Thread verifyThread = new Thread(new Runnable() {
            
            @Override
            public void run() {                
                synchronized (ensureSwitchFirst) {
                    if (!switchStarted) {
                        try {
                            ensureSwitchFirst.wait();
                        } catch (InterruptedException e) { }
                    }
                }
                
                int l = 0;
                do {
                    // Wait for credentials switching completion
                    synchronized (verifySynchronizer) {
                        try {
                            verifySynchronizer.wait();
                        } catch (InterruptedException e) { }
                    }
                    
                    CredentialsProvider credsProvider = ossClient.getCredentialsProvider();
                    Credentials currentCreds = credsProvider.getCredentials();
                    
                    try {
                        String loc = ossClient.getBucketLocation(bucketName);
                        assertEquals(OSS_TEST_REGION, loc);    
                        assertEquals(OSS_TEST_ACCESS_KEY_ID_1, currentCreds.getAccessKeyId());
                        assertEquals(OSS_TEST_ACCESS_KEY_SECRET_1, currentCreds.getSecretAccessKey());
                    } catch (OSSException ex) {
                        assertEquals(OSSErrorCode.INVALID_ACCESS_KEY_ID, ex.getErrorCode());
                        assertEquals(OSS_TEST_ACCESS_KEY_ID, currentCreds.getAccessKeyId());
                        assertEquals(OSS_TEST_ACCESS_KEY_SECRET, currentCreds.getSecretAccessKey());
                    }
                    
                    // Notify credentials switching
                    synchronized (switchSynchronizer) {
                        switchSynchronizer.notify();
                    }
                    
                } while (++l < loopTimes);
            }
        });
        
        // Switch credentials(including valid and invalid ones) synchronously
        Thread switchThread = new Thread(new Runnable() {
            
            @Override
            public void run() {        
                int l = 0;
                boolean firstSwitch = false;
                do {
                    Credentials secondCreds = new DefaultCredentials(OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
                    ossClient.switchCredentials(secondCreds);
                    CredentialsProvider credsProvider = ossClient.getCredentialsProvider();
                    secondCreds = credsProvider.getCredentials();
                    assertEquals(OSS_TEST_ACCESS_KEY_ID, secondCreds.getAccessKeyId());
                    assertEquals(OSS_TEST_ACCESS_KEY_SECRET, secondCreds.getSecretAccessKey());

                    if (!firstSwitch) {
                        synchronized (ensureSwitchFirst) {
                            switchStarted = true;
                            ensureSwitchFirst.notify();
                        }
                        firstSwitch = true;
                    }
                    
                    try {
                        Thread.sleep(switchInterval);
                    } catch (InterruptedException e) { }
                    
                    /* 
                     * Notify credentials verification and wait for next credentials switching.
                     * TODO: The two synchronized clauses below should be combined as atomic operation.
                     */
                    synchronized (verifySynchronizer) {
                        verifySynchronizer.notify();
                    }
                    synchronized (switchSynchronizer) {
                        try {
                            switchSynchronizer.wait();
                        } catch (InterruptedException e) {}
                    }
                } while (++l < loopTimes);
            }
        });
        
        verifyThread.start();
        switchThread.start();
        verifyThread.join();
        switchThread.join();
        
        restoreDefaultCredentials();
    }
    
    @Test
    public void testSwitchEndpointSynchronously() throws Exception {
        /* Ensure endpoint switching prior to endpoint verification at first time */
        final Object ensureSwitchFirst = new Object();
        final Object verifySynchronizer = new Object();
        final Object switchSynchronizer = new Object();
        
        // Verify whether endpoint switching work as expected
        Thread verifyThread = new Thread(new Runnable() {
            
            @Override
            public void run() {                
                synchronized (ensureSwitchFirst) {
                    if (!switchStarted) {
                        try {
                            ensureSwitchFirst.wait();
                        } catch (InterruptedException e) { }
                    }
                }
                
                int l = 0;
                do {
                    // Wait for endpoint switching completion
                    synchronized (verifySynchronizer) {
                        try {
                            verifySynchronizer.wait();
                        } catch (InterruptedException e) { }
                    }
                    
                    CredentialsProvider credsProvider = ossClient.getCredentialsProvider();
                    Credentials currentCreds = credsProvider.getCredentials();
                    
                    String loc = ossClient.getBucketLocation(bucketName);
                    assertEquals(OSS_TEST_REGION, loc);    
                    assertEquals(OSS_TEST_ACCESS_KEY_ID, currentCreds.getAccessKeyId());
                    assertEquals(OSS_TEST_ACCESS_KEY_SECRET, currentCreds.getSecretAccessKey());
                    
                    /*
                     * Since the default OSSClient is the same as the second OSSClient, let's
                     * do a simple verification. 
                     */
                    String secondLoc = ossClient.getBucketLocation(bucketName);
                    assertEquals(loc, secondLoc);
                    assertEquals(OSS_TEST_REGION, secondLoc);
                    CredentialsProvider secondCredsProvider = ossClient.getCredentialsProvider();
                    Credentials secondCreds = secondCredsProvider.getCredentials();
                    assertEquals(OSS_TEST_ACCESS_KEY_ID, secondCreds.getAccessKeyId());
                    assertEquals(OSS_TEST_ACCESS_KEY_SECRET, secondCreds.getSecretAccessKey());
                    
                    // Notify endpoint switching
                    synchronized (switchSynchronizer) {
                        restoreDefaultCredentials();
                        restoreDefaultEndpoint();
                        switchSynchronizer.notify();
                    }
                    
                } while (++l < loopTimes);
            }
        });
        
        // Switch endpoint synchronously
        Thread switchThread = new Thread(new Runnable() {
            
            @Override
            public void run() {        
                int l = 0;
                boolean firstSwitch = false;
                do {
                    /* 
                     * Switch both credentials and endpoint, now the default OSSClient is the same as 
                     * the second OSSClient actually.
                     */
                    Credentials secondCreds = new DefaultCredentials(OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
                    ossClient.switchCredentials(secondCreds);
                    ossClient.setEndpoint(OSS_TEST_ENDPOINT);

                    if (!firstSwitch) {
                        synchronized (ensureSwitchFirst) {
                            switchStarted = true;
                            ensureSwitchFirst.notify();
                        }
                        firstSwitch = true;
                    }
                    
                    try {
                        Thread.sleep(switchInterval);
                    } catch (InterruptedException e) { }
                    
                    /* 
                     * Notify credentials verification and wait for next credentials switching.
                     * TODO: The two synchronized clauses below should be combined as atomic operation.
                     */
                    synchronized (verifySynchronizer) {
                        verifySynchronizer.notify();
                    }
                    synchronized (switchSynchronizer) {
                        try {
                            switchSynchronizer.wait();
                        } catch (InterruptedException e) {}
                    }
                } while (++l < loopTimes);
            }
        });
        
        verifyThread.start();
        switchThread.start();
        verifyThread.join();
        switchThread.join();
        
        restoreDefaultCredentials();
        restoreDefaultEndpoint();
    }
    
    private static void restoreDefaultCredentials() {
        Credentials credentials = new DefaultCredentials(OSS_TEST_ACCESS_KEY_ID, OSS_TEST_ACCESS_KEY_SECRET);
        ossClient.switchCredentials(credentials);
    }

    private static void restoreDefaultEndpoint() {
        ossClient.setEndpoint(OSS_TEST_ENDPOINT);
    }

}
