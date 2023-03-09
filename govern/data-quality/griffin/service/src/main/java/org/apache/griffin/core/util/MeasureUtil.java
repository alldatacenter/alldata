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

package org.apache.griffin.core.util;

import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_CONNECTOR_NAME;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.INVALID_MEASURE_PREDICATE;
import static org.apache.griffin.core.exception.GriffinExceptionMessage.MISSING_METRIC_NAME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.job.entity.SegmentPredicate;
import org.apache.griffin.core.job.factory.PredicatorFactory;
import org.apache.griffin.core.measure.entity.DataConnector;
import org.apache.griffin.core.measure.entity.DataSource;
import org.apache.griffin.core.measure.entity.ExternalMeasure;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeasureUtil {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(MeasureUtil.class);

    public static void validateMeasure(Measure measure) {
        if (measure instanceof GriffinMeasure) {
            validateGriffinMeasure((GriffinMeasure) measure);
        } else if (measure instanceof ExternalMeasure) {
            validateExternalMeasure((ExternalMeasure) measure);
        }

    }

    private static void validateGriffinMeasure(GriffinMeasure measure) {
        if (getConnectorNamesIfValid(measure) == null) {
            throw new GriffinException.BadRequestException
                    (INVALID_CONNECTOR_NAME);
        }
        if (!validatePredicates(measure)) {
            throw new GriffinException.BadRequestException(INVALID_MEASURE_PREDICATE);
        }
    }

    private static boolean validatePredicates(GriffinMeasure measure) {
        for (DataSource dataSource : measure.getDataSources()) {
            for (SegmentPredicate segmentPredicate : dataSource.getConnector().getPredicates()) {
                try {
                    PredicatorFactory.newPredicateInstance(segmentPredicate);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void validateExternalMeasure(ExternalMeasure measure) {
        if (StringUtils.isBlank(measure.getMetricName())) {
            LOGGER.warn("Failed to create external measure {}. " +
                    "Its metric name is blank.", measure.getName());
            throw new GriffinException.BadRequestException(MISSING_METRIC_NAME);
        }
    }

    private static List<String> getConnectorNamesIfValid(GriffinMeasure measure) {
        Set<String> sets = new HashSet<>();
        List<DataSource> sources = measure.getDataSources();
        for (DataSource source : sources) {
            if(source.getConnector() != null && source.getConnector().getName() != null){
                sets.add(source.getConnector().getName());
            }
        }
        if (sets.size() == 0 || sets.size() < sources.size()) {
            LOGGER.warn("Connector names cannot be repeated or empty.");
            return null;
        }
        return new ArrayList<>(sets);
    }
}
