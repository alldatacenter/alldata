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

# Python Imports
import os

# Ambari Common and Resource Management Imports
from resource_management.libraries.script.script import Script
from resource_management.core.resources.service import ServiceConfig
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.core.logger import Logger
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

from resource_management.libraries.functions.mounted_dirs_helper import handle_mounted_dirs

# Local Imports


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def yarn(name = None):
  import params
  XmlConfig("mapred-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['mapred-site'],
            owner=params.yarn_user,
            mode='f'
  )
  XmlConfig("yarn-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['yarn-site'],
            owner=params.yarn_user,
            mode='f',
            configuration_attributes=params.config['configuration_attributes']['yarn-site']
  )
  XmlConfig("capacity-scheduler.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['capacity-scheduler'],
            owner=params.yarn_user,
            mode='f'
  )

  if params.service_map.has_key(name):
    service_name = params.service_map[name]

    ServiceConfig(service_name,
                  action="change_user",
                  username = params.yarn_user,
                  password = Script.get_password(params.yarn_user))

def create_log_dir(dir_name):
  import params
  Directory(dir_name,
            create_parents = True,
            cd_access="a",
            mode=0775,
            owner=params.yarn_user,
            group=params.user_group,
            ignore_failures=True,
  )
  
def create_local_dir(dir_name):
  import params
  Directory(dir_name,
            create_parents = True,
            cd_access="a",
            mode=0755,
            owner=params.yarn_user,
            group=params.user_group,
            ignore_failures=True,
            recursive_mode_flags = {'f': 'a+rw', 'd': 'a+rwx'},
  )

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def yarn(name=None, config_dir=None):
  """
  :param name: Component name, apptimelineserver, nodemanager, resourcemanager, or None (defaults for client)
  :param config_dir: Which config directory to write configs to, which could be different during rolling upgrade.
  """
  import params

  if config_dir is None:
    config_dir = params.hadoop_conf_dir

  if name == "historyserver":
    if params.yarn_log_aggregation_enabled:
      params.HdfsResource(params.yarn_nm_app_log_dir,
                           action="create_on_execute",
                           type="directory",
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0777,
                           recursive_chmod=True
      )

    # create the /tmp folder with proper permissions if it doesn't exist yet
    if params.entity_file_history_directory.startswith('/tmp'):
        params.HdfsResource(params.hdfs_tmp_dir,
                            action="create_on_execute",
                            type="directory",
                            owner=params.hdfs_user,
                            mode=0777,
        )

    params.HdfsResource(params.entity_file_history_directory,
                           action="create_on_execute",
                           type="directory",
                           owner=params.yarn_user,
                           group=params.user_group
    )
    params.HdfsResource("/mapred",
                         type="directory",
                         action="create_on_execute",
                         owner=params.mapred_user
    )
    params.HdfsResource("/mapred/system",
                         type="directory",
                         action="create_on_execute",
                         owner=params.hdfs_user
    )
    params.HdfsResource(params.mapreduce_jobhistory_done_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.mapred_user,
                         group=params.user_group,
                         change_permissions_for_parents=True,
                         mode=0777
    )
    params.HdfsResource(None, action="execute")
    Directory(params.jhs_leveldb_state_store_dir,
              owner=params.mapred_user,
              group=params.user_group,
              create_parents = True,
              cd_access="a",
              recursive_ownership = True,
              )

  #<editor-fold desc="Node Manager Section">
  if name == "nodemanager":

    # First start after enabling/disabling security
    if params.toggle_nm_security:
      Directory(params.nm_local_dirs_list + params.nm_log_dirs_list,
                action='delete'
      )

      # If yarn.nodemanager.recovery.dir exists, remove this dir
      if params.yarn_nodemanager_recovery_dir:
        Directory(InlineTemplate(params.yarn_nodemanager_recovery_dir).get_content(),
                  action='delete'
        )

      # Setting NM marker file
      if params.security_enabled:
        Directory(params.nm_security_marker_dir)
        File(params.nm_security_marker,
             content="Marker file to track first start after enabling/disabling security. "
                     "During first start yarn local, log dirs are removed and recreated"
             )
      elif not params.security_enabled:
        File(params.nm_security_marker, action="delete")


    if not params.security_enabled or params.toggle_nm_security:
      # handle_mounted_dirs ensures that we don't create dirs which are temporary unavailable (unmounted), and intended to reside on a different mount.
      nm_log_dir_to_mount_file_content = handle_mounted_dirs(create_log_dir, params.nm_log_dirs, params.nm_log_dir_to_mount_file, params)
      # create a history file used by handle_mounted_dirs
      File(params.nm_log_dir_to_mount_file,
           owner=params.hdfs_user,
           group=params.user_group,
           mode=0644,
           content=nm_log_dir_to_mount_file_content
      )
      nm_local_dir_to_mount_file_content = handle_mounted_dirs(create_local_dir, params.nm_local_dirs, params.nm_local_dir_to_mount_file, params)
      File(params.nm_local_dir_to_mount_file,
           owner=params.hdfs_user,
           group=params.user_group,
           mode=0644,
           content=nm_local_dir_to_mount_file_content
      )
  #</editor-fold>

  if params.yarn_nodemanager_recovery_dir:
    Directory(InlineTemplate(params.yarn_nodemanager_recovery_dir).get_content(),
              owner=params.yarn_user,
              group=params.user_group,
              create_parents = True,
              mode=0755,
              cd_access = 'a',
    )

  Directory([params.yarn_pid_dir_prefix, params.yarn_pid_dir, params.yarn_log_dir],
            owner=params.yarn_user,
            group=params.user_group,
            create_parents = True,
            cd_access = 'a',
  )

  Directory([params.mapred_pid_dir_prefix, params.mapred_pid_dir, params.mapred_log_dir_prefix, params.mapred_log_dir],
            owner=params.mapred_user,
            group=params.user_group,
            create_parents = True,
            cd_access = 'a',
  )
  Directory([params.yarn_log_dir_prefix],
            owner=params.yarn_user,
            group=params.user_group,
            create_parents = True,
            ignore_failures=True,
            cd_access = 'a',
  )

  XmlConfig("core-site.xml",
            conf_dir=config_dir,
            configurations=params.config['configurations']['core-site'],
            configuration_attributes=params.config['configuration_attributes']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group,
            mode=0644
  )

  # During RU, Core Masters and Slaves need hdfs-site.xml
  # TODO, instead of specifying individual configs, which is susceptible to breaking when new configs are added,
  # RU should rely on all available in <stack-root>/<version>/hadoop/conf
  if 'hdfs-site' in params.config['configurations']:
    XmlConfig("hdfs-site.xml",
              conf_dir=config_dir,
              configurations=params.config['configurations']['hdfs-site'],
              configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
              owner=params.hdfs_user,
              group=params.user_group,
              mode=0644
    )

  XmlConfig("mapred-site.xml",
            conf_dir=config_dir,
            configurations=params.config['configurations']['mapred-site'],
            configuration_attributes=params.config['configuration_attributes']['mapred-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("yarn-site.xml",
            conf_dir=config_dir,
            configurations=params.config['configurations']['yarn-site'],
            configuration_attributes=params.config['configuration_attributes']['yarn-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("capacity-scheduler.xml",
            conf_dir=config_dir,
            configurations=params.config['configurations']['capacity-scheduler'],
            configuration_attributes=params.config['configuration_attributes']['capacity-scheduler'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  if name == 'resourcemanager':
    Directory(params.rm_nodes_exclude_dir,
         mode=0755,
         create_parents=True,
         cd_access='a',
    )
    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=params.yarn_user,
         group=params.user_group
    )
    if params.include_hosts:
      Directory(params.rm_nodes_include_dir,
        mode=0755,
        create_parents=True,
        cd_access='a',
      )
      File(params.include_file_path,
        content=Template("include_hosts_list.j2"),
        owner=params.yarn_user,
        group=params.user_group
      )
    File(params.yarn_job_summary_log,
       owner=params.yarn_user,
       group=params.user_group
    )
    if not is_empty(params.node_label_enable) and params.node_label_enable or is_empty(params.node_label_enable) and params.node_labels_dir:
      params.HdfsResource(params.node_labels_dir,
                           type="directory",
                           action="create_on_execute",
                           change_permissions_for_parents=True,
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0700
      )
      params.HdfsResource(None, action="execute")


  elif name == 'apptimelineserver':
    Directory(params.ats_leveldb_dir,
       owner=params.yarn_user,
       group=params.user_group,
       create_parents = True,
       cd_access="a",
    )

    # if stack support application timeline-service state store property (timeline_state_store stack feature)
    if params.stack_supports_timeline_state_store:
      Directory(params.ats_leveldb_state_store_dir,
       owner=params.yarn_user,
       group=params.user_group,
       create_parents = True,
       cd_access="a",
      )
    # app timeline server 1.5 directories
    if not is_empty(params.entity_groupfs_store_dir):
      parent_path = os.path.dirname(os.path.abspath(params.entity_groupfs_store_dir))
      params.HdfsResource(parent_path,
                          type="directory",
                          action="create_on_execute",
                          change_permissions_for_parents=True,
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=0755
                          )
      params.HdfsResource(params.entity_groupfs_store_dir,
                          type="directory",
                          action="create_on_execute",
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=params.entity_groupfs_store_dir_mode
                          )
    if not is_empty(params.entity_groupfs_active_dir):
      parent_path = os.path.dirname(os.path.abspath(params.entity_groupfs_active_dir))
      params.HdfsResource(parent_path,
                          type="directory",
                          action="create_on_execute",
                          change_permissions_for_parents=True,
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=0755
                          )
      params.HdfsResource(params.entity_groupfs_active_dir,
                          type="directory",
                          action="create_on_execute",
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=params.entity_groupfs_active_dir_mode
                          )
    params.HdfsResource(None, action="execute")

  File(format("{limits_conf_dir}/yarn.conf"),
       mode=0644,
       content=Template('yarn.conf.j2')
  )

  File(format("{limits_conf_dir}/mapreduce.conf"),
       mode=0644,
       content=Template('mapreduce.conf.j2')
  )

  File(os.path.join(config_dir, "yarn-env.sh"),
       owner=params.yarn_user,
       group=params.user_group,
       mode=0755,
       content=InlineTemplate(params.yarn_env_sh_template)
  )

  container_executor = format("{yarn_container_bin}/container-executor")
  File(container_executor,
      group=params.yarn_executor_container_group,
      mode=params.container_executor_mode
  )

  File(os.path.join(config_dir, "container-executor.cfg"),
      group=params.user_group,
      mode=0644,
      content=Template('container-executor.cfg.j2')
  )

  Directory(params.cgroups_dir,
            group=params.user_group,
            create_parents = True,
            mode=0755,
            cd_access="a")

  if params.security_enabled:
    tc_mode = 0644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  File(os.path.join(config_dir, "mapred-env.sh"),
       owner=tc_owner,
       mode=0755,
       content=InlineTemplate(params.mapred_env_sh_template)
  )

  if params.security_enabled:
    File(os.path.join(params.hadoop_bin, "task-controller"),
         owner="root",
         group=params.mapred_tt_group,
         mode=06050
    )
    File(os.path.join(config_dir, 'taskcontroller.cfg'),
         owner = tc_owner,
         mode = tc_mode,
         group = params.mapred_tt_group,
         content=Template("taskcontroller.cfg.j2")
    )
  else:
    File(os.path.join(config_dir, 'taskcontroller.cfg'),
         owner=tc_owner,
         content=Template("taskcontroller.cfg.j2")
    )

  if "mapred-site" in params.config['configurations']:
    XmlConfig("mapred-site.xml",
              conf_dir=config_dir,
              configurations=params.config['configurations']['mapred-site'],
              configuration_attributes=params.config['configuration_attributes']['mapred-site'],
              owner=params.mapred_user,
              group=params.user_group
    )

  if "capacity-scheduler" in params.config['configurations']:
    XmlConfig("capacity-scheduler.xml",
              conf_dir=config_dir,
              configurations=params.config['configurations'][
                'capacity-scheduler'],
              configuration_attributes=params.config['configuration_attributes']['capacity-scheduler'],
              owner=params.hdfs_user,
              group=params.user_group
    )
  if "ssl-client" in params.config['configurations']:
    XmlConfig("ssl-client.xml",
              conf_dir=config_dir,
              configurations=params.config['configurations']['ssl-client'],
              configuration_attributes=params.config['configuration_attributes']['ssl-client'],
              owner=params.hdfs_user,
              group=params.user_group
    )

    Directory(params.hadoop_conf_secure_dir,
              create_parents = True,
              owner='root',
              group=params.user_group,
              cd_access='a',
              )

    XmlConfig("ssl-client.xml",
              conf_dir=params.hadoop_conf_secure_dir,
              configurations=params.config['configurations']['ssl-client'],
              configuration_attributes=params.config['configuration_attributes']['ssl-client'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  if "ssl-server" in params.config['configurations']:
    XmlConfig("ssl-server.xml",
              conf_dir=config_dir,
              configurations=params.config['configurations']['ssl-server'],
              configuration_attributes=params.config['configuration_attributes']['ssl-server'],
              owner=params.hdfs_user,
              group=params.user_group
    )
  if os.path.exists(os.path.join(config_dir, 'fair-scheduler.xml')):
    File(os.path.join(config_dir, 'fair-scheduler.xml'),
         owner=params.mapred_user,
         group=params.user_group
    )

  if os.path.exists(
    os.path.join(config_dir, 'ssl-client.xml.example')):
    File(os.path.join(config_dir, 'ssl-client.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )

  if os.path.exists(
    os.path.join(config_dir, 'ssl-server.xml.example')):
    File(os.path.join(config_dir, 'ssl-server.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )
