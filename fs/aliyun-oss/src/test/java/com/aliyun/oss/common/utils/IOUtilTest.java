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

import java.io.*;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;;

public class IOUtilTest {
    @Test
    public void testBase64String() {
        String dataString = "OssService";
        byte[] expectByteData = dataString.getBytes();
        InputStream inStream = new ByteArrayInputStream(expectByteData);
        byte[] byteData = null;
        try {
            byteData = IOUtils.readStreamAsByteArray(inStream);
            assertArrayEquals(byteData, expectByteData);
            byteData = IOUtils.readStreamAsByteArray(null);
            assertArrayEquals(byteData, new byte[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        IOUtils.safeClose(inStream);
        OutputStream outStream = new ByteArrayOutputStream();
        IOUtils.safeClose(outStream);
    }

    @Test
    public void testReadStreamAsString() {
        try {
            assertEquals("", IOUtils.readStreamAsString(null, "utf8"));
        } catch (IOException e) {
            assertTrue(false);
        }
    }

    @Test
    public void testMiscFunctions() {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        IOUtils.safeClose(inputStream);
        IOUtils.safeClose(outputStream);

        File file = null;
        assertEquals(false, IOUtils.checkFile(file));

        byte[] data = new byte[10];
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        assertEquals(null, IOUtils.getCRCValue(is));
    }

}
