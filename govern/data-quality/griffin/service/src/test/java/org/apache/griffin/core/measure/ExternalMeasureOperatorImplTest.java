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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.VirtualJob;
import org.apache.griffin.core.job.repo.VirtualJobRepo;
import org.apache.griffin.core.measure.entity.ExternalMeasure;
import org.apache.griffin.core.measure.repo.ExternalMeasureRepo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ExternalMeasureOperatorImplTest {

    @InjectMocks
    private ExternalMeasureOperatorImpl operator;

    @Mock
    private ExternalMeasureRepo measureRepo;
    @Mock
    private VirtualJobRepo jobRepo;

    @Before
    public void setup() {
    }


    @Test
    public void testCreateForSuccess() {
        ExternalMeasure measure = createExternalMeasure("view_item_hourly");
        given(measureRepo.save(measure)).willReturn(measure);
        given(jobRepo.save(Matchers.any(VirtualJob.class))).willReturn(
                new VirtualJob());

        operator.create(measure);
        verify(jobRepo, times(1)).save(new VirtualJob());
    }

    @Test(expected = GriffinException.BadRequestException.class)
    public void testCreateForFailureWithBlankMetricName() {
        String measureName = "view_item_hourly";
        ExternalMeasure measure = createExternalMeasure(measureName);
        measure.setMetricName("  ");
        operator.create(measure);
    }

    @Test
    public void testUpdateForSuccess() {
        ExternalMeasure measure = createExternalMeasure("view_item_hourly");
        measure.setId(1L);
        given(measureRepo.findOne(1L)).willReturn(measure);
        given(measureRepo.save(Matchers.any(ExternalMeasure.class)))
                .willReturn(measure);

        operator.create(measure);
        verify(measureRepo, times(1)).save(
                Matchers.any(ExternalMeasure.class));
    }

    @Test(expected = GriffinException.BadRequestException.class)
    public void testUpdateForFailureWithBlankMetricName() {
        String measureName = "view_item_hourly";
        ExternalMeasure measure = createExternalMeasure(measureName);
        measure.setMetricName("  ");

        operator.update(measure);
    }

    @Test
    public void testDeleteForSuccess() {
        ExternalMeasure measure = createExternalMeasure("view_item_hourly");
        given(measureRepo.save(measure)).willReturn(measure);

        operator.delete(measure);
        verify(measureRepo, times(1)).save(measure);
    }
}
