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
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AuditDataTest {
    @Test
    public void increaseResendTimes() {
        AuditApi.BaseCommand content = null;
        AuditData test = new AuditData(System.currentTimeMillis(), content);
        int resendTimes = test.increaseResendTimes();
        assertTrue(resendTimes == 1);
        resendTimes = test.increaseResendTimes();
        assertTrue(resendTimes == 2);
        resendTimes = test.increaseResendTimes();
        assertTrue(resendTimes == 3);
    }

    @Test
    public void getDataByte() {
        AuditApi.AuditMessageHeader.Builder headerBuilder = AuditApi.AuditMessageHeader.newBuilder();
        headerBuilder.setPacketId(1)
                .setSdkTs(0)
                .setThreadId("")
                .setIp("")
                .setDockerId("");
        AuditApi.AuditMessageBody.Builder bodyBuilder = AuditApi.AuditMessageBody.newBuilder();
        bodyBuilder.setAuditId("")
                .setInlongGroupId("")
                .setInlongStreamId("")
                .setLogTs(0)
                .setCount(0)
                .setSize(0)
                .setDelay(0);
        AuditApi.AuditRequest request = AuditApi.AuditRequest.newBuilder().setMsgHeader(headerBuilder.build())
                .addMsgBody(bodyBuilder.build()).build();
        AuditApi.BaseCommand baseCommand = AuditApi.BaseCommand.newBuilder().setAuditRequest(request).build();
        AuditData test = new AuditData(System.currentTimeMillis(), baseCommand);
        byte[] data = test.getDataByte();
        assertTrue(data.length > 0);
    }

    @Test
    public void addBytes() {
    }
}