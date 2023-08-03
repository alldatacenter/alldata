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

package org.apache.atlas.hive.hook;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.hive.hook.HiveMetastoreHookImpl.HiveMetastoreHook;
import org.apache.atlas.hive.hook.HiveHook.PreprocessAction;
import org.apache.atlas.hive.hook.HiveHook.HiveHookObjectNamesCache;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.hive.metastore.IHMSHandler;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.events.*;
import org.apache.hadoop.hive.ql.hooks.*;
import org.apache.hadoop.hive.ql.metadata.Hive;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.hive.ql.session.SessionState;

import java.util.*;

import static org.apache.atlas.hive.bridge.HiveMetaStoreBridge.getDatabaseName;
import static org.apache.atlas.hive.hook.events.BaseHiveEvent.toTable;


public class AtlasHiveHookContext {
    public static final char   QNAME_SEP_METADATA_NAMESPACE = '@';
    public static final char   QNAME_SEP_ENTITY_NAME        = '.';
    public static final char   QNAME_SEP_PROCESS            = ':';
    public static final String TEMP_TABLE_PREFIX            = "_temp-";
    public static final String CREATE_OPERATION             = "CREATE";
    public static final String ALTER_OPERATION              = "ALTER";

    private final HiveHook                 hook;
    private final HiveOperation            hiveOperation;
    private final HookContext              hiveContext;
    private final Hive                     hive;
    private final Map<String, AtlasEntity> qNameEntityMap = new HashMap<>();
    private final HiveHookObjectNamesCache knownObjects;
    private final HiveMetastoreHook        metastoreHook;
    private final ListenerEvent            metastoreEvent;
    private final IHMSHandler              metastoreHandler;

    private boolean isSkippedInputEntity;
    private boolean isSkippedOutputEntity;
    private boolean skipTempTables;

    public AtlasHiveHookContext(HiveHook hook, HiveOperation hiveOperation, HookContext hiveContext,
                                HiveHookObjectNamesCache knownObjects, boolean skipTempTables) throws Exception {
        this(hook, hiveOperation, hiveContext, knownObjects, null, null, skipTempTables);
    }

    public AtlasHiveHookContext(HiveHook hook, HiveOperation hiveOperation, HiveHookObjectNamesCache knownObjects,
                                HiveMetastoreHook metastoreHook, ListenerEvent listenerEvent, boolean skipTempTables) throws Exception {
        this(hook, hiveOperation, null, knownObjects, metastoreHook, listenerEvent, skipTempTables);
    }

    public AtlasHiveHookContext(HiveHook hook, HiveOperation hiveOperation, HookContext hiveContext, HiveHookObjectNamesCache knownObjects,
                                HiveMetastoreHook metastoreHook, ListenerEvent listenerEvent, boolean skipTempTables) throws Exception {
        this.hook             = hook;
        this.hiveOperation    = hiveOperation;
        this.hiveContext      = hiveContext;
        this.hive             = hiveContext != null ? Hive.get(hiveContext.getConf()) : null;
        this.knownObjects     = knownObjects;
        this.metastoreHook    = metastoreHook;
        this.metastoreEvent   = listenerEvent;
        this.metastoreHandler = (listenerEvent != null) ? metastoreEvent.getIHMSHandler() : null;
        this.skipTempTables   = skipTempTables;

        init();
    }

    public boolean isMetastoreHook() {
        return metastoreHook != null;
    }

    public ListenerEvent getMetastoreEvent() {
        return metastoreEvent;
    }

    public IHMSHandler getMetastoreHandler() {
        return metastoreHandler;
    }

    public Set<ReadEntity> getInputs() {
        return hiveContext != null ? hiveContext.getInputs() : Collections.emptySet();
    }

    public Set<WriteEntity> getOutputs() {
        return hiveContext != null ? hiveContext.getOutputs() : Collections.emptySet();
    }

    public boolean isSkippedInputEntity() {
        return isSkippedInputEntity;
    }

    public boolean isSkippedOutputEntity() {
        return isSkippedOutputEntity;
    }

    public void registerSkippedEntity(Entity entity) {
        if (entity instanceof ReadEntity) {
            registerSkippedInputEntity();
        } else if (entity instanceof WriteEntity) {
            registerSkippedOutputEntity();
        }
    }

    public void registerSkippedInputEntity() {
        if (!isSkippedInputEntity) {
            isSkippedInputEntity = true;
        }
    }

    public void registerSkippedOutputEntity() {
        if (!isSkippedOutputEntity) {
            isSkippedOutputEntity = true;
        }
    }

    public boolean isSkipTempTables() {
        return skipTempTables;
    }

    public LineageInfo getLineageInfo() {
        return hiveContext != null ? hiveContext.getLinfo() : null;
    }

    public HookContext getHiveContext() {
        return hiveContext;
    }

    public Hive getHive() {
        return hive;
    }

    public HiveOperation getHiveOperation() {
        return hiveOperation;
    }

    public void putEntity(String qualifiedName, AtlasEntity entity) {
        qNameEntityMap.put(qualifiedName, entity);
    }

    public AtlasEntity getEntity(String qualifiedName) {
        return qNameEntityMap.get(qualifiedName);
    }

    public Collection<AtlasEntity> getEntities() { return qNameEntityMap.values(); }

    public Map<String, AtlasEntity> getQNameToEntityMap() { return qNameEntityMap; }

    public String getMetadataNamespace() {
        return hook.getMetadataNamespace();
    }

    public String getHostName() { return hook.getHostName(); }

    public boolean isConvertHdfsPathToLowerCase() {
        return hook.isConvertHdfsPathToLowerCase();
    }

    public String getAwsS3AtlasModelVersion() {
        return hook.getAwsS3AtlasModelVersion();
    }

    public boolean getSkipHiveColumnLineageHive20633() {
        return hook.getSkipHiveColumnLineageHive20633();
    }

    public int getSkipHiveColumnLineageHive20633InputsThreshold() {
        return hook.getSkipHiveColumnLineageHive20633InputsThreshold();
    }

    public PreprocessAction getPreprocessActionForHiveTable(String qualifiedName) {
        return hook.getPreprocessActionForHiveTable(qualifiedName);
    }

    public List getIgnoreDummyDatabaseName() {
        return hook.getIgnoreDummyDatabaseName();
    }

    public  List getIgnoreDummyTableName() {
        return hook.getIgnoreDummyTableName();
    }

    public  String getIgnoreValuesTmpTableNamePrefix() {
        return hook.getIgnoreValuesTmpTableNamePrefix();
    }

    public String getQualifiedName(Database db) {
        return getDatabaseName(db) + QNAME_SEP_METADATA_NAMESPACE + getMetadataNamespace();
    }

    public String getQualifiedName(Table table) {
        String tableName = table.getTableName();

        if (table.isTemporary()) {
            if (SessionState.get() != null && SessionState.get().getSessionId() != null) {
                tableName = tableName + TEMP_TABLE_PREFIX + SessionState.get().getSessionId();
            } else {
                tableName = tableName + TEMP_TABLE_PREFIX + RandomStringUtils.random(10);
            }
        }

        return (table.getDbName() + QNAME_SEP_ENTITY_NAME + tableName + QNAME_SEP_METADATA_NAMESPACE).toLowerCase() + getMetadataNamespace();
    }

    public boolean isKnownDatabase(String dbQualifiedName) {
        return knownObjects != null && dbQualifiedName != null ? knownObjects.isKnownDatabase(dbQualifiedName) : false;
    }

    public boolean isKnownTable(String tblQualifiedName) {
        return knownObjects != null && tblQualifiedName != null ? knownObjects.isKnownTable(tblQualifiedName) : false;
    }

    public void addToKnownEntities(Collection<AtlasEntity> entities) {
        if (knownObjects != null && entities != null) {
            knownObjects.addToKnownEntities(entities);
        }
    }

    public void removeFromKnownDatabase(String dbQualifiedName) {
        if (knownObjects != null && dbQualifiedName != null) {
            knownObjects.removeFromKnownDatabase(dbQualifiedName);
        }
    }

    public void removeFromKnownTable(String tblQualifiedName) {
        if (knownObjects != null && tblQualifiedName != null) {
            knownObjects.removeFromKnownTable(tblQualifiedName);
        }
    }

    public boolean isHiveProcessPopulateDeprecatedAttributes() {
        return hook.isHiveProcessPopulateDeprecatedAttributes();
    }

    private void init() {
        if (hiveOperation == null) {
            return;
        }

        String operation = hiveOperation.getOperationName();

        if (knownObjects == null || !isCreateAlterOperation(operation)) {
            return;
        }

        List<Database> databases = new ArrayList<>();
        List<Table>    tables    = new ArrayList<>();

        if (isMetastoreHook()) {
            switch (hiveOperation) {
                case CREATEDATABASE:
                    databases.add(((CreateDatabaseEvent) metastoreEvent).getDatabase());
                    break;
                case ALTERDATABASE:
                    databases.add(((AlterDatabaseEvent) metastoreEvent).getOldDatabase());
                    databases.add(((AlterDatabaseEvent) metastoreEvent).getNewDatabase());
                    break;
                case CREATETABLE:
                    tables.add(toTable(((CreateTableEvent) metastoreEvent).getTable()));
                    break;
                case ALTERTABLE_PROPERTIES:
                case ALTERTABLE_RENAME:
                case ALTERTABLE_RENAMECOL:
                    tables.add(toTable(((AlterTableEvent) metastoreEvent).getOldTable()));
                    tables.add(toTable(((AlterTableEvent) metastoreEvent).getNewTable()));
                    break;
            }
        } else {
            if (getOutputs() != null) {
                for (WriteEntity output : hiveContext.getOutputs()) {
                    switch (output.getType()) {
                        case DATABASE:
                            databases.add(output.getDatabase());
                            break;
                        case TABLE:
                            tables.add(output.getTable());
                            break;
                    }
                }
            }
        }

        for (Database database : databases) {
            knownObjects.removeFromKnownDatabase(getQualifiedName(database));
        }

        for (Table table : tables) {
            knownObjects.removeFromKnownTable(getQualifiedName(table));
        }
    }

    private static boolean isCreateAlterOperation(String operationName) {
        return operationName != null && operationName.startsWith(CREATE_OPERATION) || operationName.startsWith(ALTER_OPERATION);
    }
}