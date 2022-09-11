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

package org.apache.inlong.tubemq.server.master.stats;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * MasterSrvStatsHolder test.
 */
public class MasterSrvStatsHolderTest {

    @Test
    public void testMasterSrvStatsHolder() {
        // add new group 2, new client-balance group 1, new consumer 4
        MasterSrvStatsHolder.incConsumerCnt(true, true);
        MasterSrvStatsHolder.incConsumerCnt(true, false);
        MasterSrvStatsHolder.incConsumerCnt(false, true);
        MasterSrvStatsHolder.incConsumerCnt(false, false);
        // dec consumer 8, timeout 4, group empty 4, client-balance group 2
        MasterSrvStatsHolder.decConsumerCnt(false, false, false);
        MasterSrvStatsHolder.decConsumerCnt(false, false, true);
        MasterSrvStatsHolder.decConsumerCnt(false, true, false);
        MasterSrvStatsHolder.decConsumerCnt(false, true, true);
        MasterSrvStatsHolder.decConsumerCnt(true, false, true);
        MasterSrvStatsHolder.decConsumerCnt(true, false, false);
        MasterSrvStatsHolder.decConsumerCnt(true, true, true);
        MasterSrvStatsHolder.decConsumerCnt(true, true, false);
        // desc group 4, timeout 2, client-balance 2
        MasterSrvStatsHolder.decConsumeGroupCnt(false, false);
        MasterSrvStatsHolder.decConsumeGroupCnt(false, true);
        MasterSrvStatsHolder.decConsumeGroupCnt(true, true);
        MasterSrvStatsHolder.decConsumeGroupCnt(true, false);
        // add server-balance dis-connect consumer 2, dec 1
        MasterSrvStatsHolder.incSvrBalDisConConsumerCnt();
        MasterSrvStatsHolder.incSvrBalDisConConsumerCnt();
        MasterSrvStatsHolder.decSvrBalDisConConsumerCnt();
        // add server-balance connect consumer 1, dec 1
        MasterSrvStatsHolder.incSvrBalConEventConsumerCnt();
        MasterSrvStatsHolder.decSvrBalConEventConsumerCnt();
        // add or dec producer
        MasterSrvStatsHolder.incProducerCnt();
        MasterSrvStatsHolder.incProducerCnt();
        MasterSrvStatsHolder.incProducerCnt();
        MasterSrvStatsHolder.decProducerCnt(false);
        MasterSrvStatsHolder.decProducerCnt(true);
        // add or dec broker
        MasterSrvStatsHolder.incBrokerConfigCnt();
        MasterSrvStatsHolder.incBrokerConfigCnt();
        MasterSrvStatsHolder.incBrokerConfigCnt();
        MasterSrvStatsHolder.decBrokerConfigCnt();
        //
        MasterSrvStatsHolder.incBrokerOnlineCnt();
        MasterSrvStatsHolder.incBrokerOnlineCnt();
        MasterSrvStatsHolder.incBrokerOnlineCnt();
        MasterSrvStatsHolder.decBrokerOnlineCnt(false);
        MasterSrvStatsHolder.decBrokerOnlineCnt(true);
        MasterSrvStatsHolder.incBrokerAbnormalCnt();
        MasterSrvStatsHolder.incBrokerForbiddenCnt();
        MasterSrvStatsHolder.decBrokerAbnormalCnt();
        MasterSrvStatsHolder.decBrokerForbiddenCnt();
        // check result
        Map<String, Long> retMap = new LinkedHashMap<>();
        MasterSrvStatsHolder.getValue(retMap);
        Assert.assertNotNull(retMap.get("reset_time"));
        Assert.assertEquals(-6, retMap.get("csm_online_group_cnt").longValue());
        Assert.assertEquals(4, retMap.get("csm_group_timeout_cnt").longValue());
        Assert.assertEquals(-3, retMap.get("csm_client_bal_group_cnt").longValue());
        Assert.assertEquals(-4, retMap.get("consumer_online_cnt").longValue());
        Assert.assertEquals(4, retMap.get("consumer_timeout_cnt").longValue());
        Assert.assertEquals(0, retMap.get("consumer_con_event_cnt").longValue());
        Assert.assertEquals(1, retMap.get("consumer_discon_event_cnt").longValue());
        Assert.assertEquals(1, retMap.get("producer_online_cnt").longValue());
        Assert.assertEquals(1, retMap.get("producer_timeout_cnt").longValue());
        Assert.assertEquals(2, retMap.get("broker_configured_cnt").longValue());
        Assert.assertEquals(1, retMap.get("broker_online_cnt").longValue());
        Assert.assertEquals(1, retMap.get("broker_timeout_cnt").longValue());
        Assert.assertEquals(0, retMap.get("broker_abnormal_cnt").longValue());
        Assert.assertEquals(0, retMap.get("broker_forbidden_cnt").longValue());
        Assert.assertEquals(0, retMap.get("server_balance_normal_count").longValue());
        Assert.assertEquals(0, retMap.get("server_balance_reset_count").longValue());
        retMap.clear();
        // get and snapshot content by StringBuilder
        StringBuilder strBuff = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        MasterSrvStatsHolder.snapShort(strBuff);
        // System.out.println(strBuff.toString());
        strBuff.delete(0, strBuff.length());
        MasterSrvStatsHolder.updSvrBalanceDurations(32);
        MasterSrvStatsHolder.updSvrBalResetDurations(100);
        MasterSrvStatsHolder.getValue(retMap);
        Assert.assertEquals(-6, retMap.get("csm_online_group_cnt").longValue());
        Assert.assertEquals(0, retMap.get("csm_group_timeout_cnt").longValue());
        Assert.assertEquals(-3, retMap.get("csm_client_bal_group_cnt").longValue());
        Assert.assertEquals(-4, retMap.get("consumer_online_cnt").longValue());
        Assert.assertEquals(0, retMap.get("consumer_timeout_cnt").longValue());
        Assert.assertEquals(0, retMap.get("consumer_con_event_cnt").longValue());
        Assert.assertEquals(1, retMap.get("consumer_discon_event_cnt").longValue());
        Assert.assertEquals(1, retMap.get("producer_online_cnt").longValue());
        Assert.assertEquals(0, retMap.get("producer_timeout_cnt").longValue());
        Assert.assertEquals(2, retMap.get("broker_configured_cnt").longValue());
        Assert.assertEquals(1, retMap.get("broker_online_cnt").longValue());
        Assert.assertEquals(0, retMap.get("broker_timeout_cnt").longValue());
        Assert.assertEquals(0, retMap.get("broker_abnormal_cnt").longValue());
        Assert.assertEquals(0, retMap.get("broker_forbidden_cnt").longValue());
        Assert.assertEquals(1, retMap.get("server_balance_normal_count").longValue());
        Assert.assertEquals(1, retMap.get("server_balance_reset_count").longValue());
    }
}
