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

package org.apache.inlong.sdk.dataproxy.example;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.sdk.dataproxy.DefaultMessageSender;
import org.apache.inlong.sdk.dataproxy.ProxyClientConfig;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

public class TcpClientExample {

    private static final Logger logger = LoggerFactory.getLogger(TcpClientExample.class);

    public static String localIP = "127.0.0.1";

    /**
     * Example of client tcp.
     */
    public static void main(String[] args) throws InterruptedException {

        String dataProxyGroup = "test_test";
        String inlongGroupId = "test_test";
        String inlongStreamId = "test_test";
        String netTag = "";

        /*
         * 1. if isLocalVisit is true, will get dataproxy server info from local file in
         * ${configBasePath}/${dataProxyGroup}.local file
         *
         * for example: /data/inlong/config/test_test.local and file context like this:
         * {"isInterVisit":1,"clusterId":"1","size":1,"switch":1,"address":[{"host":"127.0.0.1",
         * "port":"46802"},{"host":"127.0.0.1","port":"46802"}]} 2. if isLocalVisit is false, will get dataproxy server
         * info from manager so we must ensure that the manager server url is configured correctly!
         */
        String configBasePath = "/data/inlong/config";

        String inLongManagerAddr = "127.0.0.1";
        String inLongManagerPort = "8000";

        /*
         * It is recommended to use type 7. For others, please refer to the official related documents
         */
        int msgType = 7;
        String messageBody = "inglong-message-random-body!";

        TcpClientExample tcpClientExample = new TcpClientExample();
        DefaultMessageSender sender = tcpClientExample
                .getMessageSender(localIP, inLongManagerAddr, inLongManagerPort, netTag,
                        dataProxyGroup, false, false, configBasePath, msgType);
        tcpClientExample.sendTcpMessage(sender, inlongGroupId, inlongStreamId,
                messageBody, System.currentTimeMillis());
    }

    public DefaultMessageSender getMessageSender(String localIP, String inLongManagerAddr, String inLongManagerPort,
            String netTag, String dataProxyGroup, boolean isLocalVisit, boolean isReadProxyIPFromLocal,
            String configBasePath, int msgType) {
        ProxyClientConfig dataProxyConfig = null;
        DefaultMessageSender messageSender = null;
        try {
            dataProxyConfig = new ProxyClientConfig(localIP, isLocalVisit, inLongManagerAddr,
                    Integer.valueOf(inLongManagerPort), dataProxyGroup, netTag, "test", "123456");
            if (StringUtils.isNotEmpty(configBasePath)) {
                dataProxyConfig.setConfStoreBasePath(configBasePath);
            }
            dataProxyConfig.setReadProxyIPFromLocal(isReadProxyIPFromLocal);
            messageSender = DefaultMessageSender.generateSenderByClusterId(dataProxyConfig);
            messageSender.setMsgtype(msgType);
        } catch (Exception e) {
            logger.error("getMessageSender has exception e = {}", e);
        }
        return messageSender;
    }

    public void sendTcpMessage(DefaultMessageSender sender, String inlongGroupId,
            String inlongStreamId, String messageBody, long dt) {
        SendResult result = null;
        try {
            result = sender.sendMessage(messageBody.getBytes("utf8"), inlongGroupId, inlongStreamId,
                    0, String.valueOf(dt), 20, TimeUnit.SECONDS);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        logger.info("messageSender {}", result);
    }

}
