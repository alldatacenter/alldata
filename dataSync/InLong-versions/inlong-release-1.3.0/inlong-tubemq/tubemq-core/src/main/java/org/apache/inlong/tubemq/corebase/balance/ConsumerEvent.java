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

package org.apache.inlong.tubemq.corebase.balance;

import java.util.ArrayList;
import java.util.List;
import org.apache.inlong.tubemq.corebase.cluster.SubscribeInfo;

public class ConsumerEvent {

    private long rebalanceId;
    private EventType type;
    private EventStatus status;
    private List<SubscribeInfo> subscribeInfoList =
            new ArrayList<>();

    public ConsumerEvent(long rebalanceId,
                         EventType type,
                         List<SubscribeInfo> subscribeInfoList,
                         EventStatus status) {
        this.rebalanceId = rebalanceId;
        this.type = type;
        if (subscribeInfoList != null) {
            this.subscribeInfoList = subscribeInfoList;
        }
        this.status = status;
    }

    public long getRebalanceId() {
        return rebalanceId;
    }

    public void setRebalanceId(long rebalanceId) {
        this.rebalanceId = rebalanceId;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public List<SubscribeInfo> getSubscribeInfoList() {
        return subscribeInfoList;
    }

    public void setSubscribeInfoList(List<SubscribeInfo> subscribeInfoList) {
        if (subscribeInfoList != null) {
            this.subscribeInfoList = subscribeInfoList;
        }
    }

    public StringBuilder toStrBuilder(final StringBuilder sBuilder) {
        return sBuilder.append("ConsumerEvent [rebalanceId=").append(rebalanceId)
                .append(", type=").append(type).append(", status=").append(status)
                .append(", subscribeInfoList=").append(subscribeInfoList).append("]");
    }
}
