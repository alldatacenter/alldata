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

# Python Imports
import os
import glob
from urlparse import urlparse

# Resource Management and Common Imports
from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import compare_versions
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.source import StaticFile, Template, DownloadSource, InlineTemplate
from resource_management.core.shell import as_user
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_sudo
from resource_management.core.shell import quote_bash_args
from resource_management.core.logger import Logger
from resource_management.core import utils

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from hive import fill_conf_dir, jdbc_connector


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive_interactive(name=None):
  pass

"""
Sets up the configs, jdbc connection and tarball copy to HDFS for Hive Server Interactive.
"""
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive_interactive(name=None):
  import params

  # list of properties that should be excluded from the config
  # this approach is a compromise against adding a dedicated config
  # type for hive_server_interactive or needed config groups on a
  # per component basis
  exclude_list = ['hive.enforce.bucketing',
                  'hive.enforce.sorting']

  # List of configs to be excluded from hive2 client, but present in Hive2 server.
  exclude_list_for_hive2_client = ['javax.jdo.option.ConnectionPassword']

  # Copy Tarballs in HDFS.
  if params.stack_version_formatted_major and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version_formatted_major):
    resource_created = copy_to_hdfs("tez_hive2",
                 params.user_group,
                 params.hdfs_user,
                 file_mode=params.tarballs_mode,
                 host_sys_prepped=params.host_sys_prepped)

    if resource_created:
      params.HdfsResource(None, action="execute")

  Directory(params.hive_interactive_etc_dir_prefix,
            mode=0755
            )

  Logger.info("Directories to fill with configs: %s" % str(params.hive_conf_dirs_list))
  for conf_dir in params.hive_conf_dirs_list:
    fill_conf_dir(conf_dir)

  '''
  As hive2/hive-site.xml only contains the new + the changed props compared to hive/hive-site.xml,
  we need to merge hive/hive-site.xml and hive2/hive-site.xml and store it in hive2/hive-site.xml.
  '''
  merged_hive_interactive_site = {}
  merged_hive_interactive_site.update(params.config['configurations']['hive-site'])
  merged_hive_interactive_site.update(params.config['configurations']['hive-interactive-site'])
  for item in exclude_list:
    if item in merged_hive_interactive_site.keys():
      del merged_hive_interactive_site[item]

  '''
  Hive2 doesn't have support for Atlas, we need to remove the Hook 'org.apache.atlas.hive.hook.HiveHook',
  which would have come in config 'hive.exec.post.hooks' during the site merge logic, if Atlas is installed.
  '''
  remove_atlas_hook_if_exists(merged_hive_interactive_site)

  '''
  As tez_hive2/tez-site.xml only contains the new + the changed props compared to tez/tez-site.xml,
  we need to merge tez/tez-site.xml and tez_hive2/tez-site.xml and store it in tez_hive2/tez-site.xml.
  '''
  merged_tez_interactive_site = {}
  if 'tez-site' in params.config['configurations']:
    merged_tez_interactive_site.update(params.config['configurations']['tez-site'])
    Logger.info("Retrieved 'tez/tez-site' for merging with 'tez_hive2/tez-interactive-site'.")
  else:
    Logger.error("Tez's 'tez-site' couldn't be retrieved from passed-in configurations.")

  merged_tez_interactive_site.update(params.config['configurations']['tez-interactive-site'])
  XmlConfig("tez-site.xml",
            conf_dir = params.tez_interactive_config_dir,
            configurations = merged_tez_interactive_site,
            configuration_attributes=params.config['configuration_attributes']['tez-interactive-site'],
            owner = params.tez_interactive_user,
            group = params.user_group,
            mode = 0664)

  '''
  Merge properties from hiveserver2-interactive-site into hiveserver2-site
  '''
  merged_hiveserver2_interactive_site = {}
  if 'hiveserver2-site' in params.config['configurations']:
    merged_hiveserver2_interactive_site.update(params.config['configurations']['hiveserver2-site'])
    Logger.info("Retrieved 'hiveserver2-site' for merging with 'hiveserver2-interactive-site'.")
  else:
    Logger.error("'hiveserver2-site' couldn't be retrieved from passed-in configurations.")
  merged_hiveserver2_interactive_site.update(params.config['configurations']['hiveserver2-interactive-site'])


  # Create config files under /etc/hive2/conf and /etc/hive2/conf/conf.server:
  #   hive-site.xml
  #   hive-env.sh
  #   llap-daemon-log4j2.properties
  #   llap-cli-log4j2.properties
  #   hive-log4j2.properties
  #   hive-exec-log4j2.properties
  #   beeline-log4j2.properties

  hive2_conf_dirs_list = params.hive_conf_dirs_list
  hive2_client_conf_path = format("{stack_root}/current/{component_directory}/conf")

  # Making copy of 'merged_hive_interactive_site' in 'merged_hive_interactive_site_copy', and deleting 'javax.jdo.option.ConnectionPassword'
  # config from there, as Hive2 client shouldn't have that config.
  merged_hive_interactive_site_copy = merged_hive_interactive_site.copy()
  for item in exclude_list_for_hive2_client:
    if item in merged_hive_interactive_site.keys():
      del merged_hive_interactive_site_copy[item]

  for conf_dir in hive2_conf_dirs_list:
      if conf_dir == hive2_client_conf_path:
        XmlConfig("hive-site.xml",
                  conf_dir=conf_dir,
                  configurations=merged_hive_interactive_site_copy,
                  configuration_attributes=params.config['configuration_attributes']['hive-interactive-site'],
                  owner=params.hive_user,
                  group=params.user_group,
                  mode=0644)
      else:
        XmlConfig("hive-site.xml",
                  conf_dir=conf_dir,
                  configurations=merged_hive_interactive_site,
                  configuration_attributes=params.config['configuration_attributes']['hive-interactive-site'],
                  owner=params.hive_user,
                  group=params.user_group,
                  mode=0644)

      XmlConfig("hiveserver2-site.xml",
                conf_dir=conf_dir,
                configurations=merged_hiveserver2_interactive_site,
                configuration_attributes=params.config['configuration_attributes']['hiveserver2-interactive-site'],
                owner=params.hive_user,
                group=params.user_group,
                mode=0644)

      hive_server_interactive_conf_dir = conf_dir

      File(format("{hive_server_interactive_conf_dir}/hive-env.sh"),
           owner=params.hive_user,
           group=params.user_group,
           content=InlineTemplate(params.hive_interactive_env_sh_template))

      llap_daemon_log4j_filename = 'llap-daemon-log4j2.properties'
      File(format("{hive_server_interactive_conf_dir}/{llap_daemon_log4j_filename}"),
           mode=0644,
           group=params.user_group,
           owner=params.hive_user,
           content=params.llap_daemon_log4j)

      llap_cli_log4j2_filename = 'llap-cli-log4j2.properties'
      File(format("{hive_server_interactive_conf_dir}/{llap_cli_log4j2_filename}"),
           mode=0644,
           group=params.user_group,
           owner=params.hive_user,
           content=params.llap_cli_log4j2)

      hive_log4j2_filename = 'hive-log4j2.properties'
      File(format("{hive_server_interactive_conf_dir}/{hive_log4j2_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=params.hive_log4j2)

      hive_exec_log4j2_filename = 'hive-exec-log4j2.properties'
      File(format("{hive_server_interactive_conf_dir}/{hive_exec_log4j2_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=params.hive_exec_log4j2)

      beeline_log4j2_filename = 'beeline-log4j2.properties'
      File(format("{hive_server_interactive_conf_dir}/{beeline_log4j2_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=params.beeline_log4j2)

      File(os.path.join(hive_server_interactive_conf_dir, "hadoop-metrics2-hiveserver2.properties"),
           owner=params.hive_user,
           group=params.user_group,
           content=Template("hadoop-metrics2-hiveserver2.properties.j2")
           )

      File(format("{hive_server_interactive_conf_dir}/hadoop-metrics2-llapdaemon.properties"),
           owner=params.hive_user,
           group=params.user_group,
           content=Template("hadoop-metrics2-llapdaemon.j2"))

      File(format("{hive_server_interactive_conf_dir}/hadoop-metrics2-llaptaskscheduler.properties"),
           owner=params.hive_user,
           group=params.user_group,
           content=Template("hadoop-metrics2-llaptaskscheduler.j2"))


  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            create_parents = True,
            owner='root',
            group='root')

  File(os.path.join(params.limits_conf_dir, 'hive.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hive.conf.j2"))

  if not os.path.exists(params.target_hive_interactive):
    jdbc_connector(params.target_hive_interactive, params.hive_intaractive_previous_jdbc_jar)

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
       content = DownloadSource(format("{jdk_location}/{check_db_connection_jar_name}")),
       mode = 0644)
  File(params.start_hiveserver2_interactive_path,
       mode=0755,
       content=Template(format('{start_hiveserver2_interactive_script}')))

  Directory(params.hive_pid_dir,
            create_parents=True,
            cd_access='a',
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)
  Directory(params.hive_log_dir,
            create_parents=True,
            cd_access='a',
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)
  Directory(params.hive_interactive_var_lib,
            create_parents=True,
            cd_access='a',
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)

"""
Remove 'org.apache.atlas.hive.hook.HiveHook' value from Hive2/hive-site.xml config 'hive.exec.post.hooks', if exists.
"""
def remove_atlas_hook_if_exists(merged_hive_interactive_site):
  if 'hive.exec.post.hooks' in merged_hive_interactive_site.keys():
    existing_hive_exec_post_hooks = merged_hive_interactive_site.get('hive.exec.post.hooks')
    if existing_hive_exec_post_hooks:
      hook_splits = existing_hive_exec_post_hooks.split(",")
      updated_hook_splits = [hook for hook in hook_splits if not hook.strip() == 'org.apache.atlas.hive.hook.HiveHook']
      updated_hooks_str = ",".join((str(hook)).strip() for hook in updated_hook_splits)
      if updated_hooks_str != existing_hive_exec_post_hooks:
        merged_hive_interactive_site['hive.exec.post.hooks'] = updated_hooks_str
        Logger.info("Updated Hive2/hive-site.xml 'hive.exec.post.hooks' value from : '{0}' to : '{1}'"
                    .format(existing_hive_exec_post_hooks, updated_hooks_str))
      else:
        Logger.info("No change done to Hive2/hive-site.xml 'hive.exec.post.hooks' value.")
  else:
      Logger.debug("'hive.exec.post.hooks' doesn't exist in Hive2/hive-site.xml")
