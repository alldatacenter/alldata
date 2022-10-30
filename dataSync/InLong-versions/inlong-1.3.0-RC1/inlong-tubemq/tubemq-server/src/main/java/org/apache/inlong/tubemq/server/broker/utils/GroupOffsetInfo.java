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
import org.apache.inlong.tubemq.corebase.utils.Tuple2;

public class GroupOffsetInfo {
    public int partitionId = TBaseConstants.META_VALUE_UNDEFINED;
    public long offsetMin = TBaseConstants.META_VALUE_UNDEFINED;
    public long offsetMax = TBaseConstants.META_VALUE_UNDEFINED;
    public long dataMin = TBaseConstants.META_VALUE_UNDEFINED;
    public long dataMax = TBaseConstants.META_VALUE_UNDEFINED;
    public long curOffset = TBaseConstants.META_VALUE_UNDEFINED;
    public long flightOffset = TBaseConstants.META_VALUE_UNDEFINED;
    public long offsetLag = TBaseConstants.META_VALUE_UNDEFINED;
    public long curDataOffset = TBaseConstants.META_VALUE_UNDEFINED;
    public long dataLag = TBaseConstants.META_VALUE_UNDEFINED;

    public GroupOffsetInfo(int partitionId) {
        this.partitionId = partitionId;
    }

    public void setPartPubStoreInfo(TopicPubStoreInfo pubStoreInfo) {
        if (pubStoreInfo != null) {
            this.offsetMin = pubStoreInfo.offsetMin;
            this.offsetMax = pubStoreInfo.offsetMax;
            this.dataMin = pubStoreInfo.dataMin;
            this.dataMax = pubStoreInfo.dataMax;
        }
    }

    public void setConsumeOffsetInfo(Tuple2<Long, Long> offsetInfo) {
        if (offsetInfo != null) {
            this.curOffset = offsetInfo.getF0();
            this.flightOffset = offsetInfo.getF1();
        }
    }

    public void setConsumeDataOffsetInfo(long curDataOffset) {
        if (curDataOffset >= 0) {
            this.curDataOffset = curDataOffset;
        }
    }

    public void calculateLag() {
        if (offsetMax != TBaseConstants.META_VALUE_UNDEFINED
                && curOffset != TBaseConstants.META_VALUE_UNDEFINED) {
            offsetLag = offsetMax - curOffset;
        }
        if (dataMax != TBaseConstants.META_VALUE_UNDEFINED
                && curDataOffset != TBaseConstants.META_VALUE_UNDEFINED) {
            dataLag = dataMax - curDataOffset;
        }
    }

    public StringBuilder buildOffsetInfo(StringBuilder sBuilder) {
        sBuilder.append("{\"partitionId\":").append(partitionId)
                .append(",\"curOffset\":").append(curOffset)
                .append(",\"flightOffset\":").append(flightOffset)
                .append(",\"curDataOffset\":").append(curDataOffset)
                .append(",\"offsetLag\":").append(offsetLag)
                .append(",\"dataLag\":").append(dataLag)
                .append(",\"offsetMax\":").append(offsetMax)
                .append(",\"dataMax\":").append(dataMax)
                .append("}");
        return sBuilder;
    }
}
