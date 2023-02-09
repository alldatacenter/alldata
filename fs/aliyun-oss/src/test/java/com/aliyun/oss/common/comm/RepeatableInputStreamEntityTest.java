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

package com.aliyun.oss.common.comm;

import junit.framework.Assert;
import org.junit.Test;
import com.aliyun.oss.common.comm.RepeatableInputStreamEntity.NoAutoClosedInputStreamEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class RepeatableInputStreamEntityTest {

    @Test
    public void testNoAutoClosedInputStreamEntity() {
        try {
            NoAutoClosedInputStreamEntity noAutoClosedInputStreamEntity = new NoAutoClosedInputStreamEntity(null, 1);
        } catch (Exception e) {
            // expected exception.
        }

        InputStream in = new ByteArrayInputStream("123".getBytes());
        NoAutoClosedInputStreamEntity inputStreamEntity = null;

        try {
            inputStreamEntity = new NoAutoClosedInputStreamEntity(in, 3);
            Assert.assertTrue(inputStreamEntity.isStreaming());
            Assert.assertFalse(inputStreamEntity.isRepeatable());
            Assert.assertEquals(3, inputStreamEntity.getContentLength());
            Assert.assertNotNull(inputStreamEntity.getContent());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            OutputStream out = null;
            inputStreamEntity.writeTo(out);
        } catch (Exception e) {
            // expected exception.
        }
    }

}
