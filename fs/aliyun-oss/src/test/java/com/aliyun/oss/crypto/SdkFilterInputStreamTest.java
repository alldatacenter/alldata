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
import java.util.Arrays;
import org.junit.Test;
import com.aliyun.oss.ClientErrorCode;
import com.aliyun.oss.ClientException;
import junit.framework.Assert;

public class SdkFilterInputStreamTest {
    @Test
    public void testSdkAbortInputStream() {
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
         * Test the wrapped stream reading, the intterupt opreation will be effected
         * every time.
         */
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 1000; i++) {
                        InputStream in = new SdkFilterInputStream(new ByteArrayInputStream(content.getBytes()));
                        while (in.read() != -1) {
                        }
                    }
                    Assert.fail("This thread should be abort during read inputStream...");
                } catch (Exception e) {
                    if (e instanceof ClientException) {
                        // Expected excption
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

    @Test
    public void testReadInputStream() {
        String content = "012345678901234567890123456789012345678901234567890123456789";
        try {
            SdkFilterInputStream in = new SdkFilterInputStream(new ByteArrayInputStream(content.getBytes()));
            Assert.assertEquals(in.available(), content.length());
            Assert.assertEquals(in.isAborted(), false);
            in.skip(1);
            int ret = in.read();
            Assert.assertEquals(ret, content.charAt(1));
            String str = readInputStream(in);
            Assert.assertEquals(content.substring(2), str);
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testMarkReset() {
        String content = "hdfyqlmrwbdskfnsdkfbs9hmgdr61axlodfsklfjkslvmdklu0whdfyqlmrwbdskfnsdkfbskfh"
                + "isfdfsklfjkslvmdklu0wrwurjjdnksh098j62kfgjsdfbsj4427gc1sfbjsfsj123y214y2hujnhdfyq";
        try {
            SdkFilterInputStream in = new SdkFilterInputStream(new ByteArrayInputStream(content.getBytes()));
            Assert.assertEquals(in.getDelegateStream().markSupported(), true);
            Assert.assertEquals(in.markSupported(), true);

            in.mark(100);
            byte[] buffer = new byte[50];
            int len = in.read(buffer, 0, 40);

            in.reset();
            byte[] buffer2 = new byte[50];
            int len2 = in.read(buffer2, 0, 40);
            Assert.assertEquals(len, len2);
            Assert.assertTrue(Arrays.equals(buffer, buffer2));
            in.release();
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
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
