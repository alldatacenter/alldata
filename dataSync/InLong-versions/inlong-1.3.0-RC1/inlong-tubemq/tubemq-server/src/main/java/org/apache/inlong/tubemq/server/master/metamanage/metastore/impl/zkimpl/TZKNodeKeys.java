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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.zkimpl;

public class TZKNodeKeys {
    public static final String ZK_BRANCH_META_DATA = "metaData";
    public static final String ZK_BRANCH_MASTER_HA = "masterHA";
    public static final String ZK_LEAF_MASTER_HA_NODEID = "nodeId";
    public static final String ZK_LEAF_CLUSTER_CONFIG = "clusterConfig";
    public static final String ZK_LEAF_BROKER_CONFIG = "brokerConfig";
    public static final String ZK_LEAF_CONSUME_CTRL_CONFIG = "consumeCtrlConfig";
    public static final String ZK_LEAF_GROUP_CTRL_CONFIG = "groupCtrlConfig";
    public static final String ZK_LEAF_TOPIC_CTRL_CONFIG = "topicCtrlConfig";
    public static final String ZK_LEAF_TOPIC_DEPLOY_CONFIG = "topicDeployConfig";

}
