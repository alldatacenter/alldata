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
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import com.aliyun.oss.common.auth.PublicKey;
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.IniEditor;
import com.aliyuncs.exceptions.ClientException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

public class TestBase {

    protected static final String BUCKET_NAME_PREFIX = "oss-java-sdk-";

    @BeforeClass
    public static void onlyOnceSetUp() {
        // for all tests
        resetTestConfig();
    }

    @AfterClass
    public static void onlyOncetearDown() {
        // for all tests
    }

    @Before
    public void setUp() {
        // for each case
        try {
            deleteAllPublicKey();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() {
        // for each case
    }

    public static void resetTestConfig() {
        if (TestConfig.RAM_REGION_ID == null) {
            TestConfig.RAM_REGION_ID = System.getenv().get("OSS_TEST_RAM_REGION");
        }

        if (TestConfig.ROOT_ACCESS_KEY_ID == null) {
            TestConfig.ROOT_ACCESS_KEY_ID = System.getenv().get("OSS_TEST_ACCESS_KEY_ID");
        }

        if (TestConfig.ROOT_ACCESS_KEY_SECRET == null) {
            TestConfig.ROOT_ACCESS_KEY_SECRET = System.getenv().get("OSS_TEST_ACCESS_KEY_SECRET");
        }

        if (TestConfig.USER_ACCESS_KEY_ID == null) {
            TestConfig.USER_ACCESS_KEY_ID = System.getenv().get("OSS_TEST_USER_ACCESS_KEY_ID");
        }

        if (TestConfig.USER_ACCESS_KEY_SECRET == null) {
            TestConfig.USER_ACCESS_KEY_SECRET = System.getenv().get("OSS_TEST_USER_ACCESS_KEY_SECRET");
        }

        if (TestConfig.RAM_ROLE_ARN == null) {
            TestConfig.RAM_ROLE_ARN = System.getenv().get("OSS_TEST_RAM_ROLE_ARN");
        }

        if (TestConfig.ECS_ROLE_NAME == null) {
            TestConfig.ECS_ROLE_NAME = System.getenv().get("OSS_TEST_ECS_ROLE_NAME");
        }

        if (TestConfig.PUBLIC_KEY_PATH == null) {
            TestConfig.PUBLIC_KEY_PATH = System.getenv().get("PUBLIC_KEY_PATH");
        }

        if (TestConfig.PRIVATE_KEY_PATH == null) {
            TestConfig.PRIVATE_KEY_PATH = System.getenv().get("PRIVATE_KEY_PATH");
        }

        if (TestConfig.OSS_ENDPOINT == null) {
            TestConfig.OSS_ENDPOINT = System.getenv().get("OSS_TEST_ENDPOINT");
        }

        if (TestConfig.OSS_BUCKET == null) {
            TestConfig.OSS_BUCKET = System.getenv().get("OSS_BUCKET");
        }

        if (TestConfig.OSS_AUTH_SERVER_HOST == null) {
            TestConfig.OSS_AUTH_SERVER_HOST = System.getenv().get("OSS_TEST_AUTH_SERVER_HOST");
        }
    }

    public static void deleteAllPublicKey() throws ClientException {
        List<PublicKey> publicKeys = AuthUtils.listPublicKeys(TestConfig.RAM_REGION_ID,
                TestConfig.ROOT_ACCESS_KEY_ID, TestConfig.ROOT_ACCESS_KEY_SECRET);
        for (PublicKey pk : publicKeys) {
            AuthUtils.deletePublicKey(TestConfig.RAM_REGION_ID, TestConfig.ROOT_ACCESS_KEY_ID,
                    TestConfig.ROOT_ACCESS_KEY_SECRET, pk.getPublicKeyId());
        }
    }

    public static void generateProfileFile(String ProfileFilePath, String sectionName, Map<String, String> options)
            throws IOException {
        File profileFile = new File(ProfileFilePath);
        String parent = profileFile.getParent();
        File parentFile = new File(parent);

        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        if (profileFile.exists()) {
            profileFile.delete();
        }
        profileFile.createNewFile();

        IniEditor iniEditor = new IniEditor();
        iniEditor.load(profileFile);

        if (!iniEditor.hasSection(sectionName)) {
            iniEditor.addSection(sectionName);
        }

        for (Map.Entry<String, String> entry : options.entrySet()) {
            iniEditor.set(sectionName, entry.getKey(), entry.getValue());
        }

        iniEditor.save(profileFile);
    }

    /**
     * Set environment variables to include those in the given map.
     *
     * @param newenv the map of new variables to include.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void setEnv(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Remove the specified variables from the current map of environment
     * variables.
     *
     * @param vars the names of the variables to remove.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void unsetEnv(List<String> vars) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            for (String v : vars) {
                env.remove(v);
            }
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass
                    .getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            for (String v : vars) {
                cienv.remove(v);
            }
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        for (String v : vars) {
                            map.remove(v);
                        }
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public static String getRandomBucketName() {
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        String bucketName = BUCKET_NAME_PREFIX + ticks;
        return bucketName;
    }

    public static void waitForCacheExpiration(int durationSeconds) {
        try {
            Thread.sleep(durationSeconds * 1000);
        } catch (InterruptedException e) {
        }
    }
}