/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.async.message.client.kafka;

import com.qlangtech.tis.async.message.client.consumer.AsyncMsg;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.datax.IDataxProcessor;
import junit.framework.TestCase;

/**
 * @author: baisui 百岁
 * @create: 2020-12-09 15:01
 **/
public class TestKafkaMQListener extends TestCase {
    public void testRegisterTopic() throws Exception {

        TiKVKafkaMQListenerFactory listenerFactory = new TiKVKafkaMQListenerFactory();
        listenerFactory.topic = "baisui";
        listenerFactory.groupId = "test1";
        listenerFactory.mqAddress = "192.168.28.201:9092";
        listenerFactory.offsetResetStrategy = "latest";


        IMQListener imqListener = listenerFactory.create();
        //imqListener.setConsumerHandle(new MockConsumer());
        // BasicDataSourceFactory dataSource, List<ISelectedTab > tabs, ISink sink
        //imqListener.start();

        Thread.sleep(99999999);

    }

    private static class MockConsumer implements IConsumerHandle {


        @Override
        public Object consume(TargetResName dataxName, AsyncMsg asyncMsg, IDataxProcessor dataXProcessor) throws Exception {
            return null;
        }
    }
}
