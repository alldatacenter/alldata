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

package com.platform.quality.job;

import com.platform.quality.util.EntityMocksHelper;
import com.platform.quality.config.PropertiesConfig;
import com.platform.quality.job.entity.JobInstanceBean;
import com.platform.quality.job.entity.SegmentPredicate;
import com.platform.quality.job.repo.JobInstanceRepo;
import com.platform.quality.measure.entity.GriffinMeasure;
import com.platform.quality.util.JsonUtil;
import com.platform.quality.util.PropertiesUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;



@RunWith(SpringRunner.class)
public class SparkSubmitJobTest {

    @TestConfiguration
    public static class SchedulerServiceConfiguration {
        @Bean
        public SparkSubmitJob sparkSubmitJobBean() {
            return new SparkSubmitJob();
        }

        @Bean(name = "livyConf")
        public Properties sparkJobProps() {
            String path = "sparkJob.properties";
            return PropertiesUtil.getProperties(path,
                    new ClassPathResource(path));
        }
        @Bean
        public PropertiesConfig sparkConf() {
            return new PropertiesConfig("src/test/resources", null);
        }
    }

    @Autowired
    private SparkSubmitJob sparkSubmitJob;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private JobInstanceRepo jobInstanceRepo;

    @MockBean
    private JobServiceImpl jobService;

    @MockBean
    private BatchJobOperatorImpl batchJobOp;

    @MockBean
    private LivyTaskSubmitHelper livyTaskSubmitHelper;

    @Before
    public void setUp() {
    }

    @Test
    public void testExecuteWithPredicateTriggerGreaterThanRepeat()
            throws Exception {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobInstanceBean instance = EntityMocksHelper.createJobInstance();
        GriffinMeasure measure = EntityMocksHelper.createGriffinMeasure("measureName");
        SegmentPredicate predicate = EntityMocksHelper.createFileExistPredicate();
        JobDetail jd = EntityMocksHelper.createJobDetail(JsonUtil.toJson(measure), JsonUtil.toJson
                (Collections.singletonList(predicate)));
        given(context.getJobDetail()).willReturn(jd);
        given(context.getTrigger()).willReturn(EntityMocksHelper.createSimpleTrigger(4, 5));
        given(jobInstanceRepo.findByPredicateName(Matchers.anyString()))
                .willReturn(instance);

        sparkSubmitJob.execute(context);

        verify(context, times(1)).getJobDetail();
        verify(jobInstanceRepo, times(1)).findByPredicateName(
                Matchers.anyString());
    }

    @Test
    public void testExecuteWithPredicateTriggerLessThanRepeat() throws Exception {

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobInstanceBean instance = EntityMocksHelper.createJobInstance();
        GriffinMeasure measure = EntityMocksHelper.createGriffinMeasure("measureName");
        SegmentPredicate predicate = EntityMocksHelper.createFileExistPredicate();
        JobDetail jd = EntityMocksHelper.createJobDetail(JsonUtil.toJson(measure), JsonUtil.toJson
                (Collections.singletonList(predicate)));
        given(context.getJobDetail()).willReturn(jd);
        given(context.getTrigger()).willReturn(EntityMocksHelper.createSimpleTrigger(4, 4));
        given(jobInstanceRepo.findByPredicateName(Matchers.anyString()))
                .willReturn(instance);

        sparkSubmitJob.execute(context);

        verify(context, times(1)).getJobDetail();
        verify(jobInstanceRepo, times(1)).findByPredicateName(
                Matchers.anyString());
    }

    @Test
    public void testExecuteWithNoPredicateSuccess() throws Exception {

        String result = "{\"id\":1,\"state\":\"starting\",\"appId\":null," +
                "\"appInfo\":{\"driverLogUrl\":null," +
                "\"sparkUiUrl\":null},\"log\":[]}";
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobInstanceBean instance = EntityMocksHelper.createJobInstance();
        GriffinMeasure measure = EntityMocksHelper.createGriffinMeasure("measureName");
        JobDetail jd = EntityMocksHelper.createJobDetail(JsonUtil.toJson(measure), "");
        given(context.getJobDetail()).willReturn(jd);
        given(jobInstanceRepo.findByPredicateName(Matchers.anyString()))
                .willReturn(instance);

        sparkSubmitJob.execute(context);

        verify(context, times(1)).getJobDetail();
        verify(jobInstanceRepo, times(1)).findByPredicateName(
                Matchers.anyString());
    }

    @Test
    public void testExecuteWithPost2LivyException() throws Exception {

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobInstanceBean instance = EntityMocksHelper.createJobInstance();
        GriffinMeasure measure = EntityMocksHelper.createGriffinMeasure("measureName");
        JobDetail jd = EntityMocksHelper.createJobDetail(JsonUtil.toJson(measure), "");
        given(context.getJobDetail()).willReturn(jd);
        given(jobInstanceRepo.findByPredicateName(Matchers.anyString()))
                .willReturn(instance);

        sparkSubmitJob.execute(context);
        verify(context, times(1)).getJobDetail();
        verify(jobInstanceRepo, times(1)).findByPredicateName(
                Matchers.anyString());
    }

    @Test
    public void testExecuteWithNullException() {
        JobExecutionContext context = mock(JobExecutionContext.class);

        sparkSubmitJob.execute(context);
    }

    @Test
    public void testMultiplePredicatesWhichReturnsTrue() throws Exception {
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobInstanceBean instance = EntityMocksHelper.createJobInstance();
        GriffinMeasure measure = EntityMocksHelper.createGriffinMeasure("measureName");
        SegmentPredicate predicate = EntityMocksHelper.createMockPredicate();
        SegmentPredicate secondPredicate = EntityMocksHelper.createMockPredicate();
        JobDetail jd = EntityMocksHelper.createJobDetail(JsonUtil.toJson(measure), JsonUtil.toJson
                (Arrays.asList(predicate, secondPredicate)));
        given(context.getJobDetail()).willReturn(jd);
        given(context.getTrigger()).willReturn(EntityMocksHelper.createSimpleTrigger(4, 5));
        given(jobInstanceRepo.findByPredicateName(Matchers.anyString()))
                .willReturn(instance);
        sparkSubmitJob.execute(context);

        verify(context, times(1)).getJobDetail();
        verify(jobInstanceRepo, times(1)).findByPredicateName(
                Matchers.anyString());
        verify(jobInstanceRepo, times(1)).save(instance);
    }

}
