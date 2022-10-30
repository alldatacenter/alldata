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

package org.apache.inlong.tubemq.server.broker.metadata;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.utils.MixedUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;

public class ClusterConfigHolder {
    private static AtomicLong configId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private static AtomicInteger maxMsgSize =
            new AtomicInteger(TBaseConstants.META_MAX_MESSAGE_DATA_SIZE
                    + TBaseConstants.META_MAX_MESSAGE_HEADER_SIZE);
    private static AtomicInteger minMemCacheSize =
            new AtomicInteger(TBaseConstants.META_MIN_MEM_BUFFER_SIZE);

    public ClusterConfigHolder() {

    }

    // set master returned configure
    public static void updClusterSetting(ClientMaster.ClusterConfig clusterConfig) {
        if (clusterConfig == null) {
            return;
        }
        if (configId.get() != clusterConfig.getConfigId()) {
            configId.set(clusterConfig.getConfigId());
            if (clusterConfig.hasMaxMsgSize()) {
                Tuple2<Integer, Integer> calcResult =
                        calcMaxMsgSize(clusterConfig.getMaxMsgSize());
                if (calcResult.getF0() != maxMsgSize.get()) {
                    maxMsgSize.set(calcResult.getF0());
                    minMemCacheSize.set(calcResult.getF1());
                }
            }
        }
    }

    public static long getConfigId() {
        return configId.get();
    }

    public static int getMaxMsgSize() {
        return maxMsgSize.get();
    }

    public static int getMinMemCacheSize() {
        return minMemCacheSize.get();
    }

    public static Tuple2<Integer, Integer> calcMaxMsgSize(int maxMsgSize) {
        int tmpMaxSize = MixedUtils.mid(maxMsgSize,
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE,
                TBaseConstants.META_MAX_MESSAGE_DATA_SIZE_UPPER_LIMIT)
                + TBaseConstants.META_MAX_MESSAGE_HEADER_SIZE;
        int tmpMinMemCacheSize =
                tmpMaxSize + (tmpMaxSize % 4 + 1) * TBaseConstants.META_MESSAGE_SIZE_ADJUST;
        return new Tuple2<>(tmpMaxSize, tmpMinMemCacheSize);
    }

}
