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

package org.apache.ambari.view.phonelist;

import org.apache.ambari.view.*;
import org.apache.ambari.view.migration.EntityConverter;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.springframework.beans.BeanUtils;

import javax.inject.Inject;
import java.util.Map;

/**
 * Class responsible for migration from previous version (phone-list-view example)
 */
public class DataMigrator implements ViewDataMigrator {

  /**
   * The view context of target migration instance.
   */
  @Inject
  private ViewContext viewContext;

  /**
   * The migration context.
   */
  @Inject
  private ViewDataMigrationContext migrationContext;

  /**
   * Called by the framework before migration.
   * Supports only migration from version 1.0.0, so we cancel migration
   * if the origin version is not 0.
   *
   * @return true if migration started against the instance with data version "0"
   */
  @Override
  public boolean beforeMigration() {
    return migrationContext.getOriginDataVersion() == 0;
  }

  /**
   * Called by the framework after migration.
   */
  @Override
  public void afterMigration() {
  }

  /**
   * Migrate a single persistence entity.
   *
   * @param originEntityClass    class object of origin (migration source) instance
   * @param currentEntityClass   class object of current (migration target) instance
   */
  @Override
  public void migrateEntity(Class originEntityClass, Class currentEntityClass) throws ViewDataMigrationException {
    if (currentEntityClass == PhoneUser.class) {
      migrationContext.copyAllObjects(originEntityClass, currentEntityClass, new PhoneUserConverter());
    } else {
      migrationContext.copyAllObjects(originEntityClass, currentEntityClass);
    }
  }

  /**
   * Migrate instance data by adding surname.
   */
  @Override
  public void migrateInstanceData() {
    for (Map.Entry<String, Map<String, String>> userData : migrationContext.getOriginInstanceDataByUser().entrySet()) {
      for (Map.Entry<String, String> entry : userData.getValue().entrySet()) {
        String newValue = String.format("<no surname>;%s", entry.getValue());
        migrationContext.putCurrentInstanceData(userData.getKey(), entry.getKey(), newValue);
      }
    }
  }

  /**
   * The entity converter class responsible of converting PhoneUser data.
   */
  private static class PhoneUserConverter implements EntityConverter {

    /**
     * Adds surname to the new version of PhoneData. If original user name
     * contained several words, last word is used as surname.
     *
     * @param orig a single origin entity object.
     * @param dest an empty object of current persistence entity class.
     */
    @Override
    public void convert(Object orig, Object dest) {
      PhoneUser destPhone = (PhoneUser) dest;

      BeanUtils.copyProperties(orig, dest);
      if (destPhone.getName() == null) {
        destPhone.setSurname("<no surname>");
      } else {
        String[] parts = destPhone.getName().split(" ");
        if (parts.length > 1) {
          destPhone.setSurname(parts[parts.length - 1]);
        } else {
          destPhone.setSurname("<no surname>");
        }
      }

    }

  }
}
