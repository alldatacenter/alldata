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

package org.apache.ambari.server.view;

import java.util.Map;

import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.PersistenceException;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for view data migration.
 */
public class ViewDataMigrationUtility {

  /**
   * The logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ViewDataMigrationUtility.class);

  /**
   * The View Registry.
   */
  private ViewRegistry viewRegistry;

  /**
   * Constructor.
   * @param viewRegistry the view registry
   */
  public ViewDataMigrationUtility(ViewRegistry viewRegistry) {
    this.viewRegistry = viewRegistry;
  }

  /**
   * Migrates data from source to target instance
   * @param targetInstanceDefinition target instance entity
   * @param sourceInstanceDefinition source instance entity
   * @param migrateOnce cancel if previously migrated
   *
   * @throws ViewDataMigrationException when view does not support migration or an error during migration occurs.
   */
  public void migrateData(ViewInstanceEntity targetInstanceDefinition, ViewInstanceEntity sourceInstanceDefinition,
                          boolean migrateOnce)
      throws ViewDataMigrationException {
    ViewDataMigrationContextImpl migrationContext = getViewDataMigrationContext(targetInstanceDefinition, sourceInstanceDefinition);

    if (migrateOnce) {
      if (!isTargetEmpty(migrationContext)) {
          LOG.error("Migration canceled because target instance is not empty");
          return;
      }
    }

    ViewDataMigrator dataMigrator = getViewDataMigrator(targetInstanceDefinition, migrationContext);

    LOG.debug("Running before-migration hook");
    if (!dataMigrator.beforeMigration()) {
      String msg = "View " + targetInstanceDefinition.getInstanceName() + " canceled the migration process";

      LOG.error(msg);
      throw new ViewDataMigrationException(msg);
    }

    Map<String, Class> originClasses = migrationContext.getOriginEntityClasses();
    Map<String, Class> currentClasses = migrationContext.getCurrentEntityClasses();
    for (Map.Entry<String, Class> originEntity : originClasses.entrySet()) {
      LOG.debug("Migrating persistence entity {}", originEntity.getKey());
      if (currentClasses.containsKey(originEntity.getKey())) {
        Class entity = currentClasses.get(originEntity.getKey());
        dataMigrator.migrateEntity(originEntity.getValue(), entity);
      } else {
        LOG.debug("Entity {} not found in target view", originEntity.getKey());
        dataMigrator.migrateEntity(originEntity.getValue(), null);
      }
    }

    LOG.debug("Migrating instance data");
    dataMigrator.migrateInstanceData();

    LOG.debug("Running after-migration hook");
    dataMigrator.afterMigration();

    LOG.debug("Copying user permissions");
    viewRegistry.copyPrivileges(sourceInstanceDefinition, targetInstanceDefinition);

    migrationContext.putCurrentInstanceData("upgrade", "upgradedFrom", sourceInstanceDefinition.getViewEntity().getVersion());

    migrationContext.closeMigration();
  }

  private boolean isTargetEmpty(ViewDataMigrationContext migrationContext) {
    if (migrationContext.getCurrentInstanceDataByUser().size() > 0) {
      return false;
    }

    try {
      for (Class entity : migrationContext.getCurrentEntityClasses().values()) {
        if (migrationContext.getCurrentDataStore().findAll(entity, null).size() > 0) {
          return false;
        }
      }
    } catch (PersistenceException e) {
      ViewInstanceDefinition current = migrationContext.getCurrentInstanceDefinition();
      LOG.error("Persistence exception while check if instance is empty: " +
          current.getViewDefinition().getViewName() + "{" + current.getViewDefinition().getVersion() + "}/" +
          current.getInstanceName(), e);
    }

    return true;
  }

  /**
   * Create the data migration context for DataMigrator to access data of current
   * and origin instances.
   * @param targetInstanceDefinition target instance definition
   * @param sourceInstanceDefinition source instance definition
   * @return data migration context
   */
  protected ViewDataMigrationContextImpl getViewDataMigrationContext(ViewInstanceEntity targetInstanceDefinition,
                                                                            ViewInstanceEntity sourceInstanceDefinition) {
    return new ViewDataMigrationContextImpl(sourceInstanceDefinition, targetInstanceDefinition);
  }

  /**
   * Get the migrator instance for view instance with injected migration context.
   * If versions of instances are same returns copy-all-data migrator.
   * If versions are different, loads the migrator from the current view (view should
   * contain ViewDataMigrator implementation, otherwise exception will be raised).
   *
   * @param currentInstanceDefinition    the current view instance definition
   * @param migrationContext             the migration context to inject into migrator
   * @throws ViewDataMigrationException  if view does not support migration
   * @return  the data migration instance
   */
  protected ViewDataMigrator getViewDataMigrator(ViewInstanceEntity currentInstanceDefinition,
                                                        ViewDataMigrationContextImpl migrationContext)
      throws ViewDataMigrationException {
    ViewDataMigrator dataMigrator;

    LOG.info("Migrating " + currentInstanceDefinition.getInstanceName() +
        " data from " + migrationContext.getOriginDataVersion() + " to " +
        migrationContext.getCurrentDataVersion() + " data version");

    if (migrationContext.getOriginDataVersion() == migrationContext.getCurrentDataVersion()) {

      LOG.info("Instances of same version, copying all data.");
      dataMigrator = new CopyAllDataMigrator(migrationContext);
    } else {
      try {
        dataMigrator = currentInstanceDefinition.getDataMigrator(migrationContext);
        if (dataMigrator == null) {
          throw new ViewDataMigrationException("A view instance " +
              currentInstanceDefinition.getInstanceName() + " does not support migration.");
        }
        LOG.debug("Data migrator loaded");
      } catch (ClassNotFoundException e) {
        String msg = "Caught exception loading data migrator of " + currentInstanceDefinition.getInstanceName();

        LOG.error(msg, e);
        throw new RuntimeException(msg);
      }
    }
    return dataMigrator;
  }


  /**
   * The data migrator implementation that copies all data without modification.
   * Used to copy data between instances of same version.
   */
  public static class CopyAllDataMigrator implements ViewDataMigrator {
    private ViewDataMigrationContext migrationContext;

    public CopyAllDataMigrator(ViewDataMigrationContext migrationContext) {
      this.migrationContext = migrationContext;
    }

    @Override
    public boolean beforeMigration() {
      return true;
    }

    @Override
    public void afterMigration() {
    }

    @Override
    public void migrateEntity(Class originEntityClass, Class currentEntityClass)
        throws ViewDataMigrationException {
      if (currentEntityClass == null) {
        return;
      }
      migrationContext.copyAllObjects(originEntityClass, currentEntityClass);
    }

    @Override
    public void migrateInstanceData() {
      migrationContext.copyAllInstanceData();
    }
  }
}
