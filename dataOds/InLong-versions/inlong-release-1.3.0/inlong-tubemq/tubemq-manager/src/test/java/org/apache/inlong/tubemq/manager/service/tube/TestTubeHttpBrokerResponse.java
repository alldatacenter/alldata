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

package org.apache.inlong.tubemq.manager.service.tube;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class TestTubeHttpBrokerResponse {

    private final Gson gson = new Gson();

    @Test
    public void testJsonStr() {
        String jsonStr = "{\"code\":0,\"errMsg\":\"OK\",\"data\":"
                + "[{\"brokerId\":136,\"brokerIp\":\"127.0.0.1\","
                + "\"brokerPort\":8123,\"manageStatus\":\"online\","
                + "\"runStatus\":\"notRegister\",\"subStatus\":\"processing_event\","
                + "\"stepOp\":32,\"isConfChanged\":\"true\",\"isConfLoaded\":\"false\","
                + "\"isBrokerOnline\":\"false\",\"brokerVersion\":\"-\","
                + "\"acceptPublish\":\"false\",\"acceptSubscribe\":\"false\"}]}";
        TubeHttpBrokerInfoList brokerInfoList =
                gson.fromJson(jsonStr, TubeHttpBrokerInfoList.class);
        Assert.assertEquals(1, brokerInfoList.getData().size());
        Assert.assertEquals(0, brokerInfoList.getCode());
        Assert.assertEquals("OK", brokerInfoList.getErrMsg());
        Assert.assertTrue(brokerInfoList.getData().get(0).isConfChanged());
        Assert.assertFalse(brokerInfoList.getData().get(0).isAcceptPublish());
        Assert.assertFalse(brokerInfoList.getData().get(0).isBrokerOnline());
    }
}
