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

package org.apache.inlong.common.pojo.dataproxy;

import java.util.ArrayList;
import java.util.List;

/**
 * Data proxy config, includes mq clusters and topic list.
 */
public class DataProxyConfig {

    private List<MQClusterInfo> mqClusterList = new ArrayList<>();

    private List<DataProxyTopicInfo> topicList = new ArrayList<>();

    public List<MQClusterInfo> getMqClusterList() {
        return mqClusterList;
    }

    public void setMqClusterList(List<MQClusterInfo> mqClusterList) {
        this.mqClusterList = mqClusterList;
    }

    public List<DataProxyTopicInfo> getTopicList() {
        return topicList;
    }

    public void setTopicList(List<DataProxyTopicInfo> topicList) {
        this.topicList = topicList;
    }

}
