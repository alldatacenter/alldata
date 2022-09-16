/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.audit.util;

import org.apache.inlong.audit.protocol.AuditApi;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class AuditData implements Serializable {
    public static int HEAD_LENGTH = 4;
    private final long sdkTime;
    private final AuditApi.BaseCommand content;
    private AtomicInteger resendTimes = new AtomicInteger(0);

    /**
     * Constructor
     *
     * @param sdkTime
     * @param content
     */
    public AuditData(long sdkTime, AuditApi.BaseCommand content) {
        this.sdkTime = sdkTime;
        this.content = content;
    }

    /**
     * set resendTimes
     */
    public int increaseResendTimes() {
        return this.resendTimes.incrementAndGet();
    }

    /**
     * getDataByte
     *
     * @return
     */
    public byte[] getDataByte() {
        return addBytes(ByteBuffer.allocate(HEAD_LENGTH).putInt(content.toByteArray().length).array(),
                content.toByteArray());
    }

    /**
     * Concatenated byte array
     *
     * @param data1
     * @param data2
     * @return data1 and  data2 combined package result
     */
    public byte[] addBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }
}
