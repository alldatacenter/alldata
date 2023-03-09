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

package org.apache.griffin.core.util;

import static org.apache.griffin.core.job.JobInstance.MEASURE_KEY;
import static org.apache.griffin.core.job.JobInstance.PREDICATES_KEY;
import static org.apache.griffin.core.job.JobInstance.PREDICATE_JOB_NAME;
import static org.apache.griffin.core.job.JobServiceImpl.GRIFFIN_JOB_ID;
import static org.apache.hadoop.mapreduce.MRJobConfig.JOB_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.BatchJob;
import org.apache.griffin.core.job.entity.JobDataSegment;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.job.entity.SegmentRange;
import org.apache.griffin.core.job.entity.VirtualJob;
import org.apache.griffin.core.measure.entity.*;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SimpleTrigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

public class EntityMocksHelper {

    public static final String CRON_EXPRESSION = "0 0/4 * * * ?";
    public static final String TIME_ZONE = "GMT+8:00";

    public static GriffinMeasure createGriffinMeasure(String name)
        throws Exception {
        DataConnector dcSource = createDataConnector("source_name", "default",
            "test_data_src", "dt=#YYYYMMdd# AND hour=#HH#");
        DataConnector dcTarget = createDataConnector("target_name", "default",
            "test_data_tgt", "dt=#YYYYMMdd# AND hour=#HH#");
        return createGriffinMeasure(name, dcSource, dcTarget);
    }

    public static GriffinMeasure createGriffinMeasure(
        String name,
        SegmentPredicate srcPredicate,
        SegmentPredicate tgtPredicate)
        throws Exception {
        DataConnector dcSource = createDataConnector("source_name", "default",
            "test_data_src", "dt=#YYYYMMdd# AND hour=#HH#", srcPredicate);
        DataConnector dcTarget = createDataConnector("target_name", "default",
            "test_data_tgt", "dt=#YYYYMMdd# AND hour=#HH#", tgtPredicate);
        return createGriffinMeasure(name, dcSource, dcTarget);
    }

    public static GriffinMeasure createGriffinMeasure(
        String name,
        DataConnector dcSource,
        DataConnector dcTarget)
        throws Exception {
        DataSource dataSource = new DataSource(
            "source", true, createCheckpointMap(), dcSource);
        DataSource targetSource = new DataSource(
            "target", false, createCheckpointMap(), dcTarget);
        List<DataSource> dataSources = new ArrayList<>();
        dataSources.add(dataSource);
        dataSources.add(targetSource);
        Rule rule = createRule();
        EvaluateRule evaluateRule = new EvaluateRule(Arrays.asList(rule));
        return new GriffinMeasure(
            name, "test", dataSources,
            evaluateRule, Arrays.asList("ELASTICSEARCH", "HDFS"));
    }

    private static Rule createRule() throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        map.put("detail", "detail");
        String rule = "source.id=target.id " +
            "AND source.name=target.name AND source.age=target.age";
        Map<String, Object> metricMap = new HashMap<>();
        Map<String, Object> recordMap = new HashMap<>();
        metricMap.put("type", "metric");
        metricMap.put("name", "accu");
        recordMap.put("type", "record");
        recordMap.put("name", "missRecords");
        List<Map<String, Object>> outList = Arrays.asList(metricMap, recordMap);
        return new Rule(
            "griffin-dsl", DqType.ACCURACY, rule,
            "in", "out", map, outList);
    }

    private static Map<String, Object> createCheckpointMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("info.path", "source");
        return map;
    }

    public static DataConnector createDataConnector(
        String name,
        String database,
        String table,
        String where)
        throws IOException {
        HashMap<String, String> config = new HashMap<>();
        config.put("database", database);
        config.put("table.name", table);
        config.put("where", where);
        return new DataConnector(
            name, DataConnector.DataType.HIVE, "1.2",
            JsonUtil.toJson(config), "kafka");
    }

    public static DataConnector createDataConnector(
        String name,
        String database,
        String table,
        String where,
        SegmentPredicate predicate) {
        HashMap<String, String> config = new HashMap<>();
        config.put("database", database);
        config.put("table.name", table);
        config.put("where", where);
        return new DataConnector(name, "1h", config, Arrays.asList(predicate));
    }

    public static ExternalMeasure createExternalMeasure(String name) {
        return new ExternalMeasure(name, "description", "org",
            "test", "metricName", new VirtualJob());
    }

    public static AbstractJob createJob(String jobName) {
        JobDataSegment segment1 = createJobDataSegment("source_name", true);
        JobDataSegment segment2 = createJobDataSegment("target_name", false);
        List<JobDataSegment> segments = new ArrayList<>();
        segments.add(segment1);
        segments.add(segment2);
        return new BatchJob(1L, jobName,
            CRON_EXPRESSION, TIME_ZONE, segments, false);
    }

    public static AbstractJob createJob(String jobName, SegmentRange range) {
        BatchJob job = new BatchJob();
        JobDataSegment segment1 = createJobDataSegment(
            "source_name", true, range);
        JobDataSegment segment2 = createJobDataSegment(
            "target_name", false, range);
        List<JobDataSegment> segments = new ArrayList<>();
        segments.add(segment1);
        segments.add(segment2);
        return new BatchJob(1L, jobName,
            CRON_EXPRESSION, TIME_ZONE, segments, false);
    }

    public static AbstractJob createJob(
        String jobName,
        JobDataSegment source,
        JobDataSegment target) {
        List<JobDataSegment> segments = new ArrayList<>();
        segments.add(source);
        segments.add(target);
        return new BatchJob(1L, jobName,
            CRON_EXPRESSION, TIME_ZONE, segments, false);
    }

    public static JobDataSegment createJobDataSegment(
        String dataConnectorName,
        Boolean baseline,
        SegmentRange range) {
        return new JobDataSegment(dataConnectorName, baseline, range);
    }

    public static JobDataSegment createJobDataSegment(
        String dataConnectorName,
        Boolean baseline) {
        return new JobDataSegment(dataConnectorName, baseline);
    }

    public static JobInstanceBean createJobInstance() {
        JobInstanceBean jobBean = new JobInstanceBean();
        jobBean.setSessionId(1L);
        jobBean.setState(LivySessionStates.State.STARTING);
        jobBean.setAppId("app_id");
        jobBean.setTms(System.currentTimeMillis());
        return jobBean;
    }

    public static JobDetailImpl createJobDetail(
        String measureJson,
        String predicatesJson) {
        JobDetailImpl jobDetail = new JobDetailImpl();
        JobKey jobKey = new JobKey("name", "group");
        jobDetail.setKey(jobKey);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(MEASURE_KEY, measureJson);
        jobDataMap.put(PREDICATES_KEY, predicatesJson);
        jobDataMap.put(JOB_NAME, "jobName");
        jobDataMap.put("jobName", "jobName");
        jobDataMap.put(PREDICATE_JOB_NAME, "predicateJobName");
        jobDataMap.put(GRIFFIN_JOB_ID, 1L);
        jobDetail.setJobDataMap(jobDataMap);
        return jobDetail;
    }

    public static SegmentPredicate createFileExistPredicate()
            throws IOException {
        Map<String, String> config = new HashMap<>();
        config.put("root.path", "hdfs:///griffin/demo_src");
        config.put("path", "/dt=#YYYYMMdd#/hour=#HH#/_DONE");
        SegmentPredicate segmentPredicate = new SegmentPredicate("file.exist", config);
        segmentPredicate.setId(1L);
        segmentPredicate.load();
        return segmentPredicate;
    }

    public static SegmentPredicate createMockPredicate()
            throws IOException {
        Map<String, String> config = new HashMap<>();
        config.put("class", "org.apache.griffin.core.util.PredicatorMock");
        SegmentPredicate segmentPredicate = new SegmentPredicate("custom", config);
        segmentPredicate.setId(1L);
        segmentPredicate.load();
        return segmentPredicate;
    }

    public static Map<String, Object> createJobDetailMap() {
        Map<String, Object> detail = new HashMap<>();
        detail.put("jobId", 1L);
        detail.put("jobName", "jobName");
        detail.put("measureId", 1L);
        detail.put("cronExpression", CRON_EXPRESSION);
        return detail;
    }

    public static SimpleTrigger createSimpleTrigger(
        int repeatCount,
        int triggerCount) {
        SimpleTriggerImpl trigger = new SimpleTriggerImpl();
        trigger.setRepeatCount(repeatCount);
        trigger.setTimesTriggered(triggerCount);
        trigger.setPreviousFireTime(new Date());
        return trigger;
    }

    public static BatchJob createGriffinJob() {
        return new BatchJob(1L, 1L, "jobName",
            "quartzJobName", "quartzGroupName", false);
    }

}
