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

package org.apache.inlong.common.pojo.agent;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.common.enums.DataReportTypeEnum;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;

import java.util.List;

import static org.apache.inlong.common.enums.DataReportTypeEnum.DIRECT_SEND_TO_MQ;
import static org.apache.inlong.common.enums.DataReportTypeEnum.NORMAL_SEND_TO_DATAPROXY;
import static org.apache.inlong.common.enums.DataReportTypeEnum.PROXY_SEND_TO_DATAPROXY;

/**
 * The task config for agent.
 */
@Data
public class DataConfig {

    private String ip;
    private String uuid;
    private String inlongGroupId;
    private String inlongStreamId;
    private String op;
    private Integer taskId;
    private Integer taskType;
    private String taskName;
    private String snapshot;
    private Integer syncSend;
    private String syncPartitionKey;
    private String extParams;
    /**
     * The task version.
     */
    private Integer version;
    /**
     * The task delivery time, format is 'yyyy-MM-dd HH:mm:ss'.
     */
    private String deliveryTime;
    /**
     * Data report type.
     * The current constraint is that all InLong Agents under one InlongGroup use the same type.
     * <p/>
     * This constraint is not applicable to InlongStream or StreamSource, which avoids the configuration
     * granularity and reduces the operation and maintenance costs.
     * <p/>
     * Supported type:
     * <pre>
     *     0: report to DataProxy and respond when the DataProxy received data.
     *     1: report to DataProxy and respond after DataProxy sends data.
     *     2: report to MQ and respond when the MQ received data.
     * </pre>
     */
    private Integer dataReportType = 0;
    /**
     * MQ cluster information, valid when reportDataTo is 2.
     */
    private List<MQClusterInfo> mqClusters;
    /**
     * MQ's topic information, valid when reportDataTo is 2.
     */
    private DataProxyTopicInfo topicInfo;

    public boolean isValid() {
        DataReportTypeEnum reportType = DataReportTypeEnum.getReportType(dataReportType);
        if (reportType == NORMAL_SEND_TO_DATAPROXY || reportType == PROXY_SEND_TO_DATAPROXY) {
            return true;
        }
        if (reportType == DIRECT_SEND_TO_MQ && CollectionUtils.isNotEmpty(mqClusters) && mqClusters.stream()
                .allMatch(MQClusterInfo::isValid) && topicInfo.isValid()) {
            return true;
        }
        return false;
    }
}