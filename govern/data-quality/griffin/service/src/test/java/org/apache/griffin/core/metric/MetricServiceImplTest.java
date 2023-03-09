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

import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinJob;
import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinMeasure;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.apache.griffin.core.job.entity.LivySessionStates;
import org.apache.griffin.core.job.repo.JobInstanceRepo;
import org.apache.griffin.core.job.repo.JobRepo;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.apache.griffin.core.metric.model.Metric;
import org.apache.griffin.core.metric.model.MetricValue;
import org.apache.griffin.core.util.JsonUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MetricServiceImplTest {

    @InjectMocks
    private MetricServiceImpl service;

    @Mock
    private MeasureRepo<Measure> measureRepo;
    @Mock
    private JobRepo<AbstractJob> jobRepo;
    @Mock
    private MetricStoreImpl metricStore;
    @Mock
    private JobInstanceRepo jobInstanceRepo;

    @Autowired
    private Environment env;

    @Before
    public void setup() {
    }

    @Test
    public void test() {
        Environment e = env;
        System.out.println(env
                .getProperty("spring.datasource.driver - class -name "));
    }

    @Test
    public void testGetAllMetricsSuccess() throws Exception {
        Measure measure = createGriffinMeasure("measureName");
        measure.setId(1L);
        AbstractJob job = createGriffinJob();
        MetricValue value = new MetricValue("jobName", 1L, new HashMap<>());
        given(jobRepo.findByDeleted(false)).willReturn(Collections
                .singletonList(job));
        given(measureRepo.findByDeleted(false)).willReturn(Collections
                .singletonList(measure));
        given(metricStore.getMetricValues(Matchers.anyString(),
                Matchers.anyInt(), Matchers.anyInt(),
                Matchers.anyLong()))
                .willReturn(Collections.singletonList(value));

        Map<String, List<Metric>> metricMap = service.getAllMetrics();
        assertEquals(metricMap.get("measureName").get(0).getName(), "jobName");
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testGetAllMetricsFailureWithException() throws Exception {
        Measure measure = createGriffinMeasure("measureName");
        measure.setId(1L);
        AbstractJob job = createGriffinJob();
        given(jobRepo.findByDeleted(false)).willReturn(Collections
                .singletonList(job));
        given(measureRepo.findByDeleted(false)).willReturn(Collections
                .singletonList(measure));
        given(metricStore.getMetricValues(Matchers.anyString(),
                Matchers.anyInt(), Matchers.anyInt(),
                Matchers.anyLong()))
                .willThrow(new IOException());

        service.getAllMetrics();
    }

    @Test
    public void testGetMetricValuesSuccess() throws IOException {
        MetricValue value = new MetricValue("jobName", 1L, new HashMap<>());
        given(metricStore.getMetricValues(Matchers.anyString(),
                Matchers.anyInt(), Matchers.anyInt(),
                Matchers.anyLong()))
                .willReturn(Collections.singletonList(value));

        List<MetricValue> values = service.getMetricValues("jobName", 0, 300,
                0);
        assertEquals(values.size(), 1);
        assertEquals(values.get(0).getName(), "jobName");
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testGetMetricValuesFailureWithException() throws IOException {
        given(metricStore.getMetricValues(Matchers.anyString(),
                Matchers.anyInt(), Matchers.anyInt(),
                Matchers.anyLong()))
                .willThrow(new IOException());

        service.getMetricValues("jobName", 0, 300, 0);
    }

    @Test
    public void testAddMetricValuesSuccess() throws IOException {
        Map<String, Object> value = new HashMap<>();
        value.put("total", 10000);
        value.put("matched", 10000);
        List<MetricValue> values = Collections.singletonList(
                new MetricValue("jobName", 1L, value));
        given(metricStore.addMetricValues(values))
                .willReturn(
                        new ResponseEntity(
                                "{\"errors\": false, \"items\": []}",
                                HttpStatus.OK));

        ResponseEntity response = service.addMetricValues(values);
        Map body = JsonUtil.toEntity(response.getBody().toString(), Map.class);

        assertEquals(response.getStatusCode(), HttpStatus.OK);

        assertNotNull(body);

        assertEquals(body.get("errors").toString(), "false");
    }

    @Test(expected = GriffinException.BadRequestException.class)
    public void testAddMetricValuesFailureWithInvalidFormat() {
        List<MetricValue> values = Collections.singletonList(new MetricValue());

        service.addMetricValues(values);
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testAddMetricValuesFailureWithException() throws IOException {
        Map<String, Object> value = new HashMap<>();
        value.put("total", 10000);
        value.put("matched", 10000);
        List<MetricValue> values = Collections.singletonList(
                new MetricValue("jobName", 1L, value));
        given(metricStore.addMetricValues(values)).willThrow(new IOException());

        service.addMetricValues(values);
    }

    @Test
    public void testDeleteMetricValuesSuccess() throws IOException {

        given(metricStore.deleteMetricValues("metricName"))
                .willReturn(new ResponseEntity("{\"failures\": []}",
                        HttpStatus.OK));

        ResponseEntity response = service.deleteMetricValues("metricName");
        Map body = JsonUtil.toEntity(response.getBody().toString(), Map.class);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertNotNull(body);
        assertEquals(body.get("failures"), Collections.emptyList());
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testDeleteMetricValuesFailureWithException()
            throws IOException {
        given(metricStore.deleteMetricValues("metricName"))
                .willThrow(new IOException());

        service.deleteMetricValues("metricName");

    }

    @Test
    public void testFindMetricSuccess() throws IOException {
        Long id = 1L;
        String appId = "application";
        MetricValue expectedMetric = new MetricValue(
                "name", 1234L, Collections.singletonMap("applicationId", appId), new HashMap<>());

        given(jobInstanceRepo.findByInstanceId(id))
                .willReturn(new JobInstanceBean(LivySessionStates.State.RUNNING, 12L, 32L, appId));
        given(metricStore.getMetric(appId))
                .willReturn(expectedMetric);
        MetricValue actualMetric = service.findMetric(id);

        assertEquals(expectedMetric, actualMetric);
    }

    @Test(expected = GriffinException.NotFoundException.class)
    public void testFailedToFindJobInstance() throws IOException {
        Long id = 1L;
        given(jobInstanceRepo.findByInstanceId(id))
                .willReturn(null);
        service.findMetric(id);

    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testFindMetricFailure() throws IOException {
        Long id = 1L;
        String appId = "application";

        given(jobInstanceRepo.findByInstanceId(id))
                .willReturn(new JobInstanceBean(LivySessionStates.State.RUNNING, 12L, 32L, appId));
        given(metricStore.getMetric(appId))
                .willThrow(new GriffinException.ServiceException("", new RuntimeException()));
        service.findMetric(id);

    }


}
