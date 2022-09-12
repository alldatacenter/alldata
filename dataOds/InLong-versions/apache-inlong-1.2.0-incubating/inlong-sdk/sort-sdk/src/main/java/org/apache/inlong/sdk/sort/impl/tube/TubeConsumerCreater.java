/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sdk.sort.impl.tube;

import org.apache.inlong.tubemq.client.config.TubeClientConfig;
import org.apache.inlong.tubemq.client.factory.MessageSessionFactory;

public class TubeConsumerCreater {

    private final MessageSessionFactory messageSessionFactory;
    private final TubeClientConfig consumerConfig;

    /**
     * TubeConsumerCreater constructor
     *
     * @param messageSessionFactory {@link MessageSessionFactory}
     * @param consumerConfig {@link TubeClientConfig}
     */
    public TubeConsumerCreater(MessageSessionFactory messageSessionFactory,
            TubeClientConfig consumerConfig) {
        this.messageSessionFactory = messageSessionFactory;
        this.consumerConfig = consumerConfig;
    }

    /**
     * get MessageSessionFactory
     *
     * @return {@link MessageSessionFactory}
     */
    public MessageSessionFactory getMessageSessionFactory() {
        return messageSessionFactory;
    }

    /**
     * get TubeClientConfig
     *
     * @return {@link TubeClientConfig}
     */
    public TubeClientConfig getTubeClientConfig() {
        return consumerConfig;
    }
}
