/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.codec;

import static org.junit.Assert.assertEquals;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corerpc.RpcConstants;
import org.junit.Test;

public class PbEnDecoderTest {

    @Test
    public void testPbEncodeAndDecoder() throws Exception {
        // mock a pb object
        ClientMaster.RegisterRequestP2M.Builder builder = ClientMaster.RegisterRequestP2M.newBuilder();
        builder.setClientId("10001");
        builder.setBrokerCheckSum(99);
        builder.setHostName("tube-test");
        ClientMaster.RegisterRequestP2M object = builder.build();
        // encode pb
        byte[] data = PbEnDecoder.pbEncode(object);

        // decode bytes
        ClientMaster.RegisterRequestP2M decodeObject = (ClientMaster.RegisterRequestP2M)
                PbEnDecoder.pbDecode(true, RpcConstants.RPC_MSG_MASTER_PRODUCER_REGISTER, data);

        assertEquals(decodeObject.getClientId(), object.getClientId());
        assertEquals(decodeObject.getBrokerCheckSum(), object.getBrokerCheckSum());
        assertEquals(decodeObject.getHostName(), object.getHostName());
    }

}
