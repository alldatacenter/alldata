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

import com.aliyun.oss.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static com.aliyun.oss.common.utils.AuthUtils.loadPrivateKeyFromFile;
import static com.aliyun.oss.common.utils.AuthUtils.loadPublicKeyFromFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuthUtilTest {

    @Test
    public void testLoadPublicKeyFromFile() {
        String data = null;
        String path = ResourceUtils.getTestFilename("oss/rsaPrivateKey.pem");

        try {
            data = loadPublicKeyFromFile(path);
            assertEquals(false, data.isEmpty());
        } catch (Exception e) {
            Assert.fail("could not here.");
        }

        try {
            data = loadPublicKeyFromFile("invalid path");
            Assert.fail("could not here.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testLoadPrivateKeyFromFile() {
        String data = null;
        String path = ResourceUtils.getTestFilename("oss/rsaPrivateKey.pem");
        try {
            data = loadPrivateKeyFromFile(path);
            assertEquals(false, data.isEmpty());
        } catch (Exception e) {
            Assert.fail("could not here.");
        }

        try {
            data = loadPrivateKeyFromFile("invalid path");
            Assert.fail("could not here.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testLoadNonRsaPrivateKeyFromFile() {
        File file = null;
        try {
            file = File.createTempFile("test-private-key", ".pem");
            file.deleteOnExit();
            String privateKeyContent = "-----BEGIN PRIVATE KEY-----\n" +
                    "abc\n" +
                    "-----END PRIVATE KEY-----";
            Writer writer = new OutputStreamWriter(new FileOutputStream(file));
            writer.write(privateKeyContent);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            String data = loadPrivateKeyFromFile(file.getAbsolutePath());
            assertEquals("abc\n", data);
        } catch (Exception e) {
            Assert.fail("load private key error.");
        }
    }

}
