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

from resource_management.core.resources import Directory
from resource_management.core.resources import Execute
from resource_management.libraries.functions import default
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import format


DEFAULT_HADOOP_HDFS_EXTENSION_DIR = "/hdp/ext/{0}/hadoop"
DEFAULT_HADOOP_HIVE_EXTENSION_DIR = "/hdp/ext/{0}/hive"
DEFAULT_HADOOP_HBASE_EXTENSION_DIR = "/hdp/ext/{0}/hbase"

def setup_extensions():
  """
  The goal of this method is to distribute extensions (for example jar files) from
  HDFS (/hdp/ext/{major_stack_version}/{service_name}) to all nodes which contain related
  components of service (YARN, HIVE or HBASE). Extensions should be added to HDFS by
  user manually.
  """

  import params

  # Hadoop Custom extensions
  hadoop_custom_extensions_enabled = default("/configurations/core-site/hadoop.custom-extensions.enabled", False)
  hadoop_custom_extensions_services = default("/configurations/core-site/hadoop.custom-extensions.services", "")
  hadoop_custom_extensions_owner = default("/configurations/core-site/hadoop.custom-extensions.owner", params.hdfs_user)
  hadoop_custom_extensions_hdfs_dir = get_config_formatted_value(default("/configurations/core-site/hadoop.custom-extensions.root",
                                                 DEFAULT_HADOOP_HDFS_EXTENSION_DIR.format(params.major_stack_version)))
  hadoop_custom_extensions_services = [ service.strip().upper() for service in hadoop_custom_extensions_services.split(",") ]
  hadoop_custom_extensions_services.append("YARN")

  hadoop_custom_extensions_local_dir = "{0}/current/ext/hadoop".format(Script.get_stack_root())

  if params.current_service in hadoop_custom_extensions_services:
    clean_extensions(hadoop_custom_extensions_local_dir)
    if hadoop_custom_extensions_enabled:
      download_extensions(hadoop_custom_extensions_owner, params.user_group,
                          hadoop_custom_extensions_hdfs_dir,
                          hadoop_custom_extensions_local_dir)

  setup_extensions_hive()

  hbase_custom_extensions_services = []
  hbase_custom_extensions_services.append("HBASE")
  if params.current_service in hbase_custom_extensions_services:
    setup_hbase_extensions()


def setup_hbase_extensions():
  import params

  # HBase Custom extensions
  hbase_custom_extensions_enabled = default("/configurations/hbase-site/hbase.custom-extensions.enabled", False)
  hbase_custom_extensions_owner = default("/configurations/hbase-site/hbase.custom-extensions.owner", params.hdfs_user)
  hbase_custom_extensions_hdfs_dir = get_config_formatted_value(default("/configurations/hbase-site/hbase.custom-extensions.root",
                                                DEFAULT_HADOOP_HBASE_EXTENSION_DIR.format(params.major_stack_version)))
  hbase_custom_extensions_local_dir = "{0}/current/ext/hbase".format(Script.get_stack_root())

  impacted_components = ['HBASE_MASTER', 'HBASE_REGIONSERVER', 'PHOENIX_QUERY_SERVER'];
  role = params.config.get('role','')

  if role in impacted_components:
    clean_extensions(hbase_custom_extensions_local_dir)
    if hbase_custom_extensions_enabled:
      download_extensions(hbase_custom_extensions_owner, params.user_group,
                          hbase_custom_extensions_hdfs_dir,
                          hbase_custom_extensions_local_dir)


def setup_extensions_hive():
  import params

  hive_custom_extensions_enabled = default("/configurations/hive-site/hive.custom-extensions.enabled", False)
  hive_custom_extensions_owner = default("/configurations/hive-site/hive.custom-extensions.owner", params.hdfs_user)
  hive_custom_extensions_hdfs_dir = DEFAULT_HADOOP_HIVE_EXTENSION_DIR.format(params.major_stack_version)

  hive_custom_extensions_local_dir = "{0}/current/ext/hive".format(Script.get_stack_root())

  impacted_components = ['HIVE_SERVER', 'HIVE_CLIENT'];
  role = params.config.get('role','')

  # Run copying for HIVE_SERVER and HIVE_CLIENT
  if params.current_service == 'HIVE' and role in impacted_components:
    clean_extensions(hive_custom_extensions_local_dir)
    if hive_custom_extensions_enabled:
      download_extensions(hive_custom_extensions_owner, params.user_group,
                          hive_custom_extensions_hdfs_dir,
                          hive_custom_extensions_local_dir)

def download_extensions(owner_user, owner_group, hdfs_source_dir, local_target_dir):
  """
  :param owner_user: user owner of the HDFS directory
  :param owner_group: group owner of the HDFS directory
  :param hdfs_source_dir: the HDFS directory from where the files are being pull
  :param local_target_dir: the location of where to download the files
  :return: Will return True if successful, otherwise, False.
  """
  import params

  if not os.path.isdir(local_target_dir):
    extensions_tmp_dir=format("{tmp_dir}/custom_extensions")
    Directory(local_target_dir,
              owner="root",
              mode=0755,
              group="root",
              create_parents=True)

    params.HdfsResource(hdfs_source_dir,
                        type="directory",
                        action="create_on_execute",
                        owner=owner_user,
                        group=owner_group,
                        mode=0755)

    Directory(extensions_tmp_dir,
              owner=params.hdfs_user,
              mode=0755,
              create_parents=True)

    # copy from hdfs to /tmp
    params.HdfsResource(extensions_tmp_dir,
                        type="directory",
                        action="download_on_execute",
                        source=hdfs_source_dir,
                        user=params.hdfs_user,
                        mode=0644,
                        replace_existing_files=True)

    # Execute command is not quoting correctly.
    cmd = format("{sudo} mv {extensions_tmp_dir}/* {local_target_dir}")
    only_if_cmd = "ls -d {extensions_tmp_dir}/*".format(extensions_tmp_dir=extensions_tmp_dir)
    Execute(cmd, only_if=only_if_cmd)

    only_if_local = 'ls -d "{local_target_dir}"'.format(local_target_dir=local_target_dir)
    Execute(("chown", "-R", "root:root", local_target_dir),
            sudo=True,
            only_if=only_if_local)

    params.HdfsResource(None,action="execute")
  return True

def clean_extensions(local_dir):
  """
  :param local_dir: The local directory where the extensions are stored.
  :return: Will return True if successful, otherwise, False.
  """
  if os.path.isdir(local_dir):
    Directory(local_dir,
              action="delete")
  return True

def get_config_formatted_value(property_value):
  return format(property_value.replace("{{", "{").replace("}}", "}"))
