#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

__all__ = ["Direction", "SafeMode", "StackFeature"]


class Direction:
  """
  Stack Upgrade direction
  """
  UPGRADE = "upgrade"
  DOWNGRADE = "downgrade"


class SafeMode:
  """
  Namenode Safe Mode state
  """
  ON = "ON"
  OFF = "OFF"
  UNKNOWN = "UNKNOWN"


class StackFeature:
  """
  Stack Feature supported
  """
  SNAPPY = "snappy"
  LZO = "lzo"
  EXPRESS_UPGRADE = "express_upgrade"
  ROLLING_UPGRADE = "rolling_upgrade"
  CONFIG_VERSIONING = "config_versioning"
  FALCON_EXTENSIONS = "falcon_extensions"
  DATANODE_NON_ROOT = "datanode_non_root"
  SECURE_ZOOKEEPER = "secure_zookeeper"
  HADOOP_CUSTOM_EXTENSIONS = "hadoop_custom_extensions"
  REMOVE_RANGER_HDFS_PLUGIN_ENV = "remove_ranger_hdfs_plugin_env"
  RANGER = "ranger"
  RANGER_TAGSYNC_COMPONENT = "ranger_tagsync_component"
  PHOENIX = "phoenix"
  NFS = "nfs"
  TEZ_FOR_SPARK = "tez_for_spark"
  TIMELINE_STATE_STORE = "timeline_state_store"
  COPY_TARBALL_TO_HDFS = "copy_tarball_to_hdfs"
  SPARK_16PLUS = "spark_16plus"
  SPARK_THRIFTSERVER = "spark_thriftserver"
  SPARK_LIVY = "spark_livy"
  SPARK_LIVY2 = "spark_livy2"
  STORM_KERBEROS = "storm_kerberos"
  STORM_AMS = "storm_ams"
  KAFKA_LISTENERS = "kafka_listeners"
  KAFKA_KERBEROS = "kafka_kerberos"
  PIG_ON_TEZ = "pig_on_tez"
  RANGER_USERSYNC_NON_ROOT = "ranger_usersync_non_root"
  RANGER_AUDIT_DB_SUPPORT = "ranger_audit_db_support"
  ACCUMULO_KERBEROS_USER_AUTH = "accumulo_kerberos_user_auth"
  KNOX_VERSIONED_DATA_DIR = "knox_versioned_data_dir"
  KNOX_SSO_TOPOLOGY = "knox_sso_topology"
  OOZIE_ADMIN_USER = "oozie_admin_user"
  OOZIE_CREATE_HIVE_TEZ_CONFIGS = "oozie_create_hive_tez_configs"
  OOZIE_SETUP_SHARED_LIB = "oozie_setup_shared_lib"
  OOZIE_HOST_KERBEROS = "oozie_host_kerberos"
  HIVE_METASTORE_UPGRADE_SCHEMA = "hive_metastore_upgrade_schema"
  HIVE_SERVER_INTERACTIVE = "hive_server_interactive"
  HIVE_WEBHCAT_SPECIFIC_CONFIGS = "hive_webhcat_specific_configs"
  HIVE_PURGE_TABLE = "hive_purge_table"
  HIVE_SERVER2_KERBERIZED_ENV = "hive_server2_kerberized_env"
  HIVE_ENV_HEAPSIZE = "hive_env_heapsize"
  RANGER_KMS_HSM_SUPPORT = "ranger_kms_hsm_support"
  RANGER_LOG4J_SUPPORT = "ranger_log4j_support"
  RANGER_KERBEROS_SUPPORT = "ranger_kerberos_support"
  HIVE_METASTORE_SITE_SUPPORT = "hive_metastore_site_support"
  RANGER_USERSYNC_PASSWORD_JCEKS = "ranger_usersync_password_jceks"
  RANGER_INSTALL_INFRA_CLIENT = "ranger_install_infra_client"
  HBASE_HOME_DIRECTORY = "hbase_home_directory"
  ATLAS_RANGER_PLUGIN_SUPPORT = "atlas_ranger_plugin_support"
  ATLAS_UPGRADE_SUPPORT = "atlas_upgrade_support"
  ATLAS_CONF_DIR_IN_PATH = "atlas_conf_dir_in_path"
  ATLAS_HOOK_SUPPORT = "atlas_hook_support"
  FALCON_ATLAS_SUPPORT_2_3 = "falcon_atlas_support_2_3"
  FALCON_ATLAS_SUPPORT = "falcon_atlas_support"
  RANGER_PID_SUPPORT = "ranger_pid_support"
  RANGER_KMS_PID_SUPPORT = "ranger_kms_pid_support"
  RANGER_ADMIN_PASSWD_CHANGE = "ranger_admin_password_change"
  RANGER_SETUP_DB_ON_START = "ranger_setup_db_on_start"
  STORM_METRICS_APACHE_CLASSES = "storm_metrics_apache_classes"
  SPARK_JAVA_OPTS_SUPPORT = "spark_java_opts_support"
  ATLAS_HBASE_SETUP = "atlas_hbase_setup"
  RANGER_HIVE_PLUGIN_JDBC_URL = "ranger_hive_plugin_jdbc_url"
  ZKFC_VERSION_ADVERTISED = "zkfc_version_advertised"
  PHOENIX_CORE_HDFS_SITE_REQUIRED = "phoenix_core_hdfs_site_required"
  RANGER_TAGSYNC_SSL_XML_SUPPORT="ranger_tagsync_ssl_xml_support"
  RANGER_XML_CONFIGURATION = "ranger_xml_configuration"
  KAFKA_RANGER_PLUGIN_SUPPORT = "kafka_ranger_plugin_support"
  YARN_RANGER_PLUGIN_SUPPORT = "yarn_ranger_plugin_support"
  RANGER_SOLR_CONFIG_SUPPORT='ranger_solr_config_support'
  HIVE_INTERACTIVE_ATLAS_HOOK_REQUIRED = "hive_interactive_atlas_hook_required"
  CORE_SITE_FOR_RANGER_PLUGINS_SUPPORT = 'core_site_for_ranger_plugins'
  ATLAS_INSTALL_HOOK_PACKAGE_SUPPORT = "atlas_install_hook_package_support"
  ATLAS_HDFS_SITE_ON_NAMENODE_HA = 'atlas_hdfs_site_on_namenode_ha'
  HIVE_INTERACTIVE_GA_SUPPORT = 'hive_interactive_ga'
  SECURE_RANGER_SSL_PASSWORD = "secure_ranger_ssl_password"
  RANGER_KMS_SSL = "ranger_kms_ssl"
  KAFKA_ACL_MIGRATION_SUPPORT = "kafka_acl_migration_support"
  ATLAS_CORE_SITE_SUPPORT="atlas_core_site_support"
  KAFKA_EXTENDED_SASL_SUPPORT = "kafka_extended_sasl_support"
  OOZIE_EXTJS_INCLUDED = "oozie_extjs_included"
  MULTIPLE_ENV_SH_FILES_SUPPORT = "multiple_env_sh_files_support"
  AMS_LEGACY_HADOOP_SINK = "ams_legacy_hadoop_sink"
  RANGER_ALL_ADMIN_CHANGE_DEFAULT_PASSWORD = 'ranger_all_admin_change_default_password'
  KAFKA_ENV_INCLUDE_RANGER_SCRIPT='kafka_env_include_ranger_script'
  RANGER_SUPPORT_SECURITY_ZONE_FEATURE = 'ranger_support_security_zone_feature'