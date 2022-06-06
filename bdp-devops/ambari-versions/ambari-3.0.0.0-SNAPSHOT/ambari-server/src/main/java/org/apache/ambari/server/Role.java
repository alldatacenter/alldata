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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Role defines the components that are available to Ambari.  It has a few
 * similar mechanisms to an enum.
 */
public class Role {

  private static final Map<String, Role> roles = new ConcurrentHashMap<>();

  /**
   * @param name the role name
   * @return a Role instance, never <code>null</code>
   */
  public static Role valueOf(String name) {
    if (roles.containsKey(name)) {
      return roles.get(name);
    }

    Role role = new Role(name);
    roles.put(name, role);
    return role;
  }

  /**
   * @return a collection of all defined Role instances
   */
  public static Collection<Role> values() {
    return Collections.unmodifiableCollection(roles.values());
  }

  public static final Role AMBARI_SERVER_ACTION = valueOf("AMBARI_SERVER_ACTION");
  public static final Role DATANODE = valueOf("DATANODE");
  public static final Role FLUME_HANDLER = valueOf("FLUME_HANDLER");
  public static final Role FLUME_SERVICE_CHECK = valueOf("FLUME_SERVICE_CHECK");
  public static final Role GANGLIA_MONITOR = valueOf("GANGLIA_MONITOR");
  public static final Role GANGLIA_SERVER = valueOf("GANGLIA_SERVER");
  public static final Role HBASE_CLIENT = valueOf("HBASE_CLIENT");
  public static final Role HBASE_MASTER = valueOf("HBASE_MASTER");
  public static final Role HBASE_REGIONSERVER = valueOf("HBASE_REGIONSERVER");
  public static final Role HBASE_SERVICE_CHECK = valueOf("HBASE_SERVICE_CHECK");
  public static final Role HCAT = valueOf("HCAT");
  public static final Role HCAT_SERVICE_CHECK = valueOf("HCAT_SERVICE_CHECK");
  public static final Role GLUSTERFS_CLIENT = valueOf("GLUSTERFS_CLIENT");
  public static final Role GLUSTERFS_SERVICE_CHECK = valueOf("GLUSTERFS_SERVICE_CHECK");
  public static final Role HDFS_CLIENT = valueOf("HDFS_CLIENT");
  public static final Role HDFS_SERVICE_CHECK = valueOf("HDFS_SERVICE_CHECK");
  public static final Role HISTORYSERVER = valueOf("HISTORYSERVER");
  public static final Role HIVE_CLIENT = valueOf("HIVE_CLIENT");
  public static final Role HIVE_METASTORE = valueOf("HIVE_METASTORE");
  public static final Role HIVE_SERVER = valueOf("HIVE_SERVER");
  public static final Role HIVE_SERVICE_CHECK = valueOf("HIVE_SERVICE_CHECK");
  public static final Role JOBTRACKER = valueOf("JOBTRACKER");
  public static final Role NODEMANAGER = valueOf("NODEMANAGER");
  public static final Role OOZIE_CLIENT = valueOf("OOZIE_CLIENT");
  public static final Role OOZIE_SERVER = valueOf("OOZIE_SERVER");
  public static final Role PEERSTATUS = valueOf("PEERSTATUS");
  public static final Role PIG = valueOf("PIG");
  public static final Role PIG_SERVICE_CHECK = valueOf("PIG_SERVICE_CHECK");
  public static final Role MAHOUT = valueOf("MAHOUT");
  public static final Role MAHOUT_SERVICE_CHECK = valueOf("MAHOUT_SERVICE_CHECK");
  public static final Role RESOURCEMANAGER = valueOf("RESOURCEMANAGER");
  public static final Role SECONDARY_NAMENODE = valueOf("SECONDARY_NAMENODE");
  public static final Role SQOOP = valueOf("SQOOP");
  public static final Role SQOOP_SERVICE_CHECK = valueOf("SQOOP_SERVICE_CHECK");
  public static final Role HUE_SERVER = valueOf("HUE_SERVER");
  public static final Role JOURNALNODE = valueOf("JOURNALNODE");
  public static final Role MAPREDUCE_CLIENT = valueOf("MAPREDUCE_CLIENT");
  public static final Role MAPREDUCE_SERVICE_CHECK = valueOf("MAPREDUCE_SERVICE_CHECK");
  public static final Role MAPREDUCE2_SERVICE_CHECK = valueOf("MAPREDUCE2_SERVICE_CHECK");
  public static final Role MYSQL_SERVER = valueOf("MYSQL_SERVER");
  public static final Role NAMENODE = valueOf("NAMENODE");
  public static final Role NAMENODE_SERVICE_CHECK = valueOf("NAMENODE_SERVICE_CHECK");
  public static final Role OOZIE_SERVICE_CHECK = valueOf("OOZIE_SERVICE_CHECK");
  public static final Role TASKTRACKER = valueOf("TASKTRACKER");
  public static final Role WEBHCAT_SERVER = valueOf("WEBHCAT_SERVER");
  public static final Role WEBHCAT_SERVICE_CHECK = valueOf("WEBHCAT_SERVICE_CHECK");
  public static final Role YARN_SERVICE_CHECK = valueOf("YARN_SERVICE_CHECK");
  public static final Role ZKFC = valueOf("ZKFC");
  public static final Role ZOOKEEPER_CLIENT = valueOf("ZOOKEEPER_CLIENT");
  public static final Role ZOOKEEPER_QUORUM_SERVICE_CHECK = valueOf("ZOOKEEPER_QUORUM_SERVICE_CHECK");
  public static final Role ZOOKEEPER_SERVER = valueOf("ZOOKEEPER_SERVER");
  public static final Role FALCON_SERVICE_CHECK = valueOf("FALCON_SERVICE_CHECK");
  public static final Role STORM_SERVICE_CHECK = valueOf("STORM_SERVICE_CHECK");
  public static final Role YARN_CLIENT = valueOf("YARN_CLIENT");
  public static final Role KDC_SERVER = valueOf("KDC_SERVER");
  public static final Role KERBEROS_CLIENT = valueOf("KERBEROS_CLIENT");
  public static final Role KERBEROS_SERVICE_CHECK = valueOf("KERBEROS_SERVICE_CHECK");
  public static final Role METRICS_COLLECTOR = valueOf("METRICS_COLLECTOR");
  public static final Role METRICS_MONITOR = valueOf("METRICS_MONITOR");
  public static final Role AMS_SERVICE_CHECK = valueOf("AMBARI_METRICS_SERVICE_CHECK");
  public static final Role ACCUMULO_CLIENT = valueOf("ACCUMULO_CLIENT");
  public static final Role RANGER_ADMIN  = valueOf("RANGER_ADMIN");
  public static final Role RANGER_USERSYNC = valueOf("RANGER_USERSYNC");
  public static final Role KNOX_GATEWAY = valueOf("KNOX_GATEWAY");
  public static final Role KAFKA_BROKER = valueOf("KAFKA_BROKER");
  public static final Role NIMBUS = valueOf("NIMBUS");
  public static final Role RANGER_KMS_SERVER = valueOf("RANGER_KMS_SERVER");
  public static final Role LOGSEARCH_SERVER = valueOf("LOGSEARCH_SERVER");
  public static final Role INFRA_SOLR = valueOf("INFRA_SOLR");
  public static final Role LOGSEARCH_LOGFEEDER = valueOf("LOGSEARCH_LOGFEEDER");
  public static final Role INSTALL_PACKAGES = valueOf("install_packages");
  public static final Role UPDATE_REPO = valueOf("update_repo");

  private String name = null;

  private Role(String roleName) {
    name = roleName;
  }

  /**
   * @return the name given to the role
   */
  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (null == o || !Role.class.equals(o.getClass())) {
      return false;
    }

    return this == o || name.equals(((Role) o).name);
  }

}
