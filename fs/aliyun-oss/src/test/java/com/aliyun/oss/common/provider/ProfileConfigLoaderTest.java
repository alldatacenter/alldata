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

package com.aliyun.oss.common.provider;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.aliyun.oss.common.auth.ProfileConfigLoader;
import com.aliyun.oss.common.utils.AuthUtils;
import junit.framework.Assert;
import org.junit.Test;

public class ProfileConfigLoaderTest extends TestBase {

    @Test
    public void testProfileConfigLoader() {
        try {
            // generate profile
            Map<String, String> options = new HashMap<String, String>();
            options.put(AuthUtils.OSS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
            options.put(AuthUtils.OSS_SECRET_ACCESS_KEY, TEST_ACCESS_KEY_SECRET);
            options.put(AuthUtils.OSS_SESSION_TOKEN, TEST_SECURITY_TOKEN);
            generateProfileFile(AuthUtils.DEFAULT_PROFILE_PATH,
                    AuthUtils.DEFAULT_SECTION_NAME, options);

            // load profiel
            ProfileConfigLoader profileLoader = new ProfileConfigLoader();
            Map<String, String> properties = profileLoader
                    .loadProfile(new File(AuthUtils.DEFAULT_PROFILE_PATH));

            // check
            Assert.assertEquals(TEST_ACCESS_KEY_ID, properties.get(AuthUtils.OSS_ACCESS_KEY_ID));
            Assert.assertEquals(TEST_ACCESS_KEY_SECRET, properties.get(AuthUtils.OSS_SECRET_ACCESS_KEY));
            Assert.assertEquals(TEST_SECURITY_TOKEN, properties.get(AuthUtils.OSS_SESSION_TOKEN));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private static final String TEST_ACCESS_KEY_ID = "AccessKeyId";
    private static final String TEST_ACCESS_KEY_SECRET = "AccessKeySecret";
    private static final String TEST_SECURITY_TOKEN = "SecurityToken";

}
