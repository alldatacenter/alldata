/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metric;


import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_METRIC_RECORDS_OFFSET;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_METRIC_RECORDS_SIZE;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_METRIC_VALUE_FORMAT;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_INSTANCE_NOT_FOUND;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.JobRepo;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.apache.griffin.core.metric.model.Metric;
import org.apache.griffin.core.metric.model.MetricValue;
import org.codehaus.jackson.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MetricServiceImpl implements MetricService {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(MetricServiceImpl.class);

    @Autowired
    private MeasureRepo<Measure> measureRepo;
    @Autowired
    private JobRepo<AbstractJob> jobRepo;
    @Autowired
    private MetricStore metricStore;
    @Autowired
    private JobInstanceRepo jobInstanceRepo;

    @Override
    public Map<String, List<Metric>> getAllMetrics() {
        Map<String, List<Metric>> metricMap = new HashMap<>();
        List<AbstractJob> jobs = jobRepo.findByDeleted(false);
        List<Measure> measures = measureRepo.findByDeleted(false);
        Map<Long, Measure> measureMap = measures.stream().collect(Collectors
            .toMap(Measure::getId, Function.identity()));
        Map<Long, List<AbstractJob>> jobMap = jobs.stream().collect(Collectors
            .groupingBy(AbstractJob::getMeasureId, Collectors.toList()));
        for (Map.Entry<Long, List<AbstractJob>> entry : jobMap.entrySet()) {
            Long measureId = entry.getKey();
            Measure measure = measureMap.get(measureId);
            List<AbstractJob> jobList = entry.getValue();
            List<Metric> metrics = new ArrayList<>();
            for (AbstractJob job : jobList) {
                List<MetricValue> metricValues = getMetricValues(job
                    .getMetricName(), 0, 300, job.getCreatedDate());
                metrics.add(new Metric(job.getMetricName(), measure.getDqType(),
                    measure.getOwner(), metricValues));
            }
            metricMap.put(measure.getName(), metrics);

        }
        return metricMap;
    }

    @Override
    public List<MetricValue> getMetricValues(String metricName, int offset,
                                             int size, long tmst) {
        if (offset < 0) {
            throw new GriffinException.BadRequestException
                (INVALID_METRIC_RECORDS_OFFSET);
        }
        if (size < 0) {
            throw new GriffinException.BadRequestException
                (INVALID_METRIC_RECORDS_SIZE);
        }
        try {
            return metricStore.getMetricValues(metricName, offset, size, tmst);
        } catch (IOException e) {
            LOGGER.error("Failed to get metric values named {}. {}",
                metricName, e.getMessage());
            throw new GriffinException.ServiceException(
                "Failed to get metric values", e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ResponseEntity addMetricValues(List<MetricValue> values) {
        for (MetricValue value : values) {
            checkFormat(value);
        }
        try {
            return metricStore.addMetricValues(values);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Failed to parse metric value.", e.getMessage());
            throw new GriffinException.BadRequestException
                (INVALID_METRIC_VALUE_FORMAT);
        } catch (IOException e) {
            LOGGER.error("Failed to add metric values", e);
            throw new GriffinException.ServiceException(
                "Failed to add metric values", e);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ResponseEntity deleteMetricValues(String metricName) {
        try {
            return metricStore.deleteMetricValues(metricName);
        } catch (IOException e) {
            LOGGER.error("Failed to delete metric values named {}. {}",
                metricName, e.getMessage());
            throw new GriffinException.ServiceException(
                "Failed to delete metric values.", e);
        }
    }

    @Override
    public MetricValue findMetric(Long id) {
        JobInstanceBean jobInstanceBean = jobInstanceRepo.findByInstanceId(id);
        if (jobInstanceBean == null) {
            LOGGER.warn("There are no job instances with id {} ", id);
            throw new GriffinException
                .NotFoundException(JOB_INSTANCE_NOT_FOUND);
        }
        String appId = jobInstanceBean.getAppId();
        try {
            return metricStore.getMetric(appId);
        } catch (IOException e) {
            LOGGER.warn("Failed to get metric for applicationId {} ", appId);
            throw new GriffinException.ServiceException("Failed to find metric", e);
        }
    }

    private void checkFormat(MetricValue value) {
        if (StringUtils.isBlank(value.getName()) || value.getTmst() == null
            || MapUtils.isEmpty(value.getValue())) {
            throw new GriffinException.BadRequestException
                (INVALID_METRIC_VALUE_FORMAT);
        }
    }
}
