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

import static org.apache.griffin.core.util.MeasureUtil.validateMeasure;

import org.apache.griffin.core.job.JobServiceImpl;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("griffinOperation")
public class GriffinMeasureOperatorImpl implements MeasureOperator {
    private final MeasureRepo<Measure> measureRepo;

    private final JobServiceImpl jobService;

    @Autowired
    public GriffinMeasureOperatorImpl(MeasureRepo<Measure> measureRepo,
                                      JobServiceImpl jobService) {
        this.measureRepo = measureRepo;
        this.jobService = jobService;
    }


    @Override
    public Measure create(Measure measure) {
        validateMeasure(measure);
        return measureRepo.save(measure);
    }

    @Override
    public Measure update(Measure measure) {
        validateMeasure(measure);
        measure.setDeleted(false);
        measure = measureRepo.save(measure);
        return measure;
    }

    @Override
    public void delete(Measure measure) throws SchedulerException {
        jobService.deleteJobsRelateToMeasure(measure.getId());
        measure.setDeleted(true);
        measureRepo.save(measure);
    }
}
