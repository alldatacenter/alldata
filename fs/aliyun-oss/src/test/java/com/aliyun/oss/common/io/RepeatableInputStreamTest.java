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

package com.aliyun.oss.common.io;

import com.aliyun.oss.common.comm.io.BoundedInputStream;
import com.aliyun.oss.common.comm.io.RepeatableBoundedFileInputStream;
import com.aliyun.oss.common.comm.io.RepeatableFileInputStream;
import com.aliyun.oss.common.comm.io.RepeatableInputStream;
import com.aliyun.oss.utils.ResourceUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import java.io.IOException;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RepeatableInputStreamTest {

    @Test
    public void testRepeatableInputStream() {
        String data = "OssService";
        ByteArrayInputStream byteInput = new ByteArrayInputStream(data.getBytes());
        RepeatableInputStream input = new RepeatableInputStream(byteInput, 20);

        assertEquals(true, input.markSupported());
        assertEquals(byteInput, input.getWrappedInputStream());

        input.mark(10);
        input.mark(30);

        try {
            byte[] out = new byte[8];
            int ret = input.read(out, 0, 8);
            assertEquals(8, ret);
            ret = input.read();
            assertEquals('c', ret);
            ret = input.read();
            ret = input.read();
            assertEquals(-1, ret);
            input.reset();
            input.available();
            input.close();
        } catch (IOException e) {
            assertTrue(false);
        }


        //
        try {
            input = new RepeatableInputStream(null, 20);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

    }

    @Test
    public void testRepeatableFileInputStream() {
        String path = ResourceUtils.getTestFilename("oss/example.jpg");
        try {
            FileInputStream inputFile = new FileInputStream(path);
            RepeatableFileInputStream input = new RepeatableFileInputStream(inputFile);

            assertEquals(inputFile, input.getWrappedInputStream());
            assertEquals(null, input.getFile());

            input.mark(10);
            int ret0 = input.read();
            int ret1 = input.read();
            input.reset();
            input.skip(1);
            ret0 = input.read();
            assertEquals(ret1, ret0);

            input.available();
            input.close();
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Test
    public void testRepeatableBoundedFileInputStream() {
        String path = ResourceUtils.getTestFilename("oss/example.jpg");
        try {
            FileInputStream inputFile = new FileInputStream(path);
            BoundedInputStream bInput = new BoundedInputStream(inputFile);
            RepeatableBoundedFileInputStream input = new RepeatableBoundedFileInputStream(bInput);

            assertEquals(bInput, input.getWrappedInputStream());

            int ret0 = input.read();
            int ret1 = input.read();
            input.reset();
            input.skip(1);
            ret0 = input.read();
            assertEquals(ret1, ret0);

            input.available();
            input.reset();
            input.close();
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}
