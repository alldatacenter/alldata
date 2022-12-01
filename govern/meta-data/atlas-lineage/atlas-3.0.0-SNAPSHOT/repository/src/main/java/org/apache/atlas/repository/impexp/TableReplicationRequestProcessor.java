/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.impexp;

import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.discovery.AtlasDiscoveryService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;

@Component
public class TableReplicationRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableReplicationRequestProcessor.class);

    private static final String QUERY_DB_NAME_EQUALS = "qualifiedName startsWith '%s'";
    private static final String ATTR_NAME_KEY = "name";
    private static final String TYPE_HIVE_TABLE = "hive_table";
    private static final String ATTR_QUALIFIED_NAME_KEY = "qualifiedName";
    private static final String REPLICATED_TAG_NAME = "%s_replicated";

    private long startTstamp;
    private long endTstamp;
    private AuditsWriter auditsWriter;
    private AtlasEntityStore entityStore;
    private AtlasTypeRegistry typeRegistry;
    private AtlasDiscoveryService discoveryService;

    @Inject
    public TableReplicationRequestProcessor(AuditsWriter auditsWriter, AtlasEntityStore entityStore,
                                            AtlasDiscoveryService atlasDiscoveryService, AtlasTypeRegistry typeRegistry) {
        this.auditsWriter = auditsWriter;
        this.entityStore = entityStore;
        this.typeRegistry = typeRegistry;
        this.discoveryService = atlasDiscoveryService;
    }

    public void process(AtlasExportRequest exportRequest, AtlasImportRequest importRequest) throws AtlasBaseException {
        startTstamp = System.currentTimeMillis();
        LOG.info("process: deleting entities with type hive_table which are not imported.");
        String sourceCluster = importRequest.getOptionKeyReplicatedFrom();

        List<String> qualifiedNames = getQualifiedNamesFromRequest(exportRequest);

        List<String> safeGUIDs = getEntitiesFromQualifiedNames(qualifiedNames);

        String dbName = getDbName(safeGUIDs.get(0));

        Set<String> guidsToDelete = getGuidsToDelete(dbName, safeGUIDs, sourceCluster);

        deleteTables(sourceCluster, guidsToDelete);
    }

    private List<String> getQualifiedNamesFromRequest(AtlasExportRequest exportRequest) {
        List<String> qualifiedNames = new ArrayList<>();

        for (AtlasObjectId objectId : exportRequest.getItemsToExport()) {
            qualifiedNames.add(objectId.getUniqueAttributes().get(ATTR_QUALIFIED_NAME_KEY).toString());
        }
        return qualifiedNames;
    }

    private List<String> getEntitiesFromQualifiedNames(List<String> qualifiedNames) throws AtlasBaseException {

        List<String> safeGUIDs = new ArrayList<>();
        for (String qualifiedName : qualifiedNames) {
            String guid = getGuidByUniqueAttributes(Collections.singletonMap(ATTR_QUALIFIED_NAME_KEY, qualifiedName));
            safeGUIDs.add(guid);
        }
        return safeGUIDs;
    }

    private String getGuidByUniqueAttributes(Map<String, Object> uniqueAttributes) throws AtlasBaseException {
        return entityStore.getGuidByUniqueAttributes(typeRegistry.getEntityTypeByName(TYPE_HIVE_TABLE), uniqueAttributes);
    }

    private String getDbName(String tableGuid) throws AtlasBaseException {
        String dbGuid = AuditsWriter.ReplKeyGuidFinder.get(typeRegistry, entityStore, tableGuid);
        return (String) entityStore.getById(dbGuid).getEntity().getAttribute(ATTR_NAME_KEY);
    }

    private Set<String> getGuidsToDelete(String dbName, List<String> excludeGUIDs, String sourceCluster) throws AtlasBaseException {

        SearchParameters parameters = getSearchParameters(dbName, sourceCluster);
        Set<String> unsafeGUIDs = new HashSet<>();

        final int max = 10000;
        int fetchedSize = 0;
        int i = 0;
        parameters.setLimit(max);

        while (fetchedSize == (max * i)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("i={}, fetchedSize={}, unsafeGUIDs.size()={}", i, fetchedSize, unsafeGUIDs.size());
            }

            int offset = max * i;
            parameters.setOffset(offset);

            AtlasSearchResult searchResult = discoveryService.searchWithParameters(parameters);

            if (CollectionUtils.isEmpty(searchResult.getEntities())) {
                break;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("getGuidsToDelete: {}", searchResult.getApproximateCount());
                }
            }

            String classificationName = String.format(REPLICATED_TAG_NAME, sourceCluster);
            for (AtlasEntityHeader entityHeader : searchResult.getEntities()) {
                if (!entityHeader.getClassificationNames().contains(classificationName)) {
                    continue;
                }

                String guid = entityHeader.getGuid();
                if (!excludeGUIDs.contains(guid)) {
                    unsafeGUIDs.add(guid);
                }
            }
            fetchedSize = searchResult.getEntities().size();
            i++;
        }
        return unsafeGUIDs;
    }

    private SearchParameters getSearchParameters(String dbName, String sourceCluster) {
        String query = String.format(QUERY_DB_NAME_EQUALS, dbName);

        SearchParameters parameters = new SearchParameters();
        parameters.setExcludeDeletedEntities(false);
        parameters.setTypeName(TYPE_HIVE_TABLE);
        parameters.setExcludeDeletedEntities(true);

        parameters.setAttributes(new HashSet<String>() {{ add(AtlasImportRequest.OPTION_KEY_REPLICATED_FROM); }});
        parameters.setQuery(query);

        return parameters;
    }

    private void deleteTables(String sourceCluster, Set<String> guidsToDelete) throws AtlasBaseException {
        if (!CollectionUtils.isEmpty(guidsToDelete)) {
            entityStore.deleteByIds(new ArrayList<>(guidsToDelete));

            endTstamp = System.currentTimeMillis();
            createAuditEntry(sourceCluster, guidsToDelete);
        }
    }

    private void createAuditEntry(String sourceCluster, Set<String> guidsToDelete) throws AtlasBaseException {
        auditsWriter.write(AtlasAuthorizationUtils.getCurrentUserName(), sourceCluster, startTstamp, endTstamp, guidsToDelete);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted entities => {}", guidsToDelete);
        }
    }
}
