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
import org.junit.Assert;
import org.junit.Test;

public class TestTubeHttpTopicInfoList {

    private final Gson gson = new Gson();

    @Test
    public void testJsonStr() {
        String jsonStr = "{\"result\":true,\"errCode\":0,\"errMsg\":\"OK\",\""
                + "data\":[{\"topicName\":\"test1\",\"topicInfo\":[{\"topicName\":\"test1\",\""
                + "topicStatusId\":0,\"brokerId\":152509201,\"brokerIp\":\"127.0.0.1\",\""
                + "brokerPort\":8123,\"numPartitions\":1,\"unflushThreshold\":1000,\""
                + "unflushInterval\":10000,\"unFlushDataHold\":1000,\"deleteWhen\":\"\",\""
                + "deletePolicy\":\"delete,32h\",\"acceptPublish\":true,"
                + "\"acceptSubscribe\":true,\"numTopicStores\":1,\"memCacheMsgSizeInMB\":2,\""
                + "memCacheFlushIntvl\":20000,\"memCacheMsgCntInK\":10,"
                + "\"createUser\":\"Alice\",\"createDate\":\"20200917122645\","
                + "\"modifyUser\":\"Alice\",\"modifyDate\":\"20200917122645\","
                + "\"runInfo\":{\"acceptPublish\":true,\"acceptSubscribe\":true,"
                + "\"numPartitions\":1,\"numTopicStores\":1,"
                + "\"brokerManageStatus\":\"online\"}}]}]}";
        TubeHttpTopicInfoList topicInfoList = gson.fromJson(jsonStr, TubeHttpTopicInfoList.class);
        Assert.assertTrue(topicInfoList.isResult());
        Assert.assertEquals(0, topicInfoList.getErrCode());
        Assert.assertEquals(1, topicInfoList.getData().size());
        Assert.assertEquals("Alice", topicInfoList.getData().get(0)
                .getTopicInfo().get(0).getCreateUser());
        Assert.assertEquals("online", topicInfoList.getData().get(0)
                .getTopicInfo().get(0).getRunInfo().getBrokerManageStatus());
    }
}
