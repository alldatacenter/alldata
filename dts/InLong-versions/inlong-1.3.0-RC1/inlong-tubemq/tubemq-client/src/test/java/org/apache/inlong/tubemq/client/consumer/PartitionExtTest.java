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

package org.apache.inlong.tubemq.client.consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlRuleHandler;
import org.junit.Test;

public class PartitionExtTest {

    @Test
    public void testPartitionExt() {
        FlowCtrlRuleHandler groupFlowCtrlRuleHandler = new FlowCtrlRuleHandler(false);
        FlowCtrlRuleHandler defFlowCtrlRuleHandler = new FlowCtrlRuleHandler(true);
        PartitionExt partition = new PartitionExt(
                groupFlowCtrlRuleHandler,
                defFlowCtrlRuleHandler,
                new BrokerInfo(1, "127.0.0.1", 18080),
                "test",
                1);
        partition.setLastPackConsumed(true);
        assertTrue(partition.isLastPackConsumed());
        partition.setLastPackConsumed(false);
        assertFalse(partition.isLastPackConsumed());
        partition.getAndResetLastPackConsumed();
        assertFalse(partition.isLastPackConsumed());
    }

    @Test
    public void testPartitionExtSuccess() {
        FlowCtrlRuleHandler groupFlowCtrlRuleHandler = new FlowCtrlRuleHandler(false);
        FlowCtrlRuleHandler defFlowCtrlRuleHandler = new FlowCtrlRuleHandler(true);
        PartitionExt partition = new PartitionExt(
                groupFlowCtrlRuleHandler,
                defFlowCtrlRuleHandler,
                new BrokerInfo(1, "127.0.0.1", 18080),
                "test",
                1);
        int limitDlt = 4096;
        partition.setPullTempData(0, 200, false, 1024, limitDlt, 10, false);
        //assertEquals(limitDlt, partition.procConsumeResult(false));
        partition.setPullTempData(0, 200, true, 1024, limitDlt, 10, false);
        //assertEquals(0, partition.procConsumeResult(false));

    }

    @Test
    public void testPartitionExtError() {
        FlowCtrlRuleHandler groupFlowCtrlRuleHandler = new FlowCtrlRuleHandler(false);
        FlowCtrlRuleHandler defFlowCtrlRuleHandler = new FlowCtrlRuleHandler(true);
        PartitionExt partition = new PartitionExt(
                groupFlowCtrlRuleHandler,
                defFlowCtrlRuleHandler,
                new BrokerInfo(1, "127.0.0.1", 18080),
                "test",
                1);
        int limitDlt = 4096;
        partition.setPullTempData(0, 0, false, 1024, 4096, 10, false);
        //assertEquals(limitDlt, partition.procConsumeResult(false));
        partition.setPullTempData(0, 404, false, 0, 4096, 10, false);
        //assertEquals(limitDlt, partition.procConsumeResult(false));
    }
}
