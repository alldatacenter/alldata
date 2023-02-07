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

package org.apache.inlong.tubemq.corerpc.netty;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.inlong.tubemq.corerpc.RpcDataPack;
import org.junit.Assert;
import org.junit.Test;

/**
 * NettyProtocolEncoder test.
 */
public class NettyProtocolEncoderTest {

    @Test
    public void encode() {
        NettyProtocolEncoder nettyProtocolEncoder = new NettyProtocolEncoder();
        // build RpcDataPack
        RpcDataPack obj = new RpcDataPack();
        // set serial number
        obj.setSerialNo(123);
        List<ByteBuffer> dataList = new LinkedList<>();
        dataList.add(ByteBuffer.wrap("abc".getBytes()));
        dataList.add(ByteBuffer.wrap("def".getBytes()));
        // append data list.
        obj.setDataLst(dataList);
        List<Object> out = new ArrayList<>();
        try {
            // encode data
            nettyProtocolEncoder.encode(null, obj, out);
            ByteBuf buf = (ByteBuf) out.get(0);
            // read data.
            int i = buf.readInt();
            i = buf.readInt();
            Assert.assertEquals(123, i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
