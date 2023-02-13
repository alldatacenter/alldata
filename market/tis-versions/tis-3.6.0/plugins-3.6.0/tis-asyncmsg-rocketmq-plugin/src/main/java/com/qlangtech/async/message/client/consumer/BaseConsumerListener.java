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

import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import java.util.Properties;

/*
 * 消费监听器基类
 * @since 2016-06-16
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class BaseConsumerListener implements IMQListener<Object> {

    /**
     * 消息处理器
     */
    protected IConsumerHandle consumerHandle;
    /**
     * 属性
     */
    protected Properties consumerProperties;

    /**
     * topic
     */
    protected String topic;

    /**
     * Listener启动
     */
    // abstract void start() throws IOException, MQClientException;
    abstract void close();

    public void setConsumerHandle(IConsumerHandle consumerHandle) {
        this.consumerHandle = consumerHandle;
    }

    public void setConsumerProperties(Properties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return this.topic;
    }

    @Override
    public IConsumerHandle getConsumerHandle() {
        return this.consumerHandle;
    }
}
