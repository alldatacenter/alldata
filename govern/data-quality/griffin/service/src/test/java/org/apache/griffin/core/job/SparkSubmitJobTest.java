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

import org.apache.griffin.core.config.PropertiesConfig;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.util.JsonUtil;
import org.apache.griffin.core.util.PropertiesUtil;
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

import static org.apache.griffin.core.util.EntityMocksHelper.*;
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
        JobInstanceBean instance = createJobInstance();
        GriffinMeasure measure = createGriffinMeasure("measureName");
        SegmentPredicate predicate = createFileExistPredicate();
        JobDetail jd = createJobDetail(JsonUtil.toJson(measure), JsonUtil.toJson
                (Collections.singletonList(predicate)));
        given(context.getJobDetail()).willReturn(jd);
        given(context.getTrigger()).willReturn(createSimpleTrigger(4, 5));
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
        JobInstanceBean instance = createJobInstance();
        GriffinMeasure measure = createGriffinMeasure("measureName");
        SegmentPredicate predicate = createFileExistPredicate();
        JobDetail jd = createJobDetail(JsonUtil.toJson(measure), JsonUtil.toJson
                (Collections.singletonList(predicate)));
        given(context.getJobDetail()).willReturn(jd);
        given(context.getTrigger()).willReturn(createSimpleTrigger(4, 4));
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
        JobInstanceBean instance = createJobInstance();
        GriffinMeasure measure = createGriffinMeasure("measureName");
        JobDetail jd = createJobDetail(JsonUtil.toJson(measure), "");
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
        JobInstanceBean instance = createJobInstance();
        GriffinMeasure measure = createGriffinMeasure("measureName");
        JobDetail jd = createJobDetail(JsonUtil.toJson(measure), "");
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
        JobInstanceBean instance = createJobInstance();
        GriffinMeasure measure = createGriffinMeasure("measureName");
        SegmentPredicate predicate = createMockPredicate();
        SegmentPredicate secondPredicate = createMockPredicate();
        JobDetail jd = createJobDetail(JsonUtil.toJson(measure), JsonUtil.toJson
                (Arrays.asList(predicate, secondPredicate)));
        given(context.getJobDetail()).willReturn(jd);
        given(context.getTrigger()).willReturn(createSimpleTrigger(4, 5));
        given(jobInstanceRepo.findByPredicateName(Matchers.anyString()))
                .willReturn(instance);
        sparkSubmitJob.execute(context);

        verify(context, times(1)).getJobDetail();
        verify(jobInstanceRepo, times(1)).findByPredicateName(
                Matchers.anyString());
        verify(jobInstanceRepo, times(1)).save(instance);
    }

}
