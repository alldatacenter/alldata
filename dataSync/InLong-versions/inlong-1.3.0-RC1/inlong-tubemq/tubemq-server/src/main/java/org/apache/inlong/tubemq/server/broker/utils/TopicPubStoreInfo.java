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

package org.apache.inlong.tubemq.server.broker.utils;

import org.apache.inlong.tubemq.corebase.TBaseConstants;

public class TopicPubStoreInfo {

    public String topicName = null;
    public int storeId = TBaseConstants.META_VALUE_UNDEFINED;
    public int partitionId = TBaseConstants.META_VALUE_UNDEFINED;
    public long offsetMin = 0L;
    public long offsetMax = 0L;
    public long dataMin = 0L;
    public long dataMax = 0L;

    public TopicPubStoreInfo(String topicName, int storeId, int partitionId,
                             long offsetMin, long offsetMax, long dataMin, long dataMax) {
        this.topicName = topicName;
        this.storeId = storeId;
        this.partitionId = partitionId;
        this.offsetMin = offsetMin;
        this.offsetMax = offsetMax;
        this.dataMin = dataMin;
        this.dataMax = dataMax;
    }

    public StringBuilder buildPubStoreInfo(StringBuilder sBuilder) {
        sBuilder.append("{\"partitionId\":").append(partitionId)
                .append(",\"offsetMin\":").append(offsetMin)
                .append(",\"offsetMax\":").append(offsetMax)
                .append(",\"dataMin\":").append(dataMin)
                .append(",\"dataMax\":").append(dataMax)
                .append("}");
        return sBuilder;
    }

}
