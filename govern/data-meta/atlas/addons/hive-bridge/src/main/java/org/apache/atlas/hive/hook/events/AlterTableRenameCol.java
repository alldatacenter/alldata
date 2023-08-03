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

package org.apache.atlas.hive.hook.events;

import org.apache.atlas.hive.hook.AtlasHiveHookContext;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityPartialUpdateRequestV2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.events.AlterTableEvent;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AlterTableRenameCol extends AlterTable {
    private static final Logger      LOG = LoggerFactory.getLogger(AlterTableRenameCol.class);
    private        final FieldSchema columnOld;
    private        final FieldSchema columnNew;

    public AlterTableRenameCol(AtlasHiveHookContext context) {
        this(null, null, context);
    }

    public AlterTableRenameCol(FieldSchema columnOld, FieldSchema columnNew, AtlasHiveHookContext context) {
        super(context);

        this.columnOld = columnOld;
        this.columnNew = columnNew;
    }

    @Override
    public List<HookNotification> getNotificationMessages() throws Exception {
        return context.isMetastoreHook() ? getHiveMetastoreMessages() : getHiveMessages();
    }

    public List<HookNotification> getHiveMetastoreMessages() throws Exception {
        List<HookNotification> baseMsgs = super.getNotificationMessages();
        List<HookNotification> ret      = new ArrayList<>(baseMsgs);
        AlterTableEvent        tblEvent = (AlterTableEvent) context.getMetastoreEvent();
        Table                  oldTable = toTable(tblEvent.getOldTable());
        Table                  newTable = toTable(tblEvent.getNewTable());

        processColumns(oldTable, newTable, ret);

        return ret;
    }

    public List<HookNotification> getHiveMessages() throws Exception {
        List<HookNotification> baseMsgs = super.getNotificationMessages();

        if (CollectionUtils.isEmpty(getInputs())) {
            LOG.error("AlterTableRenameCol: old-table not found in inputs list");

            return null;
        }

        if (CollectionUtils.isEmpty(getOutputs())) {
            LOG.error("AlterTableRenameCol: new-table not found in outputs list");

            return null;
        }

        if (CollectionUtils.isEmpty(baseMsgs)) {
            LOG.debug("Skipped processing of column-rename (on a temporary table?)");

            return null;
        }

        List<HookNotification> ret      = new ArrayList<>(baseMsgs);
        Table                  oldTable = getInputs().iterator().next().getTable();
        Table                  newTable = getOutputs().iterator().next().getTable();

        if (newTable != null) {
            newTable = getHive().getTable(newTable.getDbName(), newTable.getTableName());
        }

        processColumns(oldTable, newTable, ret);

        return ret;
    }

    private void processColumns(Table oldTable, Table newTable, List<HookNotification> ret) {
        FieldSchema changedColumnOld = (columnOld == null) ? findRenamedColumn(oldTable, newTable) : columnOld;
        FieldSchema changedColumnNew = (columnNew == null) ? findRenamedColumn(newTable, oldTable) : columnNew;

        if (changedColumnOld != null && changedColumnNew != null) {
            AtlasObjectId oldColumnId = new AtlasObjectId(HIVE_TYPE_COLUMN, ATTRIBUTE_QUALIFIED_NAME, getQualifiedName(oldTable, changedColumnOld));
            AtlasEntity newColumn   = new AtlasEntity(HIVE_TYPE_COLUMN);

            newColumn.setAttribute(ATTRIBUTE_NAME, changedColumnNew.getName());
            newColumn.setAttribute(ATTRIBUTE_QUALIFIED_NAME, getQualifiedName(newTable, changedColumnNew));

            ret.add(0, new EntityPartialUpdateRequestV2(getUserName(), oldColumnId, new AtlasEntityWithExtInfo(newColumn)));
        } else {
            LOG.error("AlterTableRenameCol: no renamed column detected");
        }
    }

    public static FieldSchema findRenamedColumn(Table inputTable, Table outputTable) {
        FieldSchema       ret           = null;
        List<FieldSchema> inputColumns  = inputTable.getCols();
        List<FieldSchema> outputColumns = outputTable.getCols();

        for (FieldSchema inputColumn : inputColumns) {
            if (!outputColumns.contains(inputColumn)) {
                ret = inputColumn;

                break;
            }
        }

        return ret;
    }
}