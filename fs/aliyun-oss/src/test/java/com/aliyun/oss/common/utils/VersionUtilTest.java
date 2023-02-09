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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionUtilTest {
    @Test
    public void testGetDefaultUserAgent() {
        String userAgent = VersionInfoUtils.getDefaultUserAgent();
        assertTrue(userAgent.startsWith("aliyun-sdk-java/3.16.0("));
        assertEquals(userAgent.split("/").length, 4);
        assertEquals(userAgent.split(";").length, 2);
        assertEquals(userAgent.split("\\(").length, 2);
        assertEquals(userAgent.split("\\)").length, 1);
    }

    @Test
    public void testGetVersion() {
        String version = VersionInfoUtils.getVersion();
        assertEquals("3.16.0", version);
    }
}

