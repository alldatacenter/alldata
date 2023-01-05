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

package com.qlangtech.tis.mq;

import com.qlangtech.tis.manage.common.TisUTF8;
import junit.framework.TestCase;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

/**
 * @author: baisui 百岁
 * @create: 2020-11-02 10:13
 **/
public class BasicProducer extends TestCase {
    public static final String nameAddress = "192.168.28.201:9876";
    public static final String topic = "baisui-test";

    protected Message createMsg(String line, String tag) {
        return new Message(topic /* Topic */, tag, line.getBytes(TisUTF8.get()) /* Message body */);
    }

    protected DefaultMQProducer createProducter() throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("produce_baisui_test");
        // https://github.com/apache/rocketmq/issues/568
        producer.setVipChannelEnabled(false);
        producer.setSendMsgTimeout(30000);
        // Specify name server addresses.
        producer.setNamesrvAddr(nameAddress);
        //Launch the instance.
        producer.start();
        return producer;
    }

}
