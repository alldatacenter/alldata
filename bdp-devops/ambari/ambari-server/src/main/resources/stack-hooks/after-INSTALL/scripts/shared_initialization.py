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
import os

import ambari_simplejson as json
from ambari_jinja2 import Environment as JinjaEnvironment
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.fcntl_based_process_lock import FcntlBasedProcessLock
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script import Script


def setup_stack_symlinks(struct_out_file):
  """
  Invokes <stack-selector-tool> set all against a calculated fully-qualified, "normalized" version based on a
  stack version, such as "2.3". This should always be called after a component has been
  installed to ensure that all HDP pointers are correct. The stack upgrade logic does not
  interact with this since it's done via a custom command and will not trigger this hook.
  :return:
  """
  import params
  if params.upgrade_suspended:
    Logger.warning("Skipping running stack-selector-tool because there is a suspended upgrade")
    return

  if params.host_sys_prepped:
    Logger.warning("Skipping running stack-selector-tool because this is a sys_prepped host. This may cause symlink pointers not to be created for HDP components installed later on top of an already sys_prepped host")
    return

  # get the packages which the stack-select tool should be used on
  stack_packages = stack_select.get_packages(stack_select.PACKAGE_SCOPE_INSTALL)
  if stack_packages is None:
    return

  json_version = load_version(struct_out_file)

  if not json_version:
    Logger.info("There is no advertised version for this component stored in {0}".format(struct_out_file))
    return

  # On parallel command execution this should be executed by a single process at a time.
  with FcntlBasedProcessLock(params.stack_select_lock_file, enabled = params.is_parallel_execution_enabled, skip_fcntl_failures = True):
    for package in stack_packages:
      stack_select.select(package, json_version)


def setup_config():
  import params
  stackversion = params.stack_version_unformatted
  Logger.info("FS Type: {0}".format(params.dfs_type))

  is_hadoop_conf_dir_present = False
  if hasattr(params, "hadoop_conf_dir") and params.hadoop_conf_dir is not None and os.path.exists(params.hadoop_conf_dir):
    is_hadoop_conf_dir_present = True
  else:
    Logger.warning("Parameter hadoop_conf_dir is missing or directory does not exist. This is expected if this host does not have any Hadoop components.")

  if is_hadoop_conf_dir_present and (params.has_namenode or stackversion.find('Gluster') >= 0 or params.dfs_type == 'HCFS'):
    # create core-site only if the hadoop config diretory exists
    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              configuration_attributes=params.config['configurationAttributes']['core-site'],
              owner=params.hdfs_user,
              group=params.user_group,
              only_if=format("ls {hadoop_conf_dir}"),
              xml_include_file=params.mount_table_xml_inclusion_file_full_path
              )

    if params.mount_table_content:
      File(os.path.join(params.hadoop_conf_dir, params.xml_inclusion_file_name),
           owner=params.hdfs_user,
           group=params.user_group,
           content=params.mount_table_content
           )

  Directory(params.logsearch_logfeeder_conf,
            mode=0755,
            cd_access='a',
            create_parents=True
            )

  if params.logsearch_config_file_exists:
    File(format("{logsearch_logfeeder_conf}/" + params.logsearch_config_file_name),
         content=Template(params.logsearch_config_file_path,extra_imports=[default])
         )
  else:
    Logger.warning('No logsearch configuration exists at ' + params.logsearch_config_file_path)


def load_version(struct_out_file):
  """
  Load version from file.  Made a separate method for testing
  """
  try:
    with open(struct_out_file, 'r') as fp:
      json_info = json.load(fp)

    return json_info['version']
  except (IOError, KeyError, TypeError):
    return None


def link_configs(struct_out_file):
  """
  Use the conf_select module to link configuration directories correctly.
  """
  import params

  json_version = load_version(struct_out_file)

  if not json_version:
    Logger.info("Could not load 'version' from {0}".format(struct_out_file))
    return

  # On parallel command execution this should be executed by a single process at a time.
  with FcntlBasedProcessLock(params.link_configs_lock_file, enabled = params.is_parallel_execution_enabled, skip_fcntl_failures = True):
    for package_name, directories in conf_select.get_package_dirs().iteritems():
      conf_select.convert_conf_directories_to_symlinks(package_name, json_version, directories)