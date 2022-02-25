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

"""

__all__ = ["has_atlas_in_cluster", "setup_atlas_hook", "setup_atlas_jar_symlinks"]

# Python Imports
import os

# Local Imports
from resource_management.libraries.functions import stack_features
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.script import Script
from resource_management.core.resources.system import Link
from resource_management.core.resources.packaging import Package
from resource_management.core.logger import Logger
from ambari_commons import OSCheck
from ambari_commons.constants import SERVICE

'''
Only this subset of Atlas application.properties should be written out to each service that has an Atlas hook,
E.g., Hive, Storm, Sqoop, Falcon.
The reason for this is that we don't want configs to get out-of-sync between each of these services.
Assume Atlas application.properties contains props

private_prop_a
private_prop_b
private_prop_c
shared_atlas_hook_prop_d
shared_atlas_hook_prop_e

Then only shared_atlas_hook_prop_d and shared_atlas_hook_prop_e should be merged with the properties specific to
Hive, Storm, Sqoop, and Falcon.
E.g.,
Hive has,
specific_hive_atlas_hook_prop_f
specific_hive_atlas_hook_prop_g

So the atlas-application.properties.xml file that we write for Hive should contain,
shared_atlas_hook_prop_d
shared_atlas_hook_prop_e
specific_hive_atlas_hook_prop_f
specific_hive_atlas_hook_prop_g

Now, if the user wants to make a global change for Atlas hooks, they can change shared_atlas_hook_prop_d or shared_atlas_hook_prop_e
in a single place (under the Atlas Configs page).
If they want to overwrite shared_atlas_hook_prop_d just for Hive, they can add it to hive-atlas-application.properties
'''

SHARED_ATLAS_HOOK_CONFIGS = set(
  [
  "atlas.kafka.zookeeper.connect",
  "atlas.kafka.bootstrap.servers",
  "atlas.kafka.zookeeper.session.timeout.ms",
  "atlas.kafka.zookeeper.connection.timeout.ms",
  "atlas.kafka.zookeeper.sync.time.ms",
  "atlas.kafka.hook.group.id",
  "atlas.notification.create.topics",
  "atlas.notification.replicas",
  "atlas.notification.topics",
  "atlas.notification.kafka.service.principal",
  "atlas.notification.kafka.keytab.location",
  "atlas.cluster.name",
  "atlas.rest.address",

  # Security properties
  "atlas.jaas.KafkaClient.option.serviceName",
  "atlas.authentication.method.kerberos",
  "atlas.kafka.sasl.kerberos.service.name",
  "atlas.kafka.security.protocol",
  "atlas.jaas.KafkaClient.loginModuleName",
  "atlas.jaas.KafkaClient.loginModuleControlFlag"
  ]
)

SHARED_ATLAS_HOOK_SECURITY_CONFIGS_FOR_NON_CLIENT_SERVICE = set(
  [
    "atlas.jaas.KafkaClient.option.useKeyTab",
    "atlas.jaas.KafkaClient.option.storeKey"
  ]
)

NON_CLIENT_SERVICES = [SERVICE.HIVE, SERVICE.STORM, SERVICE.FALCON, SERVICE.HBASE]

def has_atlas_in_cluster():
  """
  Determine if Atlas is installed on the cluster.
  :return: True if Atlas is installed, otherwise false.
  """
  atlas_hosts = default('/clusterHostInfo/atlas_server_hosts', [])
  return len(atlas_hosts) > 0

def setup_atlas_hook(service_name, service_props, atlas_hook_filepath, owner, group):
  """
  Generate the atlas-application.properties.xml file by merging the service_props with the Atlas application-properties.
  :param service_name: Service Name to identify if it is a client-only service, which will generate slightly different configs.
  :param service_props: Atlas configs specific to this service that must be merged.
  :param atlas_hook_filepath: Config file to write, e.g., /etc/falcon/conf/atlas-application.properties.xml
  :param owner: File owner
  :param group: File group
  """
  import params
  atlas_props = default('/configurations/application-properties', {})
  merged_props = {}
  merged_props.update(service_props)

  if has_atlas_in_cluster():
    # Take the subset
    merged_props = {}
    shared_props = SHARED_ATLAS_HOOK_CONFIGS.copy()
    if service_name in NON_CLIENT_SERVICES:
      shared_props = shared_props.union(SHARED_ATLAS_HOOK_SECURITY_CONFIGS_FOR_NON_CLIENT_SERVICE)

    for prop in shared_props:
      if prop in atlas_props:
        merged_props[prop] = atlas_props[prop]

    merged_props.update(service_props)

  Logger.info(format("Generating Atlas Hook config file {atlas_hook_filepath}"))
  PropertiesFile(atlas_hook_filepath,
           properties = merged_props,
           owner = owner,
           group = group,
           mode = 0644)


def setup_atlas_jar_symlinks(hook_name, jar_source_dir):
  """
  In HDP 2.3, 2.4, and 2.5.0.0, Sqoop and Storm still relied on the following method to setup Atlas hooks
  because the RPM for Sqoop and Storm did not bring in any dependencies.

  /usr/hdp/current/storm-*/libext/ should contain symlinks for every jar in /usr/hdp/current/atlas-server/hooks/storm/somejavafile.jar
  /usr/hdp/current/sqoop-*/lib/    should contain symlinks for every jar in /usr/hdp/current/atlas-server/hooks/sqoop/somejavafile.jar

  In HDP 2.5.x.y, we plan to have the Sqoop and Storm rpms have additional dependencies on some sqoop-atlas-hook and storm-atlas-hook
  rpms, respectively, that will bring in the necessary jars and create the symlinks.

  If atlas is present on this host, then link the jars from
  {stack_root}/current/{hook_name}/lib/name_version.jar -> {jar_source_dir}/name_version.jar
  @param hook_name: one of sqoop, storm
  @param jar_source_dir: directory of where the symlinks need to be created from.
  """
  import params

  stack_root = Script.get_stack_root()
  atlas_component_name = "atlas"
  stack_version = stack_features.get_stack_feature_version(Script.get_config())
  atlas_home_dir = os.path.join(stack_root, stack_version, atlas_component_name)

  # Will only exist if this host contains Atlas Server
  atlas_hook_dir = os.path.join(atlas_home_dir, "hook", hook_name)

  if os.path.exists(atlas_hook_dir):
    Logger.info("Atlas Server is present on this host, will symlink jars inside of %s to %s if not already done." %
                (jar_source_dir, atlas_hook_dir))

    src_files = os.listdir(atlas_hook_dir)
    for file_name in src_files:
      atlas_hook_file_name = os.path.join(atlas_hook_dir, file_name)
      source_lib_file_name = os.path.join(jar_source_dir, file_name)
      if os.path.isfile(atlas_hook_file_name):
        Link(source_lib_file_name, to=atlas_hook_file_name)
  else:
    Logger.info("Atlas hook directory path {0} doesn't exist".format(atlas_hook_dir))

def install_atlas_hook_packages(atlas_plugin_package, atlas_ubuntu_plugin_package, host_sys_prepped,
                                agent_stack_retry_on_unavailability, agent_stack_retry_count):
  if not host_sys_prepped:
    # This will install packages like atlas-metadata-${service}-plugin needed for Falcon and Hive.
    Package(atlas_ubuntu_plugin_package if OSCheck.is_ubuntu_family() else atlas_plugin_package,
            retry_on_repo_unavailability=agent_stack_retry_on_unavailability, retry_count=agent_stack_retry_count)