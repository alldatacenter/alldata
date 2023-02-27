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

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.AtlasException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class AuditsWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AuditsWriter.class);
    private static final String CLUSTER_NAME_DEFAULT = "default";
    private static final String DC_SERVER_NAME_SEPARATOR = "$";

    private AtlasTypeRegistry typeRegistry;
    private AtlasEntityStore entityStore;
    private AtlasServerService atlasServerService;
    private ExportImportAuditService auditService;

    private ExportAudits auditForExport = new ExportAudits();
    private ImportAudits auditForImport = new ImportAudits();

    @Inject
    public AuditsWriter(AtlasTypeRegistry typeRegistry, AtlasEntityStore entityStore, AtlasServerService atlasServerService, ExportImportAuditService auditService) {
        this.typeRegistry = typeRegistry;
        this.entityStore = entityStore;
        this.atlasServerService = atlasServerService;
        this.auditService = auditService;
    }

    public AtlasServerService getAtlasServerService() {
        return atlasServerService;
    }

    public void write(String userName, AtlasExportResult result,
                      long startTime, long endTime,
                      List<String> entityCreationOrder) throws AtlasBaseException {
        auditForExport.add(userName, result, startTime, endTime, entityCreationOrder);
    }

    public void write(String userName, AtlasImportResult result,
                      long startTime, long endTime,
                      List<String> entityCreationOrder) throws AtlasBaseException {
        auditForImport.add(userName, result, startTime, endTime, entityCreationOrder);
    }

    public void write(String userName, String sourceCluster,
                      long startTime, long endTime,
                      Set<String> entityCreationOrder) throws AtlasBaseException {
        auditForImport.add(userName, sourceCluster, startTime, endTime, entityCreationOrder);
    }

    private void updateReplicationAttribute(boolean isReplicationSet,
                                            String serverName, String serverFullName,
                                            List<String> exportedGuids,
                                            String attrNameReplicated,
                                            long lastModifiedTimestamp) throws AtlasBaseException {
        if (!isReplicationSet || CollectionUtils.isEmpty(exportedGuids)) {
            return;
        }

        String candidateGuid = exportedGuids.get(0);
        String replGuidKey = ReplKeyGuidFinder.get(typeRegistry, entityStore, candidateGuid);
        AtlasServer server = saveServer(serverName, serverFullName, replGuidKey, lastModifiedTimestamp);
        atlasServerService.updateEntitiesWithServer(server, exportedGuids, attrNameReplicated);
    }

    private AtlasServer saveServer(String clusterName, String serverFullName,
                                   String entityGuid,
                                   long lastModifiedTimestamp) throws AtlasBaseException {

        AtlasServer server = atlasServerService.getCreateAtlasServer(clusterName, serverFullName);
        server.setAdditionalInfoRepl(entityGuid, lastModifiedTimestamp);
        if (LOG.isDebugEnabled()) {
            LOG.debug("saveServer: {}", server);
        }

        return atlasServerService.save(server);
    }

    public static String getCurrentClusterName() {
        String ret = StringUtils.EMPTY;
        try {
            ret = ApplicationProperties.get().getString(AtlasConstants.METADATA_NAMESPACE_KEY, StringUtils.EMPTY);
            if (StringUtils.isEmpty(ret)) {
                ret = ApplicationProperties.get().getString(AtlasConstants.CLUSTER_NAME_KEY, CLUSTER_NAME_DEFAULT);
            }
        } catch (AtlasException e) {
            LOG.error("getCurrentClusterName", e);
        }

        return ret;
    }

    static String getServerNameFromFullName(String fullName) {
        if (StringUtils.isEmpty(fullName) || !fullName.contains(DC_SERVER_NAME_SEPARATOR)) {
            return fullName;
        }

        String[] splits = StringUtils.split(fullName, DC_SERVER_NAME_SEPARATOR);
        if (splits == null || splits.length < 1) {
            return "";
        } else if (splits.length >= 2) {
            return splits[1];
        } else {
            return splits[0];
        }
    }

    private void saveCurrentServer() throws AtlasBaseException {
        atlasServerService.getCreateAtlasServer(getCurrentClusterName(), getCurrentClusterName());
    }

    static class ReplKeyGuidFinder {
        private static final String ENTITY_TYPE_HIVE_DB = "hive_db";
        private static final String ENTITY_TYPE_HIVE_TABLE = "hive_table";
        private static final String ENTITY_TYPE_HIVE_COLUMN = "hive_column";

        public static String get(AtlasTypeRegistry typeRegistry, AtlasEntityStore entityStore, String candidateGuid) {
            String guid = null;
            try {
                guid = getParentEntityGuid(typeRegistry, entityStore, candidateGuid);
            } catch (AtlasBaseException e) {
                LOG.error("Error fetching parent guid for child entity: {}", candidateGuid);
            }

            if (StringUtils.isEmpty(guid)) {
                guid = candidateGuid;
            }

            return guid;
        }

        private static String getParentEntityGuid(AtlasTypeRegistry typeRegistry, AtlasEntityStore entityStore, String defaultGuid) throws AtlasBaseException {
            AtlasEntity.AtlasEntityWithExtInfo extInfo = entityStore.getById(defaultGuid);
            if (extInfo == null || extInfo.getEntity() == null) {
                return null;
            }

            String typeName = extInfo.getEntity().getTypeName();
            if (!typeName.equals(ENTITY_TYPE_HIVE_TABLE) && !typeName.equals(ENTITY_TYPE_HIVE_COLUMN)) {
                return null;
            }

            String hiveDBQualifiedName = extractHiveDBQualifiedName((String) extInfo.getEntity().getAttribute(EntityGraphRetriever.QUALIFIED_NAME));
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(ENTITY_TYPE_HIVE_DB);
            return entityStore.getGuidByUniqueAttributes(entityType, Collections.singletonMap(EntityGraphRetriever.QUALIFIED_NAME, hiveDBQualifiedName));
        }

        @VisibleForTesting
        static String extractHiveDBQualifiedName(String qualifiedName) {
            return String.format("%s@%s",
                    StringUtils.substringBefore(qualifiedName, "."),
                    StringUtils.substringAfter(qualifiedName, "@"));
        }
    }

    private class ExportAudits {
        private AtlasExportRequest request;
        private String targetServerName;
        private boolean replicationOptionState;
        private String targetServerFullName;

        public void add(String userName, AtlasExportResult result,
                        long startTime, long endTime,
                        List<String> entityGuids) throws AtlasBaseException {
            request = result.getRequest();
            replicationOptionState = request.isReplicationOptionSet();

            saveCurrentServer();

            targetServerFullName = request.getOptionKeyReplicatedTo();
            targetServerName = getServerNameFromFullName(targetServerFullName);
            auditService.add(userName, getCurrentClusterName(), targetServerName,
                    ExportImportAuditEntry.OPERATION_EXPORT,
                    AtlasType.toJson(result), startTime, endTime, !entityGuids.isEmpty());

            if (result.getOperationStatus() == AtlasExportResult.OperationStatus.FAIL) {
                return;
            }

            updateReplicationAttribute(replicationOptionState, targetServerName, targetServerFullName,
                    entityGuids, Constants.ATTR_NAME_REPLICATED_TO, result.getChangeMarker());
        }
    }

    private class ImportAudits {
        private AtlasImportRequest request;
        private boolean replicationOptionState;
        private String sourceServerName;
        private String sourceServerFullName;

        public void add(String userName, AtlasImportResult result,
                        long startTime, long endTime,
                        List<String> entityGuids) throws AtlasBaseException {
            request = result.getRequest();
            replicationOptionState = request.isReplicationOptionSet();

            saveCurrentServer();

            sourceServerFullName = request.getOptionKeyReplicatedFrom();
            sourceServerName = getServerNameFromFullName(sourceServerFullName);
            auditService.add(userName,
                    sourceServerName, getCurrentClusterName(),
                    ExportImportAuditEntry.OPERATION_IMPORT,
                    AtlasType.toJson(result), startTime, endTime, !entityGuids.isEmpty());

            if(result.getOperationStatus() == AtlasImportResult.OperationStatus.FAIL) {
                return;
            }

            updateReplicationAttribute(replicationOptionState, sourceServerName, sourceServerFullName, entityGuids,
                    Constants.ATTR_NAME_REPLICATED_FROM, result.getExportResult().getChangeMarker());
        }

        public void add(String userName, String sourceCluster, long startTime,
                        long endTime, Set<String> entityGuids) throws AtlasBaseException {

            sourceServerName = getServerNameFromFullName(sourceCluster);
            auditService.add(userName,
                    sourceServerName, getCurrentClusterName(),
                    ExportImportAuditEntry.OPERATION_IMPORT_DELETE_REPL,
                    AtlasType.toJson(entityGuids), startTime, endTime, !entityGuids.isEmpty());

        }
    }
}
