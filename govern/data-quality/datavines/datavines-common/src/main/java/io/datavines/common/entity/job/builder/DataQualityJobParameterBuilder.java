/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.entity.job.builder;

import io.datavines.common.entity.ConnectionInfo;
import io.datavines.common.entity.ConnectorParameter;
import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.common.utils.JSONUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

public class DataQualityJobParameterBuilder implements ParameterBuilder {

    @Override
    public String buildJobExecutionParameter(String jobParameter, ConnectionInfo srcConnectionInfo, ConnectionInfo targetConnectionInfo) {
        List<BaseJobParameter> jobParameters = JSONUtils.toList(jobParameter, BaseJobParameter.class);

        if (CollectionUtils.isNotEmpty(jobParameters)) {
            JobExecutionParameter jobExecutionParameter = new JobExecutionParameter();
            String database = "";
            for (BaseJobParameter metricJobParameter : jobParameters) {
                Map<String,Object> metricParameters = metricJobParameter.getMetricParameter();
                database = (String)metricParameters.get("database");
                metricParameters.remove("database");
                metricParameters.put("metric_database",database);
                metricJobParameter.setMetricParameter(metricParameters);
            }

            jobExecutionParameter.setMetricParameterList(jobParameters);

            ConnectorParameter srcConnectorParameter = new ConnectorParameter();
            srcConnectorParameter.setType(srcConnectionInfo.getType());
            Map<String,Object> srcConnectorParameterMap = srcConnectionInfo.configMap();
            srcConnectorParameterMap.put("database", database);
            srcConnectorParameter.setParameters(srcConnectorParameterMap);
            jobExecutionParameter.setConnectorParameter(srcConnectorParameter);

            return JSONUtils.toJsonString(jobExecutionParameter);
        }

        return null;
    }
}
