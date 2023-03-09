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


import static org.apache.griffin.core.util.EntityMocksHelper.createExternalMeasure;
import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinMeasure;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.measure.entity.ExternalMeasure;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Component
public class MeasureServiceImplTest {

    @InjectMocks
    private MeasureServiceImpl service;

    @Mock
    private MeasureOperator externalOp;

    @Mock
    private MeasureOperator griffinOp;

    @Mock
    private MeasureRepo<Measure> measureRepo;

    @Value("${hive.hmshandler.retry.attempts}")
    private String attempts;


    @Test
    public void test() {
        System.out.println(attempts);
    }


    @Before
    public void setup() {
    }

    @Test
    public void testGetAllMeasures() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.findByDeleted(false)).willReturn(Collections
                .singletonList(measure));

        List<? extends Measure> measures = service.getAllAliveMeasures("");
        assertEquals(measures.size(), 1);
        assertEquals(measures.get(0).getName(), "view_item_hourly");
    }

    @Test
    public void testGetMeasuresById() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.findByIdAndDeleted(1L, false)).willReturn(measure);

        Measure m = service.getMeasureById(1);
        assertEquals(m.getName(), measure.getName());
    }

    @Test(expected = GriffinException.NotFoundException.class)
    public void testGetMeasuresByIdWithFileNotFoundException() {
        given(measureRepo.findByIdAndDeleted(1L, false)).willReturn(null);
        service.getMeasureById(1);
    }

    @Test
    public void testGetAliveMeasuresByOwner() throws Exception {
        String owner = "test";
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.findByOwnerAndDeleted(owner, false))
                .willReturn(Collections.singletonList(measure));

        List<Measure> measures = service.getAliveMeasuresByOwner(owner);
        assertEquals(measures.get(0).getName(), measure.getName());
    }


    @Test
    public void testDeleteMeasureByIdForGriffinSuccess() throws Exception {
        GriffinMeasure measure = createGriffinMeasure("view_item_hourly");
        measure.setId(1L);
        given(measureRepo.findByIdAndDeleted(measure.getId(), false))
                .willReturn(measure);
        doNothing().when(griffinOp).delete(measure);

        service.deleteMeasureById(measure.getId());
        verify(griffinOp, times(1)).delete(measure);
    }

    @Test
    public void testDeleteMeasureByIdForExternalSuccess() throws
            SchedulerException {
        ExternalMeasure measure = createExternalMeasure("externalMeasure");
        measure.setId(1L);
        given(measureRepo.findByIdAndDeleted(measure.getId(), false))
                .willReturn(measure);
        doNothing().when(externalOp).delete(measure);

        service.deleteMeasureById(1L);
        verify(externalOp, times(1)).delete(measure);
    }

    @Test(expected = GriffinException.NotFoundException.class)
    public void testDeleteMeasureByIdFailureWithNotFound() throws
            SchedulerException {
        given(measureRepo.findByIdAndDeleted(1L, false)).willReturn(null);
        service.deleteMeasureById(1L);
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testDeleteMeasureByIdForGriffinFailureWithException() throws
            Exception {
        GriffinMeasure measure = createGriffinMeasure("externalMeasure");
        measure.setId(1L);
        given(measureRepo.findByIdAndDeleted(measure.getId(), false))
                .willReturn(measure);
        doThrow(new GriffinException.ServiceException("Failed to delete job",
                new Exception()))
                .when(griffinOp).delete(measure);
        service.deleteMeasureById(1L);
    }

    @Test
    public void testDeleteMeasuresForGriffinSuccess() throws Exception {
        GriffinMeasure measure = createGriffinMeasure("view_item_hourly");
        measure.setId(1L);
        given(measureRepo.findByDeleted(false)).willReturn(Arrays
                .asList(measure));
        doNothing().when(griffinOp).delete(measure);
        service.deleteMeasures();
    }

    @Test
    public void testDeleteMeasuresForExternalSuccess() throws SchedulerException {
        ExternalMeasure measure = createExternalMeasure("externalMeasure");
        measure.setId(1L);
        given(measureRepo.findByDeleted(false)).willReturn(Arrays
                .asList(measure));
        doNothing().when(externalOp).delete(measure);
        service.deleteMeasures();
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testDeleteMeasuresForGriffinFailureWithException() throws
            Exception {
        GriffinMeasure measure = createGriffinMeasure("externalMeasure");
        measure.setId(1L);
        given(measureRepo.findByDeleted(false)).willReturn(Arrays
                .asList(measure));
        doThrow(new GriffinException.ServiceException("Failed to delete job",
                new Exception()))
                .when(griffinOp).delete(measure);
        service.deleteMeasures();
    }

    @Test
    public void testCreateMeasureForGriffinSuccess() throws Exception {
        String measureName = "view_item_hourly";
        GriffinMeasure griffinMeasure = createGriffinMeasure(measureName);
        given(measureRepo.findByNameAndDeleted(measureName, false))
                .willReturn(new ArrayList<>());
        given(griffinOp.create(griffinMeasure)).willReturn(griffinMeasure);

        Measure measure = service.createMeasure(griffinMeasure);
        assertEquals(measure.getName(), griffinMeasure.getName());
    }

    @Test
    public void testCreateMeasureForExternalSuccess() {
        String measureName = "view_item_hourly";
        ExternalMeasure externalMeasure = createExternalMeasure(measureName);
        given(measureRepo.findByNameAndDeleted(measureName, false))
                .willReturn(new ArrayList<>());
        given(externalOp.create(externalMeasure)).willReturn(externalMeasure);
        Measure measure = service.createMeasure(externalMeasure);
        assertEquals(measure.getName(), externalMeasure.getName());
    }

    @Test(expected = GriffinException.ConflictException.class)
    public void testCreateMeasureForFailureWithDuplicate() throws Exception {
        String measureName = "view_item_hourly";
        GriffinMeasure measure = createGriffinMeasure(measureName);
        given(measureRepo.findByNameAndDeleted(measureName, false))
                .willReturn(Collections.singletonList(measure));

        service.createMeasure(measure);
    }

    @Test
    public void testUpdateMeasureForGriffinSuccess() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.findByIdAndDeleted(measure.getId(), false))
                .willReturn(measure);
        doReturn(measure).when(externalOp).update(measure);

        service.updateMeasure(measure);
        verify(griffinOp, times(1)).update(measure);
    }

    @Test(expected = GriffinException.BadRequestException.class)
    public void testUpdateMeasureForGriffinFailureWithDiffType() throws
            Exception {
        Measure griffinMeasure = createGriffinMeasure("view_item_hourly");
        Measure externalMeasure = createExternalMeasure("externalName");
        given(measureRepo.findByIdAndDeleted(griffinMeasure.getId(), false))
                .willReturn(externalMeasure);

        service.updateMeasure(griffinMeasure);
    }

    @Test(expected = GriffinException.NotFoundException.class)
    public void testUpdateMeasureForFailureWithNotFound() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.findByIdAndDeleted(measure.getId(), false))
                .willReturn(null);

        service.updateMeasure(measure);
    }

    @Test
    public void testUpdateMeasureForExternal() {
        ExternalMeasure measure = createExternalMeasure
                ("external_view_item_hourly");
        given(measureRepo.findByIdAndDeleted(measure.getId(), false))
                .willReturn(measure);
        doReturn(measure).when(externalOp).update(measure);

        service.updateMeasure(measure);
        verify(externalOp, times(1)).update(measure);
    }
}
