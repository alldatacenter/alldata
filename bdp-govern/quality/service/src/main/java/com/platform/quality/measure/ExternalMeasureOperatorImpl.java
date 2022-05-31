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

package com.platform.quality.measure;

import static com.platform.quality.util.MeasureUtil.validateMeasure;

import com.platform.quality.job.entity.VirtualJob;
import com.platform.quality.job.repo.VirtualJobRepo;
import com.platform.quality.measure.entity.ExternalMeasure;
import com.platform.quality.measure.entity.Measure;
import com.platform.quality.measure.repo.ExternalMeasureRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("externalOperation")
public class ExternalMeasureOperatorImpl implements MeasureOperator {

    @Autowired
    private ExternalMeasureRepo measureRepo;
    @Autowired
    private VirtualJobRepo jobRepo;

    @Override
    @Transactional
    public Measure create(Measure measure) {
        ExternalMeasure em = (ExternalMeasure) measure;
        validateMeasure(em);
        em.setVirtualJob(new VirtualJob());
        em = measureRepo.save(em);
        VirtualJob vj = genVirtualJob(em, em.getVirtualJob());
        jobRepo.save(vj);
        return em;
    }

    @Override
    public Measure update(Measure measure) {
        ExternalMeasure latestMeasure = (ExternalMeasure) measure;
        validateMeasure(latestMeasure);
        ExternalMeasure originMeasure = measureRepo.findOne(
            latestMeasure.getId());
        VirtualJob vj = genVirtualJob(latestMeasure,
            originMeasure.getVirtualJob());
        latestMeasure.setVirtualJob(vj);
        measure = measureRepo.save(latestMeasure);
        return measure;
    }

    @Override
    public void delete(Measure measure) {
        ExternalMeasure em = (ExternalMeasure) measure;
        em.setDeleted(true);
        em.getVirtualJob().setDeleted(true);
        measureRepo.save(em);
    }

    private VirtualJob genVirtualJob(ExternalMeasure em, VirtualJob vj) {
        vj.setMeasureId(em.getId());
        vj.setJobName(em.getName());
        vj.setMetricName(em.getMetricName());
        return vj;
    }
}
