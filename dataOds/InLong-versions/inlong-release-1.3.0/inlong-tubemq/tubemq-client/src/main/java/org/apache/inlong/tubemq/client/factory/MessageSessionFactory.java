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

package org.apache.inlong.tubemq.client.factory;

import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.consumer.ClientBalanceConsumer;
import org.apache.inlong.tubemq.client.consumer.PullMessageConsumer;
import org.apache.inlong.tubemq.client.consumer.PushMessageConsumer;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.client.producer.MessageProducer;
import org.apache.inlong.tubemq.corebase.Shutdownable;

public interface MessageSessionFactory extends Shutdownable {

    @Override
    void shutdown() throws TubeClientException;

    <T extends Shutdownable> void removeClient(T client);

    MessageProducer createProducer() throws TubeClientException;

    PushMessageConsumer createPushConsumer(ConsumerConfig consumerConfig)
            throws TubeClientException;

    PullMessageConsumer createPullConsumer(ConsumerConfig consumerConfig)
            throws TubeClientException;

    ClientBalanceConsumer createBalanceConsumer(ConsumerConfig consumerConfig)
            throws TubeClientException;
}
