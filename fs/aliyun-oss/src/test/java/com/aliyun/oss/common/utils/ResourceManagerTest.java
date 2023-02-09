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

import java.util.Locale;

import org.junit.Test;

public class ResourceManagerTest {

    @Test
    public void testResourceManager() {
        // TODO: User a test-specific resource
        String baseName = "common";
        ResourceManager rm;

        Locale currentLocale = Locale.getDefault();

        try {
            Locale.setDefault(Locale.ENGLISH);
            rm = ResourceManager.getInstance(baseName);
            assertEquals("Failed to parse the response result.", rm.getString("FailedToParseResponse"));
            assertEquals("Connection error due to: test.", rm.getFormattedString("ConnectionError", "test."));

            Locale.setDefault(Locale.CHINA);
            rm = ResourceManager.getInstance(baseName);
            assertEquals("返回结果无效，无法解析。", rm.getString("FailedToParseResponse"));
            assertEquals("网络连接错误，详细信息：测试。", rm.getFormattedString("ConnectionError", "测试。"));
        } finally {
            Locale.setDefault(currentLocale);
        }
    }
}
