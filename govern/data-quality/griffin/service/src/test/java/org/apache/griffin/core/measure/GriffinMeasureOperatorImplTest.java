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

import static org.apache.griffin.core.util.EntityMocksHelper.createDataConnector;
import static org.apache.griffin.core.util.EntityMocksHelper.createGriffinMeasure;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.JobServiceImpl;
import org.apache.griffin.core.measure.entity.DataConnector;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.DataConnectorRepo;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class GriffinMeasureOperatorImplTest {

    @InjectMocks
    private GriffinMeasureOperatorImpl operator;

    @Mock
    private MeasureRepo<Measure> measureRepo;
    @Mock
    private DataConnectorRepo dcRepo;
    @Mock
    private JobServiceImpl jobService;


    @Before
    public void setup() {
    }

    @Test
    public void testCreateForSuccess() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.save(measure)).willReturn(measure);

        Measure m = operator.create(measure);
        assertEquals(m.getName(), measure.getName());
    }

    @Test(expected = GriffinException.BadRequestException.class)
    public void testCreateForFailureWithConnectorNull() throws Exception {
        String measureName = "view_item_hourly";
        DataConnector dcSource = createDataConnector(null, "default",
                "test_data_src", "dt=#YYYYMMdd# AND hour =#HH#");
        DataConnector dcTarget = createDataConnector(null, "default",
                "test_data_tgt", "dt=#YYYYMMdd# AND hour =#HH#");
        GriffinMeasure measure = createGriffinMeasure(measureName, dcSource,
                dcTarget);

        operator.create(measure);
    }

    @Test
    public void testUpdateForSuccess() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        given(measureRepo.save(measure)).willReturn(measure);

        operator.update(measure);
        verify(measureRepo, times(1)).save(measure);
    }

    @Test
    public void testDeleteForSuccess() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        measure.setId(1L);
        doNothing().when(jobService).deleteJobsRelateToMeasure(1L);
        given(measureRepo.save(measure)).willReturn(measure);

        operator.update(measure);
        verify(measureRepo, times(1)).save(measure);
    }

    @Test(expected = GriffinException.ServiceException.class)
    public void testDeleteForFailureWithPauseJob() throws Exception {
        Measure measure = createGriffinMeasure("view_item_hourly");
        measure.setId(1L);
        doThrow(new GriffinException.ServiceException("Service exception",
                new RuntimeException()))
                .when(jobService).deleteJobsRelateToMeasure(1L);

        operator.delete(measure);
    }

}
