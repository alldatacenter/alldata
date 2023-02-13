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

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.async.message.client.consumer.IMQConsumerStatusFactory;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.plugin.IPluginStore;
import com.qlangtech.tis.plugin.PluginStore;
import junit.framework.TestCase;

/**
 * @author: baisui 百岁
 * @create: 2020-08-29 17:38
 **/
public class TestRocketMQListenerFactory extends TestCase {
    private static final String collection = "search4totalpay";

    public void testCreateConsumerStatus() {

        IPluginStore<MQListenerFactory> mqListenerFactory = TIS.getPluginStore(collection, MQListenerFactory.class);
        assertNotNull(mqListenerFactory);

        MQListenerFactory plugin = mqListenerFactory.getPlugin();
        assertNotNull(plugin);

        IMQConsumerStatusFactory.IMQConsumerStatus consumerStatus = plugin.createConsumerStatus();
        assertNotNull(consumerStatus);


      //  assertTrue(consumerStatus.getTotalDiff() > 0);

    }
}
