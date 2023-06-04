/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.manager.service.tube;

import lombok.Data;

@Data
public class BrokerConf {

    private String brokerIp;
    private Integer brokerPort;
    private Integer brokerId;
    private String deleteWhen;
    private Integer numPartitions;
    private Integer unflushThreshold;
    private Integer unflushIntegererval;
    private Integer unflushDataHold;
    private boolean acceptPublish;
    private boolean acceptSubscribe;
    private String createUser;
    private Integer brokerTLSPort;
    private Integer numTopicStores;
    private Integer memCacheMsgCntInK;
    private Integer memCacheMsgSizeInMB;
    private Integer memCacheFlushIntegervl;
    private String deletePolicy;

    /**
     * broker configuration info
     *
     * @param other broker info
     */
    public BrokerConf(BrokerConf other) {
        this.brokerIp = other.brokerIp;
        this.brokerPort = other.brokerPort;
        this.brokerId = other.brokerId;
        this.deleteWhen = other.deleteWhen;
        this.numPartitions = other.numPartitions;
        this.unflushThreshold = other.unflushThreshold;
        this.unflushIntegererval = other.unflushIntegererval;
        this.unflushDataHold = other.unflushDataHold;
        this.acceptPublish = other.acceptPublish;
        this.acceptSubscribe = other.acceptSubscribe;
        this.createUser = other.createUser;
        this.brokerTLSPort = other.brokerTLSPort;
        this.numTopicStores = other.numTopicStores;
        this.memCacheMsgCntInK = other.memCacheMsgCntInK;
        this.memCacheMsgSizeInMB = other.memCacheMsgSizeInMB;
        this.memCacheFlushIntegervl = other.memCacheFlushIntegervl;
        this.deletePolicy = other.deletePolicy;
    }

}
