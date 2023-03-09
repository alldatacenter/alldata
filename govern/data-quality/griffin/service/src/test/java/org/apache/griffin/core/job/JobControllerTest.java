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

import static org.apache.griffin.core.exception.GriffinExceptionMessage.INSTANCE_ID_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_ID_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.JOB_NAME_DOES_NOT_EXIST;
import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinJob;
import static org.apache.griffin.core.util.EntityMocksHelper.createJobInstance;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.exception.GriffinExceptionHandler;
import org.apache.griffin.core.exception.GriffinExceptionMessage;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.util.URLHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
public class JobControllerTest {

    private MockMvc mvc;

    @Mock
    private JobServiceImpl service;

    @InjectMocks
    private JobController controller;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GriffinExceptionHandler())
                .build();
    }


    @Test
    public void testGetJobs() throws Exception {
        AbstractJob jobBean = createGriffinJob();
        jobBean.setJobName("job_name");
        given(service.getAliveJobs(""))
                .willReturn(Collections.singletonList(jobBean));

        mvc.perform(
                get(URLHelper.API_VERSION_PATH + "/jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]['job.name']", is("job_name")));
    }

    @Test
    public void testDeleteJobByIdForSuccess() throws Exception {
        doNothing().when(service).deleteJob(1L);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/jobs/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteJobByIdForFailureWithNotFound() throws Exception {
        doThrow(new GriffinException.NotFoundException(JOB_ID_DOES_NOT_EXIST))
                .when(service).deleteJob(1L);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/jobs/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteJobByIdForFailureWithException() throws Exception {
        doThrow(new GriffinException.ServiceException("Failed to delete job",
                new Exception()))
                .when(service).deleteJob(1L);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/jobs/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testDeleteJobByNameForSuccess() throws Exception {
        String jobName = "jobName";
        doNothing().when(service).deleteJob(jobName);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/jobs").param("jobName"
                , jobName))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteJobByNameForFailureWithNotFound() throws Exception {
        String jobName = "jobName";
        doThrow(new GriffinException.NotFoundException(JOB_NAME_DOES_NOT_EXIST))
                .when(service).deleteJob(jobName);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/jobs").param("jobName"
                , jobName))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteJobByNameForFailureWithException() throws Exception {
        String jobName = "jobName";
        doThrow(new GriffinException.ServiceException("Failed to delete job",
                new Exception()))
                .when(service).deleteJob(jobName);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/jobs").param("jobName"
                , jobName))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testFindInstancesOfJob() throws Exception {
        int page = 0;
        int size = 2;
        JobInstanceBean jobInstance = new JobInstanceBean(1L, LivySessionStates
                .State.RUNNING, "", "", null, null);
        given(service.findInstancesOfJob(1L, page, size)).willReturn(Arrays
                .asList(jobInstance));

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/jobs/instances").param
                ("jobId", String.valueOf(1L))
                .param("page", String.valueOf(page)).param("size",
                        String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].state", is("RUNNING")));
    }

    @Test
    public void testFindInstance() throws Exception {
        JobInstanceBean jobInstance = new JobInstanceBean(1L, LivySessionStates
                .State.RUNNING, "", "", null, null);
        given(service.findInstance(1L)).willReturn(jobInstance);

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/jobs/instances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state", is("RUNNING")));
    }

    @Test
    public void testFindInstanceForFailureWithNotFound() throws Exception {
        Long id = 1L;
        doThrow(new GriffinException.NotFoundException(INSTANCE_ID_DOES_NOT_EXIST))
            .when(service).findInstance(id);

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/jobs/instances/1"))
           .andExpect(status().isNotFound());
    }

    @Test
    public void testJobInstanceWithGivenIdNotFound() throws Exception {
        Long jobInstanceId = 2L;
        doThrow(new GriffinException.NotFoundException(GriffinExceptionMessage.JOB_INSTANCE_NOT_FOUND))
                .when(service).findInstance(jobInstanceId);

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/jobs/instances/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetHealthInfo() throws Exception {
        JobHealth jobHealth = new JobHealth(1, 3);
        given(service.getHealthInfo()).willReturn(jobHealth);

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/jobs/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthyJobCount", is(1)));
    }

    @Test
    public void testTriggerJobForSuccess() throws Exception {
        Long id = 1L;
        given(service.triggerJobById(id)).willReturn(null);

        mvc.perform(post(URLHelper.API_VERSION_PATH + "/jobs/trigger/1"))
                .andExpect(status().isOk());
    }

    @Test
    public void testTriggerJobForFailureWithException() throws Exception {
        doThrow(new GriffinException.ServiceException("Failed to trigger job",
                new Exception()))
                .when(service).triggerJobById(1L);

        mvc.perform(post(URLHelper.API_VERSION_PATH + "/jobs/trigger/1"))
                .andExpect(status().isInternalServerError());
    }
}
