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
package org.apache.ambari.server.security.authentication;

import java.sql.SQLException;

import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class LdapToPamMigrationHelper {
  private static final Logger LOG = LoggerFactory.getLogger(LdapToPamMigrationHelper.class);

  @Inject
  private DBAccessor dbAccessor;

  /**
   * Migrate LDAP user & groups to PAM
   *
   * @throws SQLException if an error occurs while executing the needed SQL statements
   */
  private void migrateLdapUsersGroups() throws SQLException {
    if (dbAccessor.getDbType() != DbType.ORACLE) { // Tested MYSQL, POSTGRES && MYSQL)
      dbAccessor.executeQuery("UPDATE users SET user_type='PAM',ldap_user=0 WHERE ldap_user=1 and user_name not in (select user_name from (select user_name from users where user_type = 'PAM') as a)");
      dbAccessor.executeQuery("UPDATE groups SET group_type='PAM',ldap_group=0 WHERE ldap_group=1 and group_name not in (select group_name from (select group_name from groups where group_type = 'PAM') as a)");
    } else { // Tested ORACLE
      dbAccessor.executeQuery("UPDATE users SET user_type='PAM',ldap_user=0 WHERE ldap_user=1 and user_name not in (select user_name from users where user_type = 'PAM')");
      dbAccessor.executeQuery("UPDATE groups SET group_type='PAM',ldap_group=0 WHERE ldap_group=1 and group_name not in (select group_name from groups where group_type = 'PAM')");
    }
  }

  /**
   * Support changes needed to migrate LDAP users & groups to PAM
   *
   * @param args Simple key value json map
   */
  public static void main(String[] args) {

    try {
      Injector injector = Guice.createInjector(new ControllerModule(), new AuditLoggerModule());
      LdapToPamMigrationHelper migrationHelper = injector.getInstance(LdapToPamMigrationHelper.class);

      migrationHelper.migrateLdapUsersGroups();

    } catch (Throwable t) {
      LOG.error("Caught exception on migration. Exiting...", t);
      System.exit(1);
    }

  }
}