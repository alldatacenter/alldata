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

import static org.apache.griffin.core.exception.GriffinExceptionMessage.ORGANIZATION_NAME_DOES_NOT_EXIST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.griffin.core.measure.entity.GriffinMeasure;
import org.apache.griffin.core.measure.entity.Measure;
import org.apache.griffin.core.measure.repo.GriffinMeasureRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MeasureOrgServiceImpl implements MeasureOrgService {

    @Autowired
    private GriffinMeasureRepo measureRepo;

    @Override
    public List<String> getOrgs() {
        return measureRepo.findOrganizations(false);
    }

    @Override
    public List<String> getMetricNameListByOrg(String org) {
        List<String> orgs = measureRepo.findNameByOrganization(org, false);
        if (CollectionUtils.isEmpty(orgs)) {
            throw new GriffinException.NotFoundException
                (ORGANIZATION_NAME_DOES_NOT_EXIST);
        }
        return orgs;
    }

    @Override
    public Map<String, List<String>> getMeasureNamesGroupByOrg() {
        Map<String, List<String>> orgWithMetricsMap = new HashMap<>();
        List<GriffinMeasure> measures = measureRepo.findByDeleted(false);
        for (Measure measure : measures) {
            String orgName = measure.getOrganization();
            orgName = orgName == null ? "null" : orgName;
            String measureName = measure.getName();
            List<String> measureList = orgWithMetricsMap.getOrDefault(orgName,
                new ArrayList<>());
            measureList.add(measureName);
            orgWithMetricsMap.put(orgName, measureList);
        }
        return orgWithMetricsMap;
    }

    @Override
    public Map<String, Map<String, List<Map<String, Object>>>>
    getMeasureWithJobDetailsGroupByOrg(Map<String,
        List<Map<String, Object>>> jobDetails) {
        Map<String, Map<String, List<Map<String, Object>>>> result =
            new HashMap<>();
        List<GriffinMeasure> measures = measureRepo.findByDeleted(false);
        if (measures == null) {
            return null;
        }
        for (Measure measure : measures) {
            String orgName = measure.getOrganization();
            String measureName = measure.getName();
            String measureId = measure.getId().toString();
            List<Map<String, Object>> jobList = jobDetails
                .getOrDefault(measureId, new ArrayList<>());
            Map<String, List<Map<String, Object>>> measureWithJobs = result
                .getOrDefault(orgName, new HashMap<>());
            measureWithJobs.put(measureName, jobList);
            result.put(orgName, measureWithJobs);
        }
        return result;
    }
}
