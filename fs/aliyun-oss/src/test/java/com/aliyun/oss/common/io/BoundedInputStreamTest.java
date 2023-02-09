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
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BoundedInputStreamTest {
    @Test
    public void testBoundedInputStream() {
        String data = "Test";
        BoundedInputStream input;
        int ret;

        try {
            byte[] out = new byte[1];
            input = new BoundedInputStream(new ByteArrayInputStream(data.getBytes()), 2);
            input.mark(10);
            ret = input.read(out);
            assertEquals(1, ret);
            ret = input.read();
            assertEquals('e', ret);
            ret = input.read();
            assertEquals(-1, ret);

            assertEquals(0, input.available());
            input.reset();
            input.available();

            input.skip(1);
            ret = input.read();
            assertEquals('e', ret);

            input.setPropagateClose(false);
            assertEquals(false, input.isPropagateClose());
            input.close();
            input.setPropagateClose(true);
            assertEquals(true, input.isPropagateClose());

            input.toString();

        } catch (IOException e) {
            assertTrue(false);
        }

        try {
            byte[] out = new byte[10];
            input = new BoundedInputStream(new ByteArrayInputStream(data.getBytes()));
            ret = input.read(out);
            assertEquals(data.length(), ret);
        } catch (Exception e) {
            assertTrue(false);
        }
    }
}
