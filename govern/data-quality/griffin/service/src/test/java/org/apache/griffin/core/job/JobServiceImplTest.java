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

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.JobRepo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinJob;
import static org.apache.griffin.core.util.EntityMocksHelper.createJobInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(SpringRunner.class)
public class JobServiceImplTest {

    @Mock
    private JobRepo<AbstractJob> jobRepo;

    @Mock
    private SchedulerFactoryBean factory;

    @Mock
    private JobInstanceRepo instanceRepo;

    @InjectMocks
    private JobServiceImpl jobService;


    @Test
    public void testTriggerJobById() throws SchedulerException {
        Long jobId = 1L;
        AbstractJob job = createGriffinJob();
        given(jobRepo.findByIdAndDeleted(jobId,false)).willReturn(job);
        Scheduler scheduler = mock(Scheduler.class);
        given(scheduler.checkExists(any(JobKey.class))).willReturn(true);
        ListenerManager listenerManager = mock(ListenerManager.class);
        given(scheduler.getListenerManager()).willReturn(listenerManager);
        given(factory.getScheduler()).willReturn(scheduler);
        JobInstanceBean jobInstanceBean = createJobInstance();
        given(instanceRepo.findByTriggerKey(anyString())).willReturn(Collections.singletonList(jobInstanceBean));

        String result = jobService.triggerJobById(jobId);

        assertTrue(result.matches("DEFAULT\\.[0-9a-f\\-]{49}"));
        verify(scheduler, times(1)).scheduleJob(any());
    }


    @Test(expected = GriffinException.NotFoundException.class)
    public void testTriggerJobByIdFail() throws SchedulerException {
        Long jobId = 1L;
        given(jobRepo.findByIdAndDeleted(jobId,false)).willReturn(null);
        jobService.triggerJobById(jobId);
    }
}
