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

package org.apache.griffin.core.job;

import static org.apache.griffin.core.exception.GriffinExceptionMessage.QUARTZ_JOB_ALREADY_EXIST;
import static org.apache.griffin.core.job.JobServiceImpl.GRIFFIN_JOB_ID;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.FINDING;
import static org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType.BATCH;
import static org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType.STREAMING;
import static org.apache.griffin.core.util.JsonUtil.toEntity;
import static org.apache.griffin.core.util.JsonUtil.toJson;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobDataSegment;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.job.entity.SegmentRange;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.JobRepo;
import org.apache.griffin.core.measure.entity.DataConnector;
import org.apache.griffin.core.measure.entity.DataSource;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.GriffinMeasure.ProcessType;
import org.apache.griffin.core.measure.repo.GriffinMeasureRepo;
import org.apache.griffin.core.util.TimeUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class JobInstance implements Job {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(JobInstance.class);
    public static final String MEASURE_KEY = "measure";
    public static final String PREDICATES_KEY = "predicts";
    public static final String PREDICATE_JOB_NAME = "predicateJobName";
    private static final String TRIGGER_KEY = "trigger";
    static final String JOB_NAME = "jobName";
    static final String PATH_CONNECTOR_CHARACTER = ",";
    public static final String INTERVAL = "interval";
    public static final String REPEAT = "repeat";
    public static final String CHECK_DONEFILE_SCHEDULE =
        "checkdonefile.schedule";

    @Autowired
    @Qualifier("schedulerFactoryBean")
    private SchedulerFactoryBean factory;
    @Autowired
    private GriffinMeasureRepo measureRepo;
    @Autowired
    private JobRepo<AbstractJob> jobRepo;
    @Autowired
    private JobInstanceRepo instanceRepo;
    @Autowired
    private Environment env;

    private GriffinMeasure measure;
    private AbstractJob job;
    private List<SegmentPredicate> mPredicates;
    private Long jobStartTime;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        try {
            initParam(context);
            setSourcesPartitionsAndPredicates(measure.getDataSources());
            createJobInstance(job.getConfigMap());
        } catch (Exception e) {
            LOGGER.error("Create predicate job failure.", e);
        }
    }

    private void initParam(JobExecutionContext context)
        throws SchedulerException {
        mPredicates = new ArrayList<>();
        JobDetail jobDetail = context.getJobDetail();
        Long jobId = jobDetail.getJobDataMap().getLong(GRIFFIN_JOB_ID);
        job = jobRepo.findOne(jobId);
        Long measureId = job.getMeasureId();
        measure = measureRepo.findOne(measureId);
        setJobStartTime(jobDetail);
        if (job.getConfigMap() == null) {
            job.setConfigMap(new HashMap<>());
        }
        job.getConfigMap().put(TRIGGER_KEY, context.getTrigger().getKey().toString());
    }

    @SuppressWarnings("unchecked")
    private void setJobStartTime(JobDetail jobDetail)
        throws SchedulerException {
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = jobDetail.getKey();
        List<Trigger> triggers =
            (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
        Date triggerTime = triggers.get(0).getPreviousFireTime();
        jobStartTime = triggerTime.getTime();
    }

    private void setSourcesPartitionsAndPredicates(List<DataSource> sources) {
        boolean isFirstBaseline = true;
        for (JobDataSegment jds : job.getSegments()) {
            if (jds.isAsTsBaseline() && isFirstBaseline) {
                Long tsOffset = TimeUtil.str2Long(
                    jds.getSegmentRange().getBegin());
                measure.setTimestamp(jobStartTime + tsOffset);
                isFirstBaseline = false;
            }
            for (DataSource ds : sources) {
                setDataConnectorPartitions(jds, ds.getConnector());
            }
        }
    }

    private void setDataConnectorPartitions(
        JobDataSegment jds,
        DataConnector dc) {
        String dcName = jds.getDataConnectorName();
        if (dcName.equals(dc.getName())) {
            Long[] sampleTs = genSampleTs(jds.getSegmentRange(), dc);
            setConnectorConf(dc, sampleTs);
            setConnectorPredicates(dc, sampleTs);
        }
    }

    /**
     * split data into several part and get every part start timestamp
     *
     * @param segRange config of data
     * @param dc       data connector
     * @return split timestamps of data
     */
    private Long[] genSampleTs(SegmentRange segRange, DataConnector dc) {
        Long offset = TimeUtil.str2Long(segRange.getBegin());
        Long range = TimeUtil.str2Long(segRange.getLength());
        String unit = dc.getDataUnit();
        Long dataUnit = TimeUtil.str2Long(StringUtils.isEmpty(unit) ? dc
            .getDefaultDataUnit() : unit);
        //offset usually is negative
        Long dataStartTime = jobStartTime + offset;
        if (range < 0) {
            dataStartTime += range;
            range = Math.abs(range);
        }
        if (Math.abs(dataUnit) >= range || dataUnit == 0) {
            return new Long[]{dataStartTime};
        }
        int count = (int) (range / dataUnit);
        Long[] timestamps = new Long[count];
        for (int index = 0; index < count; index++) {
            timestamps[index] = dataStartTime + index * dataUnit;
        }
        return timestamps;
    }

    /**
     * set data connector predicates
     *
     * @param dc       data connector
     * @param sampleTs collection of data split start timestamp
     */
    private void setConnectorPredicates(DataConnector dc, Long[] sampleTs) {
        List<SegmentPredicate> predicates = dc.getPredicates();
        for (SegmentPredicate predicate : predicates) {
            genConfMap(predicate.getConfigMap(),
                sampleTs,
                dc.getDataTimeZone());
            //Do not forget to update origin string config
            predicate.setConfigMap(predicate.getConfigMap());
            mPredicates.add(predicate);
        }
    }

    private void setConnectorConf(DataConnector dc, Long[] sampleTs) {
        genConfMap(dc.getConfigMap(), sampleTs, dc.getDataTimeZone());
        dc.setConfigMap(dc.getConfigMap());
    }

    /**
     * @param conf     config map
     * @param sampleTs collection of data split start timestamp
     * @return all config data combine,like {"where": "year=2017 AND month=11
     * AND dt=15 AND hour=09,year=2017 AND month=11 AND
     * dt=15 AND hour=10"}
     * or like {"path": "/year=2017/month=11/dt=15/hour=09/_DONE
     * ,/year=2017/month=11/dt=15/hour=10/_DONE"}
     */
    private void genConfMap(Map<String, Object> conf, Long[] sampleTs, String
        timezone) {
        if (conf == null) {
            LOGGER.warn("Predicate config is null.");
            return;
        }
        for (Map.Entry<String, Object> entry : conf.entrySet()) {
            // in case entry value is a json object instead of a string
            if (entry.getValue() instanceof String) {
                String value = (String) entry.getValue();
                Set<String> set = new HashSet<>();
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                for (Long timestamp : sampleTs) {
                    set.add(TimeUtil.format(value, timestamp,
                        TimeUtil.getTimeZone(timezone)));
                }
                conf.put(entry.getKey(), StringUtils.join(set,
                    PATH_CONNECTOR_CHARACTER));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createJobInstance(Map<String, Object> confMap)
        throws Exception {
        confMap = checkConfMap(confMap != null ? confMap : new HashMap<>());
        Map<String, Object> config = (Map<String, Object>) confMap
            .get(CHECK_DONEFILE_SCHEDULE);
        Long interval = TimeUtil.str2Long((String) config.get(INTERVAL));
        Integer repeat = Integer.valueOf(config.get(REPEAT).toString());
        String groupName = "PG";
        String jobName = job.getJobName() + "_predicate_"
            + System.currentTimeMillis();
        TriggerKey tk = triggerKey(jobName, groupName);
        if (factory.getScheduler().checkExists(tk)) {
            throw new GriffinException.ConflictException(QUARTZ_JOB_ALREADY_EXIST);
        }
        String triggerKey = (String) confMap.get(TRIGGER_KEY);
        saveJobInstance(jobName, groupName, triggerKey);
        createJobInstance(tk, interval, repeat, jobName);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> checkConfMap(Map<String, Object> confMap) {
        Map<String, Object> config = (Map<String, Object>) confMap.get
            (CHECK_DONEFILE_SCHEDULE);
        String interval = env.getProperty("predicate.job.interval");
        interval = interval != null ? interval : "5m";
        String repeat = env.getProperty("predicate.job.repeat.count");
        repeat = repeat != null ? repeat : "12";
        if (config == null) {
            Map<String, Object> map = new HashMap<>();
            map.put(INTERVAL, interval);
            map.put(REPEAT, repeat);
            confMap.put(CHECK_DONEFILE_SCHEDULE, map);
        } else { // replace if interval or repeat is not null
            String confRepeat = config.get(REPEAT).toString();
            String confInterval = config.get(INTERVAL).toString();
            interval = confInterval != null ? confInterval : interval;
            repeat = confRepeat != null ? confRepeat : repeat;
            config.put(INTERVAL, interval);
            config.put(REPEAT, repeat);
        }
        return confMap;
    }

    private void saveJobInstance(String pName, String pGroup, String triggerKey) {
        ProcessType type = measure.getProcessType() == BATCH ? BATCH :
            STREAMING;
        Long tms = System.currentTimeMillis();
        String expired = env.getProperty("jobInstance.expired.milliseconds");
        Long expireTms = Long.valueOf(expired != null ? expired : "604800000")
            + tms;
        JobInstanceBean instance = new JobInstanceBean(FINDING, pName, pGroup,
            tms, expireTms, type);
        instance.setJob(job);
        instance.setTriggerKey(triggerKey);
        instanceRepo.save(instance);
    }

    private void createJobInstance(TriggerKey tk, Long interval, Integer
        repeatCount, String pJobName) throws Exception {
        JobDetail jobDetail = addJobDetail(tk, pJobName);
        Trigger trigger = genTriggerInstance(tk, jobDetail, interval,
            repeatCount);
        factory.getScheduler().scheduleJob(trigger);
    }

    private Trigger genTriggerInstance(TriggerKey tk, JobDetail jd, Long
        interval, Integer repeatCount) {
        return newTrigger().withIdentity(tk).forJob(jd).startNow()
            .withSchedule(simpleSchedule().withIntervalInMilliseconds
                (interval).withRepeatCount(repeatCount))
            .build();
    }

    private JobDetail addJobDetail(TriggerKey tk, String pJobName)
        throws SchedulerException, IOException {
        Scheduler scheduler = factory.getScheduler();
        JobKey jobKey = jobKey(tk.getName(), tk.getGroup());
        JobDetail jobDetail;
        Boolean isJobKeyExist = scheduler.checkExists(jobKey);
        if (isJobKeyExist) {
            jobDetail = scheduler.getJobDetail(jobKey);
        } else {
            jobDetail = newJob(SparkSubmitJob.class)
                .storeDurably()
                .withIdentity(jobKey)
                .build();
        }
        setJobDataMap(jobDetail, pJobName);
        scheduler.addJob(jobDetail, isJobKeyExist);
        return jobDetail;
    }

    private void setJobDataMap(JobDetail jobDetail, String pJobName)
        throws IOException {
        JobDataMap dataMap = jobDetail.getJobDataMap();
        preProcessMeasure();
        String result = toJson(measure);
        dataMap.put(MEASURE_KEY, result);
        dataMap.put(PREDICATES_KEY, toJson(mPredicates));
        dataMap.put(JOB_NAME, job.getJobName());
        dataMap.put(PREDICATE_JOB_NAME, pJobName);
    }

    private void preProcessMeasure() throws IOException {
        for (DataSource source : measure.getDataSources()) {
            Map cacheMap = source.getCheckpointMap();
            //to skip batch job
            if (cacheMap == null) {
                return;
            }
            String cache = toJson(cacheMap);
            cache = cache.replaceAll("\\$\\{JOB_NAME}", job.getJobName());
            cache = cache.replaceAll("\\$\\{SOURCE_NAME}", source.getName());
            cache = cache.replaceAll("\\$\\{TARGET_NAME}", source.getName());
            cacheMap = toEntity(cache, Map.class);
            source.setCheckpointMap(cacheMap);
        }
    }

}
