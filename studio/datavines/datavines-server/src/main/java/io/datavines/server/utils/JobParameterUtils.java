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
package io.datavines.server.utils;

import io.datavines.common.entity.job.BaseJobParameter;
import io.datavines.metric.api.SqlMetric;
import io.datavines.spi.PluginLoader;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobParameterUtils {

    public static List<BaseJobParameter> regenerateJobParameterList(List<BaseJobParameter> jobParameters) {
        List<BaseJobParameter> result = new ArrayList<>();
        for (BaseJobParameter jobParameter : jobParameters) {
            SqlMetric metric = PluginLoader.getPluginLoader(SqlMetric.class).getOrCreatePlugin(jobParameter.getMetricType());
            if (metric == null) {
                continue;
            }

            if (metric.supportMultiple()) {
                List<Map<String, Object>> metricParameterList = metric.getMetricParameter(jobParameter.getMetricParameter());
                if (CollectionUtils.isNotEmpty(metricParameterList)) {
                    for (Map<String, Object> metricParameter : metricParameterList) {
                        BaseJobParameter newJobParameter = new BaseJobParameter();
                        BeanUtils.copyProperties(jobParameter, newJobParameter);
                        newJobParameter.setMetricParameter(metricParameter);
                        result.add(newJobParameter);
                    }
                }

            } else {
                result.add(jobParameter);
            }
        }

        return result;
    }
}
