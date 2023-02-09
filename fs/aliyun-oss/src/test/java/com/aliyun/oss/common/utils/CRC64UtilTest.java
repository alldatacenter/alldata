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


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CRC64UtilTest {
    @Test
    public void testCRC64() {
        String data1 = "123456789";
        String data2 = "This is a test of the emergency broadcast system.";
        CRC64 crc64;

        crc64 = new CRC64(0);
        crc64.update(data1.getBytes(), data1.length());
        long pat = Long.valueOf("-7395533204333446662");
        assertEquals(pat, crc64.getValue());

        crc64 = new CRC64(data2.getBytes(), data2.length());
        pat = Long.valueOf("2871916124362751090");
        assertEquals(pat, crc64.getValue());

        crc64.reset();
        crc64.update(data1.getBytes(), data1.length());
        pat = Long.valueOf("-7395533204333446662");
        assertEquals(pat, crc64.getValue());

        byte[] init = new byte[4];
        init[0] = init[1] = init[2] = init[3] = 0;
        crc64 = CRC64.fromBytes(init);
        assertEquals(0, crc64.getValue());


        String total = data1 + data2;
        CRC64 crc1 = new CRC64();
        crc1.update(data1.getBytes(), data1.length());

        CRC64 crc2 = new CRC64();
        crc2.update(data2.getBytes(), data2.length());

        CRC64 crc3 = new CRC64();
        crc3.update(total.getBytes(), total.length());

        CRC64 crc4 = CRC64.combine(crc1, crc2, data2.length());
        assertEquals(crc3.getValue(), crc4.getValue());

        CRC64 crc5 = CRC64.combine(crc1, crc2, 0);
        assertEquals(crc1.getValue(), crc5.getValue());

        assertEquals(2, CRC64.combine(2, 3, 0));

        assertTrue(crc4.getBytes().length > 0);
    }
}
