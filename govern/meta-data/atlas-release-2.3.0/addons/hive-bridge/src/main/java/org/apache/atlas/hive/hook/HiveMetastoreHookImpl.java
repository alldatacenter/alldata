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
package org.apache.atlas.hive.hook;

import org.apache.atlas.hive.hook.events.*;
import org.apache.atlas.hook.AtlasHook;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.MetaStoreEventListener;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.events.*;
import org.apache.hadoop.hive.metastore.utils.SecurityUtils;
import org.apache.hadoop.hive.ql.metadata.Table;
import org.apache.hadoop.hive.ql.plan.HiveOperation;
import org.apache.hadoop.hive.shims.Utils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.apache.atlas.hive.hook.events.AlterTableRenameCol.findRenamedColumn;
import static org.apache.atlas.hive.hook.events.BaseHiveEvent.toTable;
import static org.apache.atlas.repository.Constants.HMS_SOURCE;
import static org.apache.hadoop.hive.ql.plan.HiveOperation.*;

public class HiveMetastoreHookImpl extends MetaStoreEventListener {
    private static final Logger            LOG = LoggerFactory.getLogger(HiveMetastoreHookImpl.class);
    private        final HiveHook          hiveHook;
    private        final HiveMetastoreHook hook;

    public HiveMetastoreHookImpl(Configuration config) {
        super(config);

        this.hiveHook = new HiveHook(this.getClass().getSimpleName());
        this.hook     = new HiveMetastoreHook();
    }

    @Override
    public void onCreateDatabase(CreateDatabaseEvent dbEvent) {
        HiveOperationContext context = new HiveOperationContext(CREATEDATABASE, dbEvent);

        hook.handleEvent(context);
    }

    @Override
    public void onDropDatabase(DropDatabaseEvent dbEvent) {
        HiveOperationContext context = new HiveOperationContext(DROPDATABASE, dbEvent);

        hook.handleEvent(context);
    }

    @Override
    public void onAlterDatabase(AlterDatabaseEvent dbEvent) {
        HiveOperationContext context = new HiveOperationContext(ALTERDATABASE, dbEvent);

        hook.handleEvent(context);
    }

    @Override
    public void onCreateTable(CreateTableEvent tableEvent) {
        HiveOperationContext context = new HiveOperationContext(CREATETABLE, tableEvent);

        hook.handleEvent(context);
    }

    @Override
    public void onDropTable(DropTableEvent tableEvent) {
        HiveOperationContext context = new HiveOperationContext(DROPTABLE, tableEvent);

        hook.handleEvent(context);
    }

    @Override
    public void onAlterTable(AlterTableEvent tableEvent) {
        HiveOperationContext context = new HiveOperationContext(tableEvent);
        Table                oldTable = toTable(tableEvent.getOldTable());
        Table                newTable = toTable(tableEvent.getNewTable());

        if (isTableRename(oldTable, newTable)) {
            context.setOperation(ALTERTABLE_RENAME);
        } else if (isColumnRename(oldTable, newTable, context)) {
            context.setOperation(ALTERTABLE_RENAMECOL);
        } else if(isAlterTableProperty(tableEvent, "last_modified_time") ||
                isAlterTableProperty(tableEvent, "transient_lastDdlTime")) {
            context.setOperation(ALTERTABLE_PROPERTIES); // map other alter table operations to ALTERTABLE_PROPERTIES
        }

        hook.handleEvent(context);
    }

    public class HiveMetastoreHook extends AtlasHook {
        public HiveMetastoreHook() {
        }

        @Override
        public String getMessageSource() {
            return HMS_SOURCE;
        }

        public void handleEvent(HiveOperationContext operContext) {
            ListenerEvent listenerEvent = operContext.getEvent();

            if (!listenerEvent.getStatus()) {
                return;
            }

            try {
                HiveOperation        oper    = operContext.getOperation();
                AtlasHiveHookContext context = new AtlasHiveHookContext(hiveHook, oper, hiveHook.getKnownObjects(), this, listenerEvent, hiveHook.isSkipTempTables());
                BaseHiveEvent        event   = null;

                switch (oper) {
                    case CREATEDATABASE:
                        event = new CreateDatabase(context);
                        break;

                    case DROPDATABASE:
                        event = new DropDatabase(context);
                        break;

                    case ALTERDATABASE:
                        event = new AlterDatabase(context);
                        break;

                    case CREATETABLE:
                        event = new CreateTable(context);
                        break;

                    case DROPTABLE:
                        event = new DropTable(context);
                        break;

                    case ALTERTABLE_PROPERTIES:
                        event = new AlterTable(context);
                        break;

                    case ALTERTABLE_RENAME:
                        event = new AlterTableRename(context);
                        break;

                    case ALTERTABLE_RENAMECOL:
                        FieldSchema columnOld = operContext.getColumnOld();
                        FieldSchema columnNew = operContext.getColumnNew();

                        event = new AlterTableRenameCol(columnOld, columnNew, context);
                        break;

                    default:
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("HiveMetastoreHook.handleEvent({}): operation ignored.", listenerEvent);
                        }
                        break;
                }

                if (event != null) {
                    final UserGroupInformation ugi = SecurityUtils.getUGI() == null ? Utils.getUGI() : SecurityUtils.getUGI();

                    super.notifyEntities(event.getNotificationMessages(), ugi);
                }
            } catch (Throwable t) {
                LOG.error("HiveMetastoreHook.handleEvent({}): failed to process operation {}", listenerEvent, t);
            }
        }
    }

    private static boolean isTableRename(Table oldTable, Table newTable) {
        String oldTableName = oldTable.getTableName();
        String newTableName = newTable.getTableName();

        return !StringUtils.equalsIgnoreCase(oldTableName, newTableName);
    }

    private static boolean isColumnRename(Table oldTable, Table newTable, HiveOperationContext context) {
        FieldSchema columnOld      = findRenamedColumn(oldTable, newTable);
        FieldSchema columnNew      = findRenamedColumn(newTable, oldTable);
        boolean     isColumnRename = columnOld != null && columnNew != null;

        if (isColumnRename) {
            context.setColumnOld(columnOld);
            context.setColumnNew(columnNew);
        }

        return isColumnRename;
    }

    private boolean isAlterTableProperty(AlterTableEvent tableEvent, String propertyToCheck) {
        final boolean ret;
        String        oldTableModifiedTime = tableEvent.getOldTable().getParameters().get(propertyToCheck);
        String        newTableModifiedTime = tableEvent.getNewTable().getParameters().get(propertyToCheck);


        if (oldTableModifiedTime == null) {
            ret = newTableModifiedTime != null;
        } else {
            ret = !oldTableModifiedTime.equals(newTableModifiedTime);
        }

        return ret;

    }
}