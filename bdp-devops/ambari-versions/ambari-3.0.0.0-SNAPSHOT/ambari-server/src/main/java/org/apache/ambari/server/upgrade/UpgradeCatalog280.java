/*
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
package org.apache.ambari.server.upgrade;

import java.sql.SQLException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.DBAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;


/**
 * The {@link UpgradeCatalog280} upgrades Ambari from 2.7.2 to 2.8.0.
 */
public class UpgradeCatalog280 extends AbstractUpgradeCatalog {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog280.class);

  private static final String REQUEST_SCHEDULE_TABLE_NAME = "requestschedule";
  protected static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  private static final String REQUEST_SCHEDULE_BATCH_TOLERATION_LIMIT_PER_BATCH_COLUMN_NAME = "batch_toleration_limit_per_batch";
  private static final String REQUEST_SCHEDULE_PAUSE_AFTER_FIRST_BATCH_COLUMN_NAME = "pause_after_first_batch";
  protected static final String LAST_LIVE_STATE_COLUMN = "last_live_state";

  private static final String UPGRADE_TABLE = "upgrade";
  private static final String UPGRADE_PACK_STACK_ID = "upgrade_pack_stack_id";

  protected static final String AMBARI_CONFIGURATION_TABLE = "ambari_configuration";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = "property_value";

  @Inject
  public UpgradeCatalog280(Injector injector) {
    super(injector);
  }

  @Override
  public String getSourceVersion() {
    return "2.7.2";
  }

  @Override
  public String getTargetVersion() {
    return "2.8.0";
  }

  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    removeLastValidState();
    addColumnsToRequestScheduleTable();
    addColumnsToUpgradeTable();
    modifyPropertyValueColumnInAmbariConfigurationTable();
  }

  private void modifyPropertyValueColumnInAmbariConfigurationTable() throws SQLException {
    dbAccessor.alterColumn(AMBARI_CONFIGURATION_TABLE, new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 4000, null, false));
    LOG.info("Altered {}.{} to NOT NULL and extended its length to 4000", AMBARI_CONFIGURATION_TABLE, AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN);
  }

  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
  }

  protected void addColumnsToRequestScheduleTable() throws SQLException {
    dbAccessor.addColumn(REQUEST_SCHEDULE_TABLE_NAME,
        new DBAccessor.DBColumnInfo(REQUEST_SCHEDULE_BATCH_TOLERATION_LIMIT_PER_BATCH_COLUMN_NAME, Short.class, null,
            null, true));
    dbAccessor.addColumn(REQUEST_SCHEDULE_TABLE_NAME,
        new DBAccessor.DBColumnInfo(REQUEST_SCHEDULE_PAUSE_AFTER_FIRST_BATCH_COLUMN_NAME, Boolean.class, null,
            null, true));    
  }

  protected void addColumnsToUpgradeTable() throws SQLException {
    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(UPGRADE_PACK_STACK_ID, String.class, 255,
            "", false));
  }

  protected void removeLastValidState() throws SQLException {
    dbAccessor.dropColumn(HOST_COMPONENT_STATE_TABLE, LAST_LIVE_STATE_COLUMN);
  }

}
