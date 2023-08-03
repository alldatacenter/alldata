/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.sqoop.hook;


import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.hook.AtlasHook;
import org.apache.atlas.hook.AtlasHookException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityCreateRequestV2;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.sqoop.model.SqoopDataTypes;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AtlasConfigurationUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.sqoop.SqoopJobDataPublisher;
import org.apache.sqoop.util.ImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.List;
import java.util.Date;

import static org.apache.atlas.repository.Constants.SQOOP_SOURCE;

/**
 * AtlasHook sends lineage information to the AtlasSever.
 */
public class SqoopHook extends SqoopJobDataPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(SqoopHook.class);

    public static final String CLUSTER_NAME_KEY           = "atlas.cluster.name";
    public static final String ATLAS_METADATA_NAMESPACE   = "atlas.metadata.namespace";
    public static final String DEFAULT_CLUSTER_NAME       = "primary";

    public static final String USER           = "userName";
    public static final String DB_STORE_TYPE  = "dbStoreType";
    public static final String DB_STORE_USAGE = "storeUse";
    public static final String SOURCE         = "source";
    public static final String DESCRIPTION    = "description";
    public static final String STORE_URI      = "storeUri";
    public static final String OPERATION      = "operation";
    public static final String START_TIME     = "startTime";
    public static final String END_TIME       = "endTime";
    public static final String CMD_LINE_OPTS  = "commandlineOpts";
    public static final String INPUTS         = "inputs";
    public static final String OUTPUTS        = "outputs";
    public static final String ATTRIBUTE_DB   = "db";

    public static final String RELATIONSHIP_HIVE_TABLE_DB = "hive_table_db";
    public static final String RELATIONSHIP_DATASET_PROCESS_INPUTS = "dataset_process_inputs";
    public static final String RELATIONSHIP_PROCESS_DATASET_OUTPUTS = "process_dataset_outputs";

    private static final AtlasHookImpl atlasHook;

    static {
        org.apache.hadoop.conf.Configuration.addDefaultResource("sqoop-site.xml");

        atlasHook = new AtlasHookImpl();
    }

    @Override
    public void publish(SqoopJobDataPublisher.Data data) throws AtlasHookException {
        try {
            Configuration atlasProperties   = ApplicationProperties.get();
            String        metadataNamespace =
                    AtlasConfigurationUtil.getRecentString(atlasProperties, ATLAS_METADATA_NAMESPACE, getClusterName(atlasProperties));

            AtlasEntity entDbStore   = toSqoopDBStoreEntity(data);
            AtlasEntity entHiveDb    = toHiveDatabaseEntity(metadataNamespace, data.getHiveDB());
            AtlasEntity entHiveTable = data.getHiveTable() != null ? toHiveTableEntity(entHiveDb, data.getHiveTable()) : null;
            AtlasEntity entProcess   = toSqoopProcessEntity(entDbStore, entHiveDb, entHiveTable, data, metadataNamespace);


            AtlasEntitiesWithExtInfo entities = new AtlasEntitiesWithExtInfo(entProcess);

            entities.addReferredEntity(entDbStore);
            entities.addReferredEntity(entHiveDb);
            if (entHiveTable != null) {
                entities.addReferredEntity(entHiveTable);
            }

            HookNotification message = new EntityCreateRequestV2(AtlasHook.getUser(), entities);

            atlasHook.sendNotification(message);
        } catch(Exception e) {
            LOG.error("SqoopHook.publish() failed", e);

            throw new AtlasHookException("SqoopHook.publish() failed.", e);
        }
    }

    private String getClusterName(Configuration config) {
        return config.getString(CLUSTER_NAME_KEY, DEFAULT_CLUSTER_NAME);
    }

    private AtlasEntity toHiveDatabaseEntity(String metadataNamespace, String dbName) {
        AtlasEntity entHiveDb     = new AtlasEntity(HiveDataTypes.HIVE_DB.getName());
        String      qualifiedName = HiveMetaStoreBridge.getDBQualifiedName(metadataNamespace, dbName);

        entHiveDb.setAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, metadataNamespace);
        entHiveDb.setAttribute(AtlasClient.NAME, dbName);
        entHiveDb.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName);

        return entHiveDb;
    }

    private AtlasEntity toHiveTableEntity(AtlasEntity entHiveDb, String tableName) {
        AtlasEntity entHiveTable  = new AtlasEntity(HiveDataTypes.HIVE_TABLE.getName());
        String      qualifiedName = HiveMetaStoreBridge.getTableQualifiedName((String)entHiveDb.getAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE), (String)entHiveDb.getAttribute(AtlasClient.NAME), tableName);

        entHiveTable.setAttribute(AtlasClient.NAME, tableName.toLowerCase());
        entHiveTable.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName);
        entHiveTable.setRelationshipAttribute(ATTRIBUTE_DB, AtlasTypeUtil.getAtlasRelatedObjectId(entHiveDb, RELATIONSHIP_HIVE_TABLE_DB));

        return entHiveTable;
    }

    private AtlasEntity toSqoopDBStoreEntity(SqoopJobDataPublisher.Data data) throws ImportException {
        String table = data.getStoreTable();
        String query = data.getStoreQuery();

        if (StringUtils.isBlank(table) && StringUtils.isBlank(query)) {
            throw new ImportException("Both table and query cannot be empty for DBStoreInstance");
        }

        String usage  = table != null ? "TABLE" : "QUERY";
        String source = table != null ? table : query;
        String name   = getSqoopDBStoreName(data);

        AtlasEntity entDbStore = new AtlasEntity(SqoopDataTypes.SQOOP_DBDATASTORE.getName());

        entDbStore.setAttribute(AtlasClient.NAME, name);
        entDbStore.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, name);
        entDbStore.setAttribute(SqoopHook.DB_STORE_TYPE, data.getStoreType());
        entDbStore.setAttribute(SqoopHook.DB_STORE_USAGE, usage);
        entDbStore.setAttribute(SqoopHook.STORE_URI, data.getUrl());
        entDbStore.setAttribute(SqoopHook.SOURCE, source);
        entDbStore.setAttribute(SqoopHook.DESCRIPTION, "");
        entDbStore.setAttribute(AtlasClient.OWNER, data.getUser());

        return entDbStore;
    }

    private AtlasEntity toSqoopProcessEntity(AtlasEntity entDbStore, AtlasEntity entHiveDb, AtlasEntity entHiveTable,
                                             SqoopJobDataPublisher.Data data, String metadataNamespace) {
        AtlasEntity         entProcess       = new AtlasEntity(SqoopDataTypes.SQOOP_PROCESS.getName());
        String              sqoopProcessName = getSqoopProcessName(data, metadataNamespace);
        Map<String, String> sqoopOptionsMap  = new HashMap<>();
        Properties          options          = data.getOptions();

        for (Object k : options.keySet()) {
            sqoopOptionsMap.put((String)k, (String) options.get(k));
        }

        entProcess.setAttribute(AtlasClient.NAME, sqoopProcessName);
        entProcess.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, sqoopProcessName);
        entProcess.setAttribute(SqoopHook.OPERATION, data.getOperation());

        List<AtlasObjectId> sqoopObjects = Collections.singletonList(AtlasTypeUtil.getAtlasObjectId(entDbStore));
        List<AtlasObjectId> hiveObjects  = Collections.singletonList(AtlasTypeUtil.getAtlasObjectId(entHiveTable != null ? entHiveTable : entHiveDb));

        if (isImportOperation(data)) {
            entProcess.setRelationshipAttribute(SqoopHook.INPUTS, AtlasTypeUtil.getAtlasRelatedObjectIdList(sqoopObjects, RELATIONSHIP_DATASET_PROCESS_INPUTS));
            entProcess.setRelationshipAttribute(SqoopHook.OUTPUTS, AtlasTypeUtil.getAtlasRelatedObjectIdList(hiveObjects, RELATIONSHIP_PROCESS_DATASET_OUTPUTS));
        } else {
            entProcess.setRelationshipAttribute(SqoopHook.INPUTS, AtlasTypeUtil.getAtlasRelatedObjectIdList(hiveObjects, RELATIONSHIP_DATASET_PROCESS_INPUTS));
            entProcess.setRelationshipAttribute(SqoopHook.OUTPUTS, AtlasTypeUtil.getAtlasRelatedObjectIdList(sqoopObjects, RELATIONSHIP_PROCESS_DATASET_OUTPUTS));
        }

        entProcess.setAttribute(SqoopHook.USER, data.getUser());
        entProcess.setAttribute(SqoopHook.START_TIME, new Date(data.getStartTime()));
        entProcess.setAttribute(SqoopHook.END_TIME, new Date(data.getEndTime()));
        entProcess.setAttribute(SqoopHook.CMD_LINE_OPTS, sqoopOptionsMap);

        return entProcess;
    }

    private boolean isImportOperation(SqoopJobDataPublisher.Data data) {
        return data.getOperation().toLowerCase().equals("import");
    }

    static String getSqoopProcessName(Data data, String metadataNamespace) {
        StringBuilder name = new StringBuilder(String.format("sqoop %s --connect %s", data.getOperation(), data.getUrl()));

        if (StringUtils.isNotEmpty(data.getHiveTable())) {
            name.append(" --table ").append(data.getStoreTable());
        } else {
            name.append(" --database ").append(data.getHiveDB());
        }

        if (StringUtils.isNotEmpty(data.getStoreQuery())) {
            name.append(" --query ").append(data.getStoreQuery());
        }

        if (data.getHiveTable() != null) {
            name.append(String.format(" --hive-%s --hive-database %s --hive-table %s --hive-cluster %s", data.getOperation(), data.getHiveDB().toLowerCase(), data.getHiveTable().toLowerCase(), metadataNamespace));
        } else {
            name.append(String.format("--hive-%s --hive-database %s --hive-cluster %s", data.getOperation(), data.getHiveDB(), metadataNamespace));
        }

        return name.toString();
    }

    static String getSqoopDBStoreName(SqoopJobDataPublisher.Data data)  {
        StringBuilder name = new StringBuilder(String.format("%s --url %s", data.getStoreType(), data.getUrl()));

        if (StringUtils.isNotEmpty(data.getHiveTable())) {
            name.append(" --table ").append(data.getStoreTable());
        } else {
            name.append(" --database ").append(data.getHiveDB());
        }

        if (StringUtils.isNotEmpty(data.getStoreQuery())) {
            name.append(" --query ").append(data.getStoreQuery());
        }

        return name.toString();
    }

    private static class AtlasHookImpl extends AtlasHook {

        public String getMessageSource() {
            return SQOOP_SOURCE;
        }

        public void sendNotification(HookNotification notification) {
            super.notifyEntities(Collections.singletonList(notification), null);
        }
    }
}
