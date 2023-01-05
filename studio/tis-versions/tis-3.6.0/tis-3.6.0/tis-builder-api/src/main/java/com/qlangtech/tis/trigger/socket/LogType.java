/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.trigger.socket;

/**
 * 网络传输的日志类型
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年5月28日
 */
public enum LogType {

    // 部署状态变化
    INCR_DEPLOY_STATUS_CHANGE(1, "incrdeploy-change"),
    DATAX_WORKER_POD_LOG(7, "datax-worker-pod-log"),
    // 近一段时间内增量监听的各个tag的多少值, 之前是监听rocketMQ的发送情况的
   // MQ_TAGS_STATUS(2, "mq_tags_status"),
    // 全量构建各阶段日志信息
    FULL(3, "full"),
    INCR(4, "incr"),
    INCR_SEND(5, "incrsend"),
    // 监听构建过程中各个阶段的状态
    BuildPhraseMetrics(6, "build_status_metrics");

    // 服务端某些场景下会超时，需要告知客户端，以便重连
    // TIMEOUT(7, "timeout");
    // private static final Logger logger = LoggerFactory.getLogger(LogType.class);
    public static LogType parse(String value) {
        for (LogType type : LogType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalStateException("value:" + value + " is illegal");
    }

    private final String value;

    public final int typeKind;

    // private final MonitorFileCreator monitorFileCreator;
    public String getValue() {
        return this.value;
    }

    private LogType(int typeKind, String value) {
        this.value = value;
        this.typeKind = typeKind;
        // this(value, new MonitorFileCreator());
    }
}
