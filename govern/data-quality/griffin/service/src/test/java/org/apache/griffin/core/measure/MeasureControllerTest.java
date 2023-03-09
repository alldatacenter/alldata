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

package org.apache.griffin.core.measure;

import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinMeasure;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.exception.GriffinExceptionHandler;
import org.apache.griffin.core.exception.GriffinExceptionMessage;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.util.JsonUtil;
import org.apache.griffin.core.util.URLHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
public class MeasureControllerTest {

    private MockMvc mvc;

    @Mock
    private MeasureServiceImpl service;

    @InjectMocks
    private MeasureController controller;


    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GriffinExceptionHandler())
                .build();
    }

    @Test
    public void testGetAllMeasures() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        Mockito.<List<? extends Measure>>when(service.getAllAliveMeasures(""))
                .thenReturn(Collections.singletonList(measure));

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/measures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].name", is("view_item_hourly")));
    }


    @Test
    public void testGetMeasuresById() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(service.getMeasureById(1L)).willReturn(measure);

        mvc.perform(get(URLHelper.API_VERSION_PATH + "/measures/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("view_item_hourly")));
    }

    @Test
    public void testDeleteMeasureByIdForSuccess() throws Exception {
        doNothing().when(service).deleteMeasureById(1L);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/measures/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteMeasureByIdForNotFound() throws Exception {
        doThrow(new GriffinException.NotFoundException(GriffinExceptionMessage
                .MEASURE_ID_DOES_NOT_EXIST))
                .when(service).deleteMeasureById(1L);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/measures/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteMeasureByIdForGriffinFailureWithException() throws
            Exception {
        doThrow(new GriffinException.ServiceException("Failed to delete job",
                new Exception()))
                .when(service).deleteMeasureById(1L);

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/measures/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testDeleteMeasuresForSuccess() throws Exception {
        doNothing().when(service).deleteMeasures();

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/measures"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteMeasuresForNotFound() throws Exception {
        doThrow(new GriffinException.NotFoundException(GriffinExceptionMessage
                .MEASURE_ID_DOES_NOT_EXIST))
                .when(service).deleteMeasures();

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/measures"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteMeasuresForGriffinFailureWithException()
            throws Exception {
        doThrow(new GriffinException.ServiceException("Failed to delete job",
                new Exception()))
                .when(service).deleteMeasures();

        mvc.perform(delete(URLHelper.API_VERSION_PATH + "/measures"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testUpdateMeasureForSuccess() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        String measureJson = JsonUtil.toJson(measure);
        doReturn(measure).when(service).updateMeasure(measure);

        mvc.perform(put(URLHelper.API_VERSION_PATH + "/measures")
                .contentType(MediaType.APPLICATION_JSON).content(measureJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("view_item_hourly")));
    }

    @Test
    public void testUpdateMeasureForNotFound() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        String measureJson = JsonUtil.toJson(measure);
        doThrow(new GriffinException.NotFoundException(GriffinExceptionMessage
                .MEASURE_ID_DOES_NOT_EXIST))
                .when(service).updateMeasure(measure);

        mvc.perform(put(URLHelper.API_VERSION_PATH + "/measures")
                .contentType(MediaType.APPLICATION_JSON).content(measureJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateMeasureForTypeMismatch() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        String measureJson = JsonUtil.toJson(measure);
        doThrow(new GriffinException.BadRequestException(GriffinExceptionMessage
                .MEASURE_TYPE_DOES_NOT_MATCH))
                .when(service).updateMeasure(measure);

        mvc.perform(put(URLHelper.API_VERSION_PATH + "/measures")
                .contentType(MediaType.APPLICATION_JSON).content(measureJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetAllMeasuresByOwner() throws Exception {
        String owner = "test";
        List<Measure> measureList = new LinkedList<>();
        Measure measure = createGriffinMeasure("view_item_hourly");
        measureList.add(measure);
        given(service.getAliveMeasuresByOwner(owner)).willReturn(measureList);

        mvc.perform(get(URLHelper.API_VERSION_PATH
                + "/measures/owner/" + owner)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].name", is("view_item_hourly")))
        ;
    }

    @Test
    public void testCreateNewMeasureForSuccess() throws Exception {
        GriffinMeasure measure = createGriffinMeasure("view_item_hourly");
        String measureJson = JsonUtil.toJson(measure);
        given(service.createMeasure(measure)).willReturn(measure);

        mvc.perform(post(URLHelper.API_VERSION_PATH + "/measures")
                .contentType(MediaType.APPLICATION_JSON).content(measureJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("view_item_hourly")));
    }

    @Test
    public void testCreateNewMeasureForFailWithDuplicate() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        String measureJson = JsonUtil.toJson(measure);
        doThrow(new GriffinException.ConflictException(GriffinExceptionMessage
                .MEASURE_NAME_ALREADY_EXIST))
                .when(service).createMeasure(measure);

        mvc.perform(post(URLHelper.API_VERSION_PATH + "/measures")
                .contentType(MediaType.APPLICATION_JSON).content(measureJson))
                .andExpect(status().isConflict());
    }

    @Test
    public void testCreateNewMeasureForFailWithInvalidParams() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        String measureJson = JsonUtil.toJson(measure);
        doThrow(new GriffinException.BadRequestException(GriffinExceptionMessage
                .MISSING_METRIC_NAME))
                .when(service).createMeasure(measure);

        mvc.perform(post(URLHelper.API_VERSION_PATH + "/measures")
                .contentType(MediaType.APPLICATION_JSON).content(measureJson))
                .andExpect(status().isBadRequest());
    }

}
