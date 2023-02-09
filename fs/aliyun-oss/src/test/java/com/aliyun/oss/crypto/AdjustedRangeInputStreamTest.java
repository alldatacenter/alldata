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

package com.aliyun.oss.crypto;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Test;
import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;
import junit.framework.Assert;

public class AdjustedRangeInputStreamTest {
    @Test
    public void testReadBuffer() {
        String content = "012345678901234567890123456789012345678901234567890123456789";
        try {
            int begin = 0;
            int end = 37;
            AdjustedRangeInputStream adjIs = new AdjustedRangeInputStream(new ByteArrayInputStream(content.getBytes()), begin, end);
            Assert.assertEquals(adjIs.available(), end - begin + 1);
            String str = readInputStream(adjIs);
            Assert.assertEquals(content.substring(begin, end + 1), str);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            int begin = 1;
            int end = 37;
            AdjustedRangeInputStream adjIs = new AdjustedRangeInputStream(new ByteArrayInputStream(content.getBytes()), begin, end);
            String str = readInputStream(adjIs);
            Assert.assertEquals(content.substring(begin, end + 1), str);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            int begin = 17;
            int end = 37;
            InputStream in = new ByteArrayInputStream(content.getBytes());
            // Simulate it as a decipher stream.
            in.skip(16);
            AdjustedRangeInputStream adjIs = new AdjustedRangeInputStream(in, begin, end);
            String str = readInputStream(adjIs);
            Assert.assertEquals(content.substring(begin, end + 1), str);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testReadOneByte() {
        String content = "012345678901234567890123456789012345678901234567890123456789";
        try {
            int begin = 1;
            int end = 37;
            AdjustedRangeInputStream in = new AdjustedRangeInputStream(new ByteArrayInputStream(content.getBytes()), begin, end);
            int ret = in.read();
            Assert.assertEquals(ret, content.charAt(begin));

        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAbortInputStream() {
        final String content = "qwertyuuihonkffttdctgvbkhiijojilkmkeowirnskdnsiwi93729741084084875dfdf212fa";

        /**
         * Test the normal inputStream reading, the intterupt opreation will not be
         * effected when the state of the thread is not WAITING or TIMED_WAITING.
         */
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    for (i = 0; i < 1000; i++) {
                        InputStream in = new ByteArrayInputStream(content.getBytes());
                        while (in.read() != -1) {
                        }
                    }
                    // During the inputStream reading, this thread would not responds the interrupt request.
                    Assert.assertEquals(1000, i);
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.fail(e.getMessage());
                }
            }
        }, "thread1");

        /**
         * Test the wrapped stream reading, the intterupt opreation will be effected every time.
         */
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 1000; i++) {
                        InputStream in = new AdjustedRangeInputStream(new ByteArrayInputStream(content.getBytes()), 0, 10);
                        while (in.read() != -1) {
                        }
                    }
                    Assert.fail("This thread should be abort during read inputStream...");
                } catch (Exception e) {
                    if (e instanceof ClientException) {
                        // Expected exception.
                        Assert.assertEquals(((ClientException)e).getErrorCode(), ClientErrorCode.INPUTSTREAM_READING_ABORTED);
                    } else {
                        e.printStackTrace();
                        Assert.fail(e.getMessage());
                    }
                }
            }
        }, "thread2");

        thread1.start();
        thread2.start();
        thread1.interrupt();
        thread2.interrupt();
    }

    private static String readInputStream(InputStream in) throws Throwable {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuffer buffer = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
        return buffer.toString();
    }
}
