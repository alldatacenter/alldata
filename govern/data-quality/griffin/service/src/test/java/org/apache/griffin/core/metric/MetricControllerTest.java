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

import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_METRIC_VALUE_FORMAT;
import static org.apache.griffin.core.measure.entity.DqType.ACCURACY;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.exception.GriffinExceptionHandler;
import org.apache.griffin.core.metric.model.Metric;
import org.apache.griffin.core.metric.model.MetricValue;
import org.apache.griffin.core.util.JsonUtil;
import org.apache.griffin.core.util.URLHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
public class MetricControllerTest {

    private MockMvc mvc;

    @InjectMocks
    private MetricController controller;

    @Mock
    private MetricServiceImpl service;


    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GriffinExceptionHandler())
                .build();
    }

    @Test
    public void testGetAllMetricsSuccess() throws Exception {
        Metric metric = new Metric("metricName", ACCURACY, "owner", Collections
                .emptyList());
        given(service.getAllMetrics()).willReturn(
                Collections.singletonMap("measureName", Collections
                        .singletonList(metric)));


        mvc.perform(get(URLHelper.API_VERSION_PATH + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measureName", hasSize(1)));
    }

    @Test
    public void testGetAllMetricsFailureWithException() throws Exception {
        given(service.getAllMetrics())
                .willThrow(new GriffinException.ServiceException(
                        "Failed to get metrics", new RuntimeException()));

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/metrics"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetMetricValuesSuccess() throws Exception {
        MetricValue value = new MetricValue("jobName", 1L, new HashMap<>());
        given(service.getMetricValues(Matchers.anyString(), Matchers.anyInt(),
                Matchers.anyInt(), Matchers.anyLong()))
                .willReturn(Collections.singletonList(value));

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/metrics/values")
                .param("metricName", "jobName")
                .param("size", "5"))
                .andExpect(jsonPath("$.[0].name", is("jobName")));
    }

    @Test
    public void testGetMetricValuesFailureWithException() throws Exception {
        given(service.getMetricValues(Matchers.anyString(), Matchers.anyInt(),
                Matchers.anyInt(), Matchers.anyLong()))
                .willThrow(new GriffinException.ServiceException(
                        "Failed to get metric values", new IOException()));

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/metrics/values")
                .param("metricName", "jobName")
                .param("size", "5"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testAddMetricValuesSuccess() throws Exception {
        List<MetricValue> values = Collections.singletonList(new MetricValue());
        given(service.addMetricValues(Matchers.any()))
                .willReturn(
                        new ResponseEntity<>(
                                "{\"errors\": false, \"items\": []}",
                                HttpStatus.OK));

        mvc.perform(

                post(URLHelper.API_VERSION_PATH + "/metrics/values")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.toJson(values)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors", is(false)));
    }

    @Test
    public void testAddMetricValuesFailureWithException() throws Exception {
        List<MetricValue> values = Collections.singletonList(new MetricValue());
        given(service.addMetricValues(Matchers.any()))
                .willThrow(new GriffinException.ServiceException(
                        "Failed to add metric values", new IOException()));
        mvc.perform(post(URLHelper.API_VERSION_PATH + "/metrics/values")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.toJson(values)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testAddMetricValuesFailureWithInvalidFormat() throws Exception {
        List<MetricValue> values = Collections.singletonList(new MetricValue());
        given(service.addMetricValues(Matchers.any()))
                .willThrow(new GriffinException.BadRequestException
                        (INVALID_METRIC_VALUE_FORMAT));
        mvc.perform(post(URLHelper.API_VERSION_PATH + "/metrics/values")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.toJson(values)))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testDeleteMetricValuesSuccess() throws Exception {
        given(service.deleteMetricValues("metricName"))
                .willReturn(new ResponseEntity<>("{\"failures\": []}",
                        HttpStatus.OK));

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/metrics/values")
                .param("metricName", "metricName"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failures", hasSize(0)));
    }

    @Test
    public void testDeleteMetricValuesFailureWithException() throws Exception {
        given(service.deleteMetricValues("metricName"))
                .willThrow(new GriffinException.ServiceException(
                        "Failed to delete metric values.",
                        new IOException()));

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/metrics/values")
                .param("metricName", "metricName"))
                .andExpect(status().isInternalServerError());
    }

}
