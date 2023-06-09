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

package com.qlangtech.async.message.client.consumer;

import com.qlangtech.tis.async.message.client.consumer.IMQConsumerStatusFactory;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;

import java.util.Set;

/**
 * @author: baisui 百岁
 * @create: 2020-08-29 16:55
 **/
public class RocketMQConsumerStatus implements IMQConsumerStatusFactory.IMQConsumerStatus {

    private final ConsumeStats consumeStats;
    private final DefaultMQAdminExt defaultMQAdminExt;

    public RocketMQConsumerStatus(ConsumeStats consumeStats, DefaultMQAdminExt defaultMQAdminExt) {
        this.consumeStats = consumeStats;
        this.defaultMQAdminExt = defaultMQAdminExt;
    }

//    @Override
//    public long getTotalDiff() {
//        Set<MessageQueue> mqList = consumeStats.getOffsetTable().keySet();
//        long diffTotal = 0L;
//        long diff;
//        OffsetWrapper offsetWrapper = null;
//        for (MessageQueue mq : mqList) {
//            offsetWrapper = consumeStats.getOffsetTable().get(mq);
//            diff = (offsetWrapper.getBrokerOffset() - offsetWrapper.getConsumerOffset());
//            diffTotal += diff;
//        }
//        return diffTotal;
//    }

//    @Override
//    public void close() {
//        this.defaultMQAdminExt.shutdown();
//    }
}
