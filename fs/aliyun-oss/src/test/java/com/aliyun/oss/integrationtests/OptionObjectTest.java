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

import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OptionsRequest;

public class OptionObjectTest extends TestBase {
    
    @Test
    public void testOptionObject() {
        List<String> existingKeys = new ArrayList<String>();
        final String key = "option-object";
        existingKeys.add(key);
        
        if (!batchPutObject(ossClient, bucketName, existingKeys)) {
            Assert.fail("batch put object failed");
        }
        
        final String origin = "http://www.example.com";
        final String requestHeaders = "x-oss-test";
        OptionsRequest optionsRequest = new OptionsRequest(bucketName, key);
        optionsRequest.setOrigin(origin);
        optionsRequest.setRequestHeaders(requestHeaders);
        optionsRequest.setRequestMethod(HttpMethod.PUT);
        
        // Disable bucket cors, return 403 Forbidden 
        try {
            ossClient.optionsObject(optionsRequest);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, e.getErrorCode());
        }
        
        /*
        // Enable bucket cors
        SetBucketCORSRequest setBucketCORSRequest = new SetBucketCORSRequest(bucketName);
        
        CORSRule r0 = new CORSRule();
        r0.addAllowdOrigin("http://www.example.com");
        r0.addAllowedMethod("PUT");
        r0.addExposeHeader("x-oss-test");
        setBucketCORSRequest.addCorsRule(r0);
        
        secondClient.setBucketCORS(setBucketCORSRequest);
        
        // Now options object will be successful
        ResponseMessage response = secondClient.optionsObject(optionsRequest);
        Map<String, String> responseHeaders =  response.getHeaders();
        Assert.assertEquals(origin, responseHeaders.get(OSSHeaders.ORIGIN));
        Assert.assertEquals(requestHeaders, responseHeaders.get(OSSHeaders.ACCESS_CONTROL_REQUEST_HEADER));
        Assert.assertEquals(HttpMethod.PUT, responseHeaders.get(OSSHeaders.ACCESS_CONTROL_ALLOW_METHODS));
        */
    }
}
