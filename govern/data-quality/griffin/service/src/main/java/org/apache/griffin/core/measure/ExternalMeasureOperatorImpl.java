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

import org.apache.griffin.core.job.entity.VirtualJob;
import org.apache.griffin.core.job.repo.VirtualJobRepo;
import org.apache.griffin.core.measure.entity.ExternalMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.ExternalMeasureRepo;
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
