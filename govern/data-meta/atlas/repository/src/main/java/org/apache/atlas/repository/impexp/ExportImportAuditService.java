/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.impexp;

import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.discovery.AtlasDiscoveryService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.atlas.repository.ogm.ExportImportAuditEntryDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AtlasService
public class ExportImportAuditService {
    private static final Logger LOG = LoggerFactory.getLogger(ExportImportAuditService.class);
    private static final String ENTITY_TYPE_NAME = "__ExportImportAuditEntry";

    private final DataAccess dataAccess;
    private AtlasDiscoveryService discoveryService;

    @Inject
    public ExportImportAuditService(DataAccess dataAccess, AtlasDiscoveryService discoveryService) {
        this.dataAccess = dataAccess;
        this.discoveryService = discoveryService;
    }

    @GraphTransaction
    public void save(ExportImportAuditEntry entry) throws AtlasBaseException {
        dataAccess.saveNoLoad(entry);
    }
    public ExportImportAuditEntry get(ExportImportAuditEntry entry) throws AtlasBaseException {
        if(entry.getGuid() == null) {
            throw new AtlasBaseException("entity does not have GUID set. load cannot proceed.");
        }
        return dataAccess.load(entry);
    }

    public List<ExportImportAuditEntry> get(String userName, String operation, String cluster,
                                            String startTime, String endTime,
                                            int limit, int offset) throws AtlasBaseException {
        SearchParameters.FilterCriteria criteria = new SearchParameters.FilterCriteria();
        criteria.setCondition(SearchParameters.FilterCriteria.Condition.AND);
        criteria.setCriterion(new ArrayList<>());

        addSearchParameters(criteria, userName, operation, cluster, startTime, endTime);

        SearchParameters searchParameters = getSearchParameters(limit, offset, criteria);
        searchParameters.setAttributes(getAuditEntityAttributes());

        AtlasSearchResult result = discoveryService.searchWithParameters(searchParameters);
        return toExportImportAuditEntry(result);
    }

    private Set<String> getAuditEntityAttributes() {
        return ExportImportAuditEntryDTO.getAttributes();
    }

    private List<ExportImportAuditEntry> toExportImportAuditEntry(AtlasSearchResult result) {
        List<ExportImportAuditEntry> ret = new ArrayList<>();
        if(CollectionUtils.isEmpty(result.getEntities())) {
            return ret;
        }

        for (AtlasEntityHeader entityHeader : result.getEntities()) {
            ExportImportAuditEntry entry = ExportImportAuditEntryDTO.from(entityHeader.getGuid(),
                                                                            entityHeader.getAttributes());
            if(entry == null) {
                continue;
            }

            ret.add(entry);
        }

        return ret;
    }

    private SearchParameters getSearchParameters(int limit, int offset, SearchParameters.FilterCriteria criteria) {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setTypeName(ENTITY_TYPE_NAME);
        searchParameters.setEntityFilters(criteria);
        searchParameters.setLimit(limit);
        searchParameters.setOffset(offset);

        return searchParameters;
    }

    private void addSearchParameters(SearchParameters.FilterCriteria criteria, String userName, String operation,
                                     String cluster, String startTime, String endTime) {
        addParameterIfValueNotEmpty(criteria, ExportImportAuditEntryDTO.PROPERTY_USER_NAME, userName);
        addParameterIfValueNotEmpty(criteria, ExportImportAuditEntryDTO.PROPERTY_OPERATION, operation);
        addParameterIfValueNotEmpty(criteria, ExportImportAuditEntryDTO.PROPERTY_START_TIME, startTime);
        addParameterIfValueNotEmpty(criteria, ExportImportAuditEntryDTO.PROPERTY_END_TIME, endTime);

        addServerFilterCriteria(criteria, cluster);
    }

    private void addServerFilterCriteria(SearchParameters.FilterCriteria parentCriteria, String cluster) {
        if (StringUtils.isEmpty(cluster)) {
            return;
        }

        SearchParameters.FilterCriteria criteria = new SearchParameters.FilterCriteria();
        criteria.setCondition(SearchParameters.FilterCriteria.Condition.OR);
        criteria.setCriterion(new ArrayList<>());

        addParameterIfValueNotEmpty(criteria, ExportImportAuditEntryDTO.PROPERTY_SOURCE_SERVER_NAME, cluster);
        addParameterIfValueNotEmpty(criteria, ExportImportAuditEntryDTO.PROPERTY_TARGET_SERVER_NAME, cluster);

        parentCriteria.getCriterion().add(criteria);
    }

    private void addParameterIfValueNotEmpty(SearchParameters.FilterCriteria criteria, String attributeName, String value) {
        if(StringUtils.isEmpty(value)) {
            return;
        }

        SearchParameters.FilterCriteria filterCriteria = new SearchParameters.FilterCriteria();
        filterCriteria.setAttributeName(attributeName);
        filterCriteria.setAttributeValue(value);
        filterCriteria.setOperator(SearchParameters.Operator.EQ);

        criteria.getCriterion().add(filterCriteria);
    }

    public void add(String userName, String sourceCluster, String targetCluster, String operation,
                               String result, long startTime, long endTime, boolean hasData) throws AtlasBaseException {
        if(!hasData) return;

        ExportImportAuditEntry entry = new ExportImportAuditEntry();

        entry.setUserName(userName);
        entry.setSourceServerName(sourceCluster);
        entry.setTargetServerName(targetCluster);
        entry.setOperation(operation);
        entry.setResultSummary(result);
        entry.setStartTime(startTime);
        entry.setEndTime(endTime);

        save(entry);
        LOG.info("addAuditEntry: user: {}, source: {}, target: {}, operation: {}", entry.getUserName(),
                entry.getSourceServerName(), entry.getTargetServerName(), entry.getOperation());
    }
}
