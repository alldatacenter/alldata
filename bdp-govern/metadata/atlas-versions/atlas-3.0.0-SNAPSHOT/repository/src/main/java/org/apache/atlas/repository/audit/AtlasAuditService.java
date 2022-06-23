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

package org.apache.atlas.repository.audit;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.annotation.AtlasService;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.discovery.AtlasDiscoveryService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.audit.AtlasAuditEntry.AuditOperation;
import org.apache.atlas.model.audit.AuditSearchParameters;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.repository.ogm.AtlasAuditEntryDTO;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@AtlasService
public class AtlasAuditService {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasAuditService.class);
    public static final String ENTITY_TYPE_AUDIT_ENTRY = "__AtlasAuditEntry";

    private final DataAccess dataAccess;
    private final AtlasDiscoveryService discoveryService;

    @Inject
    public AtlasAuditService(DataAccess dataAccess, AtlasDiscoveryService discoveryService) {
        this.dataAccess = dataAccess;
        this.discoveryService = discoveryService;
    }

    @GraphTransaction
    public void save(AtlasAuditEntry entry) throws AtlasBaseException {
        dataAccess.saveNoLoad(entry);
    }

    public void add(AuditOperation operation, String params, String result, long resultCount) throws AtlasBaseException {
        final Date startTime = new Date(RequestContext.get().getRequestTime());
        final Date endTime = new Date();
        add(operation, startTime, endTime, params, result, resultCount);
    }

    public void add(AuditOperation operation, Date startTime,
                         Date endTime, String params, String result, long resultCount) throws AtlasBaseException {
        String userName = RequestContext.get().getCurrentUser();
        String clientId = RequestContext.get().getClientIPAddress();
        if (StringUtils.isEmpty(clientId)) {
            try {
                clientId = InetAddress.getLocalHost().getHostName() + ":" +InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                LOG.error("Exception occurred during InetAddress retrieval", e);
                clientId = "unknown";
            }
        }
        add(userName, operation, clientId, startTime, endTime, params, result, resultCount);
    }

    public void add(String userName, AuditOperation operation, String clientId, Date startTime,
                    Date endTime, String params, String result, long resultCount) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasAuditService.add()");
        }

        AtlasAuditEntry entry = new AtlasAuditEntry();

        entry.setUserName(userName);
        entry.setOperation(operation);
        entry.setClientId(clientId);
        entry.setStartTime(startTime);
        entry.setEndTime(endTime);
        entry.setParams(params);
        entry.setResult(result);
        entry.setResultCount(resultCount);

        save(entry);

        if (LOG.isDebugEnabled()) {
            LOG.debug("addAuditEntry: user: {}, clientId: {}, operation: {} ", entry.getUserName(),
                    entry.getClientId(), entry.getOperation());
            LOG.debug("<== AtlasAuditService.add({})");
        }
    }

    public AtlasAuditEntry get(AtlasAuditEntry entry) throws AtlasBaseException {
        if(entry.getGuid() == null) {
            throw new AtlasBaseException("Entity does not have GUID set. load cannot proceed.");
        }
        return dataAccess.load(entry);
    }

    public List<AtlasAuditEntry> get(AuditSearchParameters auditSearchParameters) throws AtlasBaseException {
        if (auditSearchParameters == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Audit Search Parameters not specified");
        }

        SearchParameters searchParameters = getSearchParameters(auditSearchParameters);

        searchParameters.setAttributes(getAuditEntityAttributes());

        AtlasSearchResult result = discoveryService.searchWithParameters(searchParameters);
        return toAtlasAuditEntries(result);
    }

    public AtlasAuditEntry toAtlasAuditEntry(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        AtlasAuditEntry ret = null;

        if(entityWithExtInfo != null && entityWithExtInfo.getEntity() != null) {
            ret = AtlasAuditEntryDTO.from(entityWithExtInfo.getEntity().getGuid(),
                    entityWithExtInfo.getEntity().getAttributes());
        }

        return ret;
    }

    private Set<String> getAuditEntityAttributes() {
        return AtlasAuditEntryDTO.getAttributes();
    }

    private List<AtlasAuditEntry> toAtlasAuditEntries(AtlasSearchResult result) {
        List<AtlasAuditEntry> ret = new ArrayList<>();

        if(CollectionUtils.isNotEmpty(result.getEntities())) {
            for (AtlasEntityHeader entityHeader : result.getEntities()) {
                AtlasAuditEntry entry = AtlasAuditEntryDTO.from(entityHeader.getGuid(),
                        entityHeader.getAttributes());
                if (entry == null) {
                    continue;
                }

                ret.add(entry);
            }
        }

        return ret;
    }

    private SearchParameters getSearchParameters(AuditSearchParameters auditSearchParameters) throws AtlasBaseException {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.setTypeName(ENTITY_TYPE_AUDIT_ENTRY);

        SearchParameters.FilterCriteria validFilter = getNonEmptyFilter(auditSearchParameters.getAuditFilters());
        searchParameters.setEntityFilters(validFilter);

        searchParameters.setLimit(auditSearchParameters.getLimit());
        searchParameters.setOffset(auditSearchParameters.getOffset());

        String sortBy = auditSearchParameters.getSortBy();
        validateSortByParameter(sortBy);

        searchParameters.setSortBy(auditSearchParameters.getSortBy());
        searchParameters.setSortOrder(auditSearchParameters.getSortOrder());

        return searchParameters;
    }

    private void validateSortByParameter(String sortBy) throws AtlasBaseException{
        if (StringUtils.isNotEmpty(sortBy) && !AtlasAuditEntryDTO.getAttributes().contains(sortBy)) {
            throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_ATTRIBUTE, sortBy, "Atlas Audit Entry");
        }
    }

    private SearchParameters.FilterCriteria getNonEmptyFilter(SearchParameters.FilterCriteria auditFilter) throws AtlasBaseException {
        SearchParameters.FilterCriteria outCriteria = new SearchParameters.FilterCriteria();
        outCriteria.setCriterion(new ArrayList<>());

        if(auditFilter != null) {
            outCriteria.setCondition(auditFilter.getCondition());
            List<SearchParameters.FilterCriteria> givenFilterCriterion = auditFilter.getCriterion();

            for (SearchParameters.FilterCriteria each : givenFilterCriterion) {
                if (StringUtils.isNotEmpty(each.getAttributeName()) && !AtlasAuditEntryDTO.getAttributes().contains(each.getAttributeName())) {
                    throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_ATTRIBUTE, each.getAttributeName(), "Atlas Audit Entry");
                }

                addParameterIfValueNotEmpty(outCriteria, each.getAttributeName(), each.getOperator(), each.getAttributeValue());
            }
        }

        return outCriteria;
    }

    private void addParameterIfValueNotEmpty(SearchParameters.FilterCriteria criteria, String attributeName,
                                             SearchParameters.Operator operator, String value) {
        if(StringUtils.isNotEmpty(value)) {
            SearchParameters.FilterCriteria filterCriteria = new SearchParameters.FilterCriteria();
            filterCriteria.setAttributeName(attributeName);
            filterCriteria.setAttributeValue(value);
            filterCriteria.setOperator(operator);

            criteria.getCriterion().add(filterCriteria);
        }
    }

}