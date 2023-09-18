/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.datax;

import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import junit.framework.TestCase;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-07 15:54
 **/
public class TestDataXJobConsumer extends TestCase {
    // TISTestCase {

    static {
        CenterResource.setNotFetchFromCenterRepository();
        HttpUtils.addMockGlobalParametersConfig();
        //  Config.setTestDataDir();
    }

    /**
     * 需要执行 TestDistributedOverseerDataXJobSubmit.testPushMsgToDistributeQueue() 作为测试配合
     *
     * @throws Exception
     */
    public void testConsumeDistributeMsg() throws Exception {

//        String[] args = new String[]{"192.168.28.200:2181/tis/cloud", "/datax/jobs"};
//        DataXJobConsumer.main(args);

        DataXJobConsumer consumer = DataXJobConsumer.getDataXJobConsumer("/datax/jobs", "192.168.28.200:2181/tis/cloud");
        assertNotNull(consumer);

        // dataXName:ttt,jobid:866,jobName:customer_order_relation_0.json,jobPath:/opt/data/tis/cfg_repo/tis_plugin_config/ap/ttt/dataxCfg/customer_order_relation_0.json

        CuratorDataXTaskMessage msg = new CuratorDataXTaskMessage();
        msg.setDataXName("ttt");
       // msg.setJobPath("/opt/data/tis/cfg_repo/tis_plugin_config/ap/ttt/dataxCfg/customer_order_relation_0.json");
        msg.setJobName("customer_order_relation_0.json");
        msg.setJobId(866);
        int count = 0;
        while (count++ < 2) {
            System.out.println("turn:" + count);
            consumer.consumeMessage(msg);
            Thread.sleep(3000);
        }


    }
}
