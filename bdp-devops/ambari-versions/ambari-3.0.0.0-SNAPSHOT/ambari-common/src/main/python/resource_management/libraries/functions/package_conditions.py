#!/usr/bin/env python
"""
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

Ambari Agent

"""
__all__ = ["should_install_phoenix", "should_install_ams_collector", "should_install_ams_grafana",
           "should_install_mysql", "should_install_ranger_tagsync"]

import os
from resource_management.libraries.script import Script
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version

def _has_local_components(config, components, indicator_function = any):
  if 'role' not in config:
    return False
  if config['role'] == 'install_packages':
    # When installing new stack version for upgrade, all packages on a host are installed by install_packages.
    # Check if
    if 'localComponents' not in config:
      return False
    return indicator_function([component in config['localComponents'] for component in components])
  else:
    return config['role'] in components

def _has_applicable_local_component(config, components):
  return _has_local_components(config, components, any)

def should_install_phoenix():
  phoenix_hosts = default('/clusterHostInfo/phoenix_query_server_hosts', [])
  phoenix_enabled = default('/configurations/hbase-env/phoenix_sql_enabled', False)
  has_phoenix = len(phoenix_hosts) > 0
  return phoenix_enabled or has_phoenix

def should_install_ams_collector():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["METRICS_COLLECTOR"])

def should_install_ams_grafana():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["METRICS_GRAFANA"])

def should_install_infra_solr():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["INFRA_SOLR"])

def should_install_infra_solr_client():
  config = Script.get_config()
  return _has_applicable_local_component(config, ['INFRA_SOLR_CLIENT', 'ATLAS_SERVER', 'RANGER_ADMIN', 'LOGSEARCH_SERVER'])

def should_install_logsearch_portal():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["LOGSEARCH_SERVER"])

def should_install_mysql():
  config = Script.get_config()
  hive_database = config['configurations']['hive-env']['hive_database']
  hive_use_existing_db = hive_database.startswith('Existing')

  if hive_use_existing_db:
    return False
  return _has_applicable_local_component(config, "MYSQL_SERVER")

def should_install_mysql_connector():
  config = Script.get_config()
  hive_database = config['configurations']['hive-env']['hive_database']
  hive_use_existing_db = hive_database.startswith('Existing')

  if hive_use_existing_db:
    return False
  return _has_applicable_local_component(config, ["MYSQL_SERVER", "HIVE_METASTORE", "HIVE_SERVER", "HIVE_SERVER_INTERACTIVE"])

def should_install_hive_atlas():
  atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
  has_atlas = len(atlas_hosts) > 0
  return has_atlas

def should_install_falcon_atlas_hook():
  config = Script.get_config()
  stack_version_unformatted = config['clusterLevelParams']['stack_version']
  stack_version_formatted = format_stack_version(stack_version_unformatted)
  if check_stack_feature(StackFeature.FALCON_ATLAS_SUPPORT_2_3, stack_version_formatted) \
      or check_stack_feature(StackFeature.FALCON_ATLAS_SUPPORT, stack_version_formatted):
    return _has_applicable_local_component(config, ['FALCON_SERVER'])
  return False

def should_install_ranger_tagsync():
  config = Script.get_config()
  ranger_tagsync_hosts = default("/clusterHostInfo/ranger_tagsync_hosts", [])
  has_ranger_tagsync = len(ranger_tagsync_hosts) > 0

  return has_ranger_tagsync

def should_install_rpcbind():
  config = Script.get_config()
  return _has_applicable_local_component(config, ["NFS_GATEWAY"])
