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


import static org.apache.griffin.core.exception.GriffinExceptionMessage.MEASURE_ID_DOES_NOT_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MEASURE_NAME_ALREADY_EXIST;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MEASURE_TYPE_DOES_NOT_MATCH;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MEASURE_TYPE_DOES_NOT_SUPPORT;

import java.util.List;

import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.measure.entity.ExternalMeasure;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.ExternalMeasureRepo;
import org.apache.griffin.core.measure.repo.GriffinMeasureRepo;
import org.apache.griffin.core.measure.repo.MeasureRepo;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class MeasureServiceImpl implements MeasureService {
    private static final Logger LOGGER = LoggerFactory
        .getLogger(MeasureServiceImpl.class);
    private static final String GRIFFIN = "griffin";
    private static final String EXTERNAL = "external";

    @Autowired
    private MeasureRepo<Measure> measureRepo;
    @Autowired
    private GriffinMeasureRepo griffinMeasureRepo;
    @Autowired
    private ExternalMeasureRepo externalMeasureRepo;
    @Autowired
    @Qualifier("griffinOperation")
    private MeasureOperator griffinOp;
    @Autowired
    @Qualifier("externalOperation")
    private MeasureOperator externalOp;

    @Override
    public List<? extends Measure> getAllAliveMeasures(String type) {
        if (type.equals(GRIFFIN)) {
            return griffinMeasureRepo.findByDeleted(false);
        } else if (type.equals(EXTERNAL)) {
            return externalMeasureRepo.findByDeleted(false);
        }
        return measureRepo.findByDeleted(false);
    }

    @Override
    public Measure getMeasureById(long id) {
        Measure measure = measureRepo.findByIdAndDeleted(id, false);
        if (measure == null) {
            throw new GriffinException
                .NotFoundException(MEASURE_ID_DOES_NOT_EXIST);
        }
        return measure;
    }

    @Override
    public List<Measure> getAliveMeasuresByOwner(String owner) {
        return measureRepo.findByOwnerAndDeleted(owner, false);
    }

    @Override
    public Measure createMeasure(Measure measure) {
        List<Measure> aliveMeasureList = measureRepo
            .findByNameAndDeleted(measure.getName(), false);
        if (!CollectionUtils.isEmpty(aliveMeasureList)) {
            LOGGER.warn("Failed to create new measure {}, it already exists.",
                measure.getName());
            throw new GriffinException.ConflictException(
                MEASURE_NAME_ALREADY_EXIST);
        }
        MeasureOperator op = getOperation(measure);
        return op.create(measure);
    }

    @Override
    public Measure updateMeasure(Measure measure) {
        Measure m = measureRepo.findByIdAndDeleted(measure.getId(), false);
        if (m == null) {
            throw new GriffinException.NotFoundException(
                MEASURE_ID_DOES_NOT_EXIST);
        }
        if (!m.getType().equals(measure.getType())) {
            LOGGER.warn("Can't update measure to different type.");
            throw new GriffinException.BadRequestException(
                MEASURE_TYPE_DOES_NOT_MATCH);
        }
        MeasureOperator op = getOperation(measure);
        return op.update(measure);
    }

    @Override
    public void deleteMeasureById(Long measureId) throws SchedulerException {
        Measure measure = measureRepo.findByIdAndDeleted(measureId, false);
        if (measure == null) {
            throw new GriffinException.NotFoundException(
                MEASURE_ID_DOES_NOT_EXIST);
        }
        MeasureOperator op = getOperation(measure);
        op.delete(measure);
    }

    @Override
    public void deleteMeasures() throws SchedulerException {
        List<Measure> measures = measureRepo.findByDeleted(false);
        for (Measure m : measures) {
            MeasureOperator op = getOperation(m);
            op.delete(m);
        }
    }

    private MeasureOperator getOperation(Measure measure) {
        if (measure instanceof GriffinMeasure) {
            return griffinOp;
        } else if (measure instanceof ExternalMeasure) {
            return externalOp;
        }
        throw new GriffinException.BadRequestException(
            MEASURE_TYPE_DOES_NOT_SUPPORT);
    }

}
