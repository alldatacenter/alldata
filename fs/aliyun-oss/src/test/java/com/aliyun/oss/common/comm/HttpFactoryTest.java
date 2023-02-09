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

package com.aliyun.oss.common.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Test;

import com.aliyun.oss.HttpMethod;

public class HttpFactoryTest {

    @Test
    public void testCreateHttpRequest() throws Exception {
        ExecutionContext context = new ExecutionContext();
        String url = "http://127.0.0.1";
        String content = "This is a test request";
        byte[] contentBytes = null;
        try {
            contentBytes = content.getBytes(context.getCharset());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        HttpRequestFactory factory = new HttpRequestFactory();

        ServiceClient.Request request = new ServiceClient.Request();
        request.setUrl(url);

        HttpRequestBase httpRequest = null;

        // GET
        request.setMethod(HttpMethod.GET);
        httpRequest = factory.createHttpRequest(request, context);
        HttpGet getMethod = (HttpGet)httpRequest;
        assertEquals(url, getMethod.getURI().toString());

        // DELETE
        request.setMethod(HttpMethod.DELETE);
        httpRequest = factory.createHttpRequest(request, context);
        HttpDelete delMethod = (HttpDelete)httpRequest;
        assertEquals(url, delMethod.getURI().toString());

        // HEAD
        request.setMethod(HttpMethod.HEAD);
        httpRequest = factory.createHttpRequest(request, context);
        HttpHead headMethod = (HttpHead)httpRequest;
        assertEquals(url, headMethod.getURI().toString());

        //POST
        request.setContent(new ByteArrayInputStream(contentBytes));
        request.setContentLength(contentBytes.length);
        request.setMethod(HttpMethod.POST);
        httpRequest = factory.createHttpRequest(request, context);
        HttpPost postMethod = (HttpPost)httpRequest;

        assertEquals(url, postMethod.getURI().toString());
        HttpEntity entity = postMethod.getEntity();

        try {
            assertEquals(content, readSting(entity.getContent()));
        } catch (IllegalStateException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        //PUT
        request.setContent(new ByteArrayInputStream(contentBytes));
        request.setContentLength(contentBytes.length);
        request.setMethod(HttpMethod.PUT);
        httpRequest = factory.createHttpRequest(request, context);
        HttpPut putMethod = (HttpPut)httpRequest;

        assertEquals(url, putMethod.getURI().toString());
        entity = putMethod.getEntity();
        try {
            assertEquals(content, readSting(entity.getContent()));
        } catch (IllegalStateException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        try {
            request.close();
        } catch (IOException e) { }
    }

    private String readSting(InputStream input){
        InputStreamReader reader = new InputStreamReader(input);
        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
        finally{
            try {
                br.close();
                reader.close();
            } catch (IOException e) {
            }
        }
    }
}
