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

package org.apache.inlong.tubemq.client.config;

import org.apache.inlong.tubemq.corerpc.RpcConfig;
import org.apache.inlong.tubemq.corerpc.RpcConstants;

public class TubeClientConfigUtils {

    /**
     * Get RPC configure by client configure information
     *
     * @param tubeClientConfig   the client configure
     * @param isSingleSession    whether single session factory
     */
    public static RpcConfig getRpcConfigByClientConfig(final TubeClientConfig tubeClientConfig,
                                                       boolean isSingleSession) {
        RpcConfig config = new RpcConfig();
        config.put(RpcConstants.TLS_OVER_TCP, tubeClientConfig.isTlsEnable());
        if (tubeClientConfig.isTlsEnable()) {
            config.put(RpcConstants.TLS_TRUSTSTORE_PATH,
                    tubeClientConfig.getTrustStorePath());
            config.put(RpcConstants.TLS_TRUSTSTORE_PASSWORD,
                    tubeClientConfig.getTrustStorePassword());
            config.put(RpcConstants.TLS_TWO_WAY_AUTHENTIC,
                    tubeClientConfig.isEnableTLSTwoWayAuthentic());
            if (tubeClientConfig.isEnableTLSTwoWayAuthentic()) {
                config.put(RpcConstants.TLS_KEYSTORE_PATH,
                        tubeClientConfig.getKeyStorePath());
                config.put(RpcConstants.TLS_KEYSTORE_PASSWORD,
                        tubeClientConfig.getKeyStorePassword());
            }
        }
        config.put(RpcConstants.CONNECT_TIMEOUT, 3000);
        config.put(RpcConstants.REQUEST_TIMEOUT, tubeClientConfig.getRpcTimeoutMs());
        config.put(RpcConstants.NETTY_WRITE_HIGH_MARK,
                tubeClientConfig.getNettyWriteBufferHighWaterMark());
        config.put(RpcConstants.NETTY_WRITE_LOW_MARK,
                tubeClientConfig.getNettyWriteBufferLowWaterMark());
        config.put(RpcConstants.WORKER_COUNT, tubeClientConfig.getRpcConnProcessorCnt());
        if (isSingleSession) {
            config.put(RpcConstants.WORKER_THREAD_NAME, "tube_single_netty_worker-");
        } else {
            config.put(RpcConstants.WORKER_THREAD_NAME, "tube_multi_netty_worker-");
        }
        config.put(RpcConstants.CALLBACK_WORKER_COUNT,
                tubeClientConfig.getRpcRspCallBackThreadCnt());
        return config;
    }
}
