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

package org.apache.ambari.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.metamodel.EntityType;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessorImpl;

import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class H2DatabaseCleaner {
  private static final String SEQ_STATEMENT =
      "INSERT INTO ambari_sequences(sequence_name, sequence_value) values (?, 0);";
  private static List<String> sequenceList = new ArrayList<>();

  static {
        sequenceList.add("extension_id_seq");
        sequenceList.add("resource_id_seq");
        sequenceList.add("alert_target_id_seq");
        sequenceList.add("topology_request_id_seq");
        sequenceList.add("setting_id_seq");
        sequenceList.add("principal_type_id_seq");
        sequenceList.add("group_id_seq");
        sequenceList.add("remote_cluster_id_seq");
        sequenceList.add("privilege_id_seq");
        sequenceList.add("servicecomponent_history_id_seq");
        sequenceList.add("permission_id_seq");
        sequenceList.add("principal_id_seq");
        sequenceList.add("repo_version_id_seq");
        sequenceList.add("cluster_version_id_seq");
        sequenceList.add("topology_host_task_id_seq");
        sequenceList.add("topology_logical_task_id_seq");
        sequenceList.add("host_id_seq");
        sequenceList.add("servicecomponentdesiredstate_id_seq");
        sequenceList.add("configgroup_id_seq");
        sequenceList.add("topology_host_group_id_seq");
        sequenceList.add("upgrade_item_id_seq");
        sequenceList.add("requestschedule_id_seq");
        sequenceList.add("blueprint_setting_id_seq");
        sequenceList.add("host_version_id_seq");
        sequenceList.add("hostcomponentstate_id_seq");
        sequenceList.add("cluster_id_seq");
        sequenceList.add("view_instance_id_seq");
        sequenceList.add("resourcefilter_id_seq");
        sequenceList.add("alert_group_id_seq");
        sequenceList.add("link_id_seq");
        sequenceList.add("topology_host_info_id_seq");
        sequenceList.add("viewentity_id_seq");
        sequenceList.add("alert_notice_id_seq");
        sequenceList.add("user_id_seq");
        sequenceList.add("upgrade_id_seq");
        sequenceList.add("stack_id_seq");
        sequenceList.add("alert_current_id_seq");
        sequenceList.add("widget_id_seq");
        sequenceList.add("remote_cluster_service_id_seq");
        sequenceList.add("alert_history_id_seq");
        sequenceList.add("config_id_seq");
        sequenceList.add("upgrade_group_id_seq");
        sequenceList.add("member_id_seq");
        sequenceList.add("service_config_id_seq");
        sequenceList.add("widget_layout_id_seq");
        sequenceList.add("hostcomponentdesiredstate_id_seq");
        sequenceList.add("operation_level_id_seq");
        sequenceList.add("servicecomponent_version_id_seq");
        sequenceList.add("host_role_command_id_seq");
        sequenceList.add("alert_definition_id_seq");
        sequenceList.add("resource_type_id_seq");
  }

  public static void clearDatabaseAndStopPersistenceService(Injector injector) throws AmbariException, SQLException {
    clearDatabase(injector.getProvider(EntityManager.class).get());
    injector.getInstance(PersistService.class).stop();
  }

  public static void clearDatabase(EntityManager entityManager) throws AmbariException, SQLException {
    clearDatabase(entityManager, Configuration.JDBC_IN_MEMORY_URL,
      Configuration.JDBC_IN_MEMORY_USER, Configuration.JDBC_IN_MEMORY_PASSWORD);
  }

  //TODO all tests this method is used in should be modified to remove hardcoded IDs
  public static void resetSequences(Injector injector) {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    try {
      if (dbAccessor.tableExists("ambari_sequences")) {
        dbAccessor.truncateTable("ambari_sequences");
        PreparedStatement preparedStatement = dbAccessor.getConnection().prepareStatement(SEQ_STATEMENT);
        try {
          for (String sequenceName : sequenceList) {
            preparedStatement.setString(1, sequenceName);
            preparedStatement.executeUpdate();
          }
        } finally {
          preparedStatement.close();
        }

      }
    } catch (SQLException ignored) {
    }
  }

  public static void clearDatabase(EntityManager entityManager, String dbURL, String dbUser, String dbPass) throws SQLException {
    Connection connection = DriverManager.getConnection(dbURL, dbUser, dbPass);
    Statement s = connection.createStatement();

    try {
      // Disable FK
      s.execute("SET REFERENTIAL_INTEGRITY FALSE");

      entityManager.getTransaction().begin();
      // Truncate tables for all entities
      for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
        Query query = entityManager.createQuery("DELETE FROM " + entity.getName() + " em");
        query.executeUpdate();
//        final String tableName = entity.getBindableJavaType().getAnnotation(Table.class).name();
//        s.executeUpdate("TRUNCATE TABLE " + tableName);
      }

      entityManager.getTransaction().commit();

      // Enable FK
      s.execute("SET REFERENTIAL_INTEGRITY TRUE");

      //reset shared cache
      entityManager.getEntityManagerFactory().getCache().evictAll();
    } finally {
      s.close();
      connection.close();
    }
  }
}
