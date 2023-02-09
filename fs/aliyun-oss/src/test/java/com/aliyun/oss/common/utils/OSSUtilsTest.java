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

package com.aliyun.oss.common.utils;


import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.internal.OSSUtils;
import junit.framework.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OSSUtilsTest {
    @Test
    public void testOSSUtils() {
        assertEquals(OSSUtils.validateBucketName(null), false);

        assertEquals(OSSUtils.validateObjectKey(null), false);
        assertEquals(OSSUtils.validateObjectKey(""), false);
        assertEquals(OSSUtils.validateObjectKey("/key-with-invalid-prefix"), true);
        assertEquals(OSSUtils.validateObjectKey("\\key-with-invalid-prefix"), false);

        try {
            OSSUtils.ensureObjectKeyValid("");
            assertTrue(false);
        }catch (Exception ex) {
            Assert.assertTrue(true);
        }

        try {
            OSSUtils.ensureLiveChannelNameValid("");
            assertTrue(false);
        }catch (Exception ex) {
            Assert.assertTrue(true);
        }

        assertEquals(OSSUtils.makeResourcePath("bucket", "key"),"bucket/key");
        assertEquals(OSSUtils.makeResourcePath("bucket", null),"bucket/");
        assertEquals(OSSUtils.makeResourcePath(null, "key"),null);
        assertEquals(OSSUtils.makeResourcePath("key/123/"),"key/123/" );
        assertEquals(OSSUtils.makeResourcePath("/key/123/"),"%2Fkey%2F123%2F" );
        assertEquals(OSSUtils.makeResourcePath("bucket", "key/123/"),"bucket/key/123/" );
        assertEquals(OSSUtils.makeResourcePath("bucket", "/key/123/"),"bucket/%2Fkey%2F123%2F" );

        assertEquals(OSSUtils.trimQuotes(null),null);
        assertEquals(OSSUtils.trimQuotes("\"test"),"test");
        assertEquals(OSSUtils.trimQuotes("test\""),"test");
        assertEquals(OSSUtils.trimQuotes("test"),"test");
        assertEquals(OSSUtils.trimQuotes("\"test\""),"test");
    }

    @Test
    public void testToURI() {
        String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
        String defaultProtocol = "https";

        URI res = OSSUtils.toEndpointURI(endpoint, defaultProtocol);
        assertEquals(endpoint, res.toString());
    }

    @Test
    public void testToURI2() {
        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        String defaultProtocol = "https";

        URI res = OSSUtils.toEndpointURI(endpoint, defaultProtocol);
        assertEquals("https://oss-cn-hangzhou.aliyuncs.com", res.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToURI3() {
        String endpoint = "oss-cn-hangzhou.aliyuncs.com";
        String defaultProtocol = "";

        URI res = OSSUtils.toEndpointURI(endpoint, defaultProtocol);
        assertEquals("https://oss-cn-hangzhou.aliyuncs.com", res.toString());
    }

    @Test
    public void testToURI4() {
        String endpoint = null;
        String defaultProtocol = "http";

        try {
            URI res = OSSUtils.toEndpointURI(endpoint, defaultProtocol);
            assertEquals(null, res);
        } catch (Exception e) {
            assertTrue(e instanceof  NullPointerException);
        }
    }
}
