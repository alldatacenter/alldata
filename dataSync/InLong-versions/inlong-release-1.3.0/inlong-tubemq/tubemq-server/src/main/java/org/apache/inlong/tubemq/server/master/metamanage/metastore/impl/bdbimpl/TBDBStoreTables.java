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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.bdbimpl;

public final class TBDBStoreTables {

    public static final String BDB_CLUSTER_SETTING_STORE_NAME = "bdbClusterSetting";
    public static final String BDB_TOPIC_CONFIG_STORE_NAME = "bdbTopicConfig";
    public static final String BDB_BROKER_CONFIG_STORE_NAME = "bdbBrokerConfig";
    public static final String BDB_CONSUMER_GROUP_STORE_NAME = "bdbConsumerGroup";
    public static final String BDB_TOPIC_AUTH_CONTROL_STORE_NAME = "bdbTopicAuthControl";
    public static final String BDB_BLACK_GROUP_STORE_NAME = "bdbBlackGroup";
    public static final String BDB_GROUP_FILTER_COND_STORE_NAME = "bdbGroupFilterCond";
    public static final String BDB_GROUP_FLOW_CONTROL_STORE_NAME = "bdbGroupFlowCtrlCfg";
    public static final String BDB_CONSUME_GROUP_SETTING_STORE_NAME = "bdbConsumeGroupSetting";
}
