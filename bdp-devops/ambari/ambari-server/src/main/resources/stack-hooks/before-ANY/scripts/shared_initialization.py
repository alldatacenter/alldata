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
import re
import getpass
import tempfile
from copy import copy
from resource_management.libraries.functions.version import compare_versions
from resource_management import *
from resource_management.core import shell

def setup_users():
  """
  Creates users before cluster installation
  """
  import params

  should_create_users_and_groups = False
  if params.host_sys_prepped:
    should_create_users_and_groups = not params.sysprep_skip_create_users_and_groups
  else:
    should_create_users_and_groups = not params.ignore_groupsusers_create

  if should_create_users_and_groups:
    for group in params.group_list:
      Group(group,
      )

    for user in params.user_list:
      User(user,
           uid = get_uid(user) if params.override_uid == "true" else None,
           gid = params.user_to_gid_dict[user],
           groups = params.user_to_groups_dict[user],
           fetch_nonlocal_groups = params.fetch_nonlocal_groups,
           )

    if params.override_uid == "true":
      set_uid(params.smoke_user, params.smoke_user_dirs)
    else:
      Logger.info('Skipping setting uid for smoke user as host is sys prepped')
  else:
    Logger.info('Skipping creation of User and Group as host is sys prepped or ignore_groupsusers_create flag is on')
    pass


  if params.has_hbase_masters:
    Directory (params.hbase_tmp_dir,
               owner = params.hbase_user,
               mode=0775,
               create_parents = True,
               cd_access="a",
    )

    if params.override_uid == "true":
      set_uid(params.hbase_user, params.hbase_user_dirs)
    else:
      Logger.info('Skipping setting uid for hbase user as host is sys prepped')

  if should_create_users_and_groups:
    if params.has_namenode:
      create_dfs_cluster_admins()
    if params.has_tez and params.stack_version_formatted != "" and compare_versions(params.stack_version_formatted, '2.3') >= 0:
      create_tez_am_view_acls()
  else:
    Logger.info('Skipping setting dfs cluster admin and tez view acls as host is sys prepped')

def create_dfs_cluster_admins():
  """
  dfs.cluster.administrators support format <comma-delimited list of usernames><space><comma-delimited list of group names>
  """
  import params

  groups_list = create_users_and_groups(params.dfs_cluster_administrators_group)

  User(params.hdfs_user,
    groups = params.user_to_groups_dict[params.hdfs_user] + groups_list,
    fetch_nonlocal_groups = params.fetch_nonlocal_groups
  )

def create_tez_am_view_acls():

  """
  tez.am.view-acls support format <comma-delimited list of usernames><space><comma-delimited list of group names>
  """
  import params

  if not params.tez_am_view_acls.startswith("*"):
    create_users_and_groups(params.tez_am_view_acls)

def create_users_and_groups(user_and_groups):

  import params

  parts = re.split('\s+', user_and_groups)
  if len(parts) == 1:
    parts.append("")

  users_list = parts[0].strip(",").split(",") if parts[0] else []
  groups_list = parts[1].strip(",").split(",") if parts[1] else []

  # skip creating groups and users if * is provided as value.
  users_list = filter(lambda x: x != '*' , users_list)
  groups_list = filter(lambda x: x != '*' , groups_list)

  if users_list:
    User(users_list,
          fetch_nonlocal_groups = params.fetch_nonlocal_groups
    )

  if groups_list:
    Group(copy(groups_list),
    )
  return groups_list

def set_uid(user, user_dirs):
  """
  user_dirs - comma separated directories
  """
  import params

  File(format("{tmp_dir}/changeUid.sh"),
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
  ignore_groupsusers_create_str = str(params.ignore_groupsusers_create).lower()
  uid = get_uid(user, return_existing=True)
  Execute(format("{tmp_dir}/changeUid.sh {user} {user_dirs} {new_uid}", new_uid=0 if uid is None else uid),
          not_if = format("(test $(id -u {user}) -gt 1000) || ({ignore_groupsusers_create_str})"))

def get_uid(user, return_existing=False):
  """
  Tries to get UID for username. It will try to find UID in custom properties in *cluster_env* and, if *return_existing=True*,
  it will try to return UID of existing *user*.

  :param user: username to get UID for
  :param return_existing: return UID for existing user
  :return:
  """
  import params
  user_str = str(user) + "_uid"
  service_env = [ serviceEnv for serviceEnv in params.config['configurations'] if user_str in params.config['configurations'][serviceEnv]]

  if service_env and params.config['configurations'][service_env[0]][user_str]:
    service_env_str = str(service_env[0])
    uid = params.config['configurations'][service_env_str][user_str]
    if len(service_env) > 1:
      Logger.warning("Multiple values found for %s, using %s"  % (user_str, uid))
    return uid
  else:
    if return_existing:
      # pick up existing UID or try to find available UID in /etc/passwd, see changeToSecureUid.sh for more info
      if user == params.smoke_user:
        return None
      File(format("{tmp_dir}/changeUid.sh"),
           content=StaticFile("changeToSecureUid.sh"),
           mode=0555)
      code, newUid = shell.call(format("{tmp_dir}/changeUid.sh {user}"))
      return int(newUid)
    else:
      # do not return UID for existing user, used in User resource call to let OS to choose UID for us
      return None

def setup_hadoop_env():
  import params
  stackversion = params.stack_version_unformatted
  Logger.info("FS Type: {0}".format(params.dfs_type))
  if params.has_namenode or stackversion.find('Gluster') >= 0 or params.dfs_type == 'HCFS':
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user

    # create /etc/hadoop
    Directory(params.hadoop_dir, mode=0755)

    # write out hadoop-env.sh, but only if the directory exists
    if os.path.exists(params.hadoop_conf_dir):
      File(os.path.join(params.hadoop_conf_dir, 'hadoop-env.sh'), owner=tc_owner,
        group=params.user_group,
        content=InlineTemplate(params.hadoop_env_sh_template))
        
def setup_env():
  import params
  # Create tmp dir for java.io.tmpdir
  # Handle a situation when /tmp is set to noexec
  Directory(params.hadoop_java_io_tmpdir,
            owner=params.hdfs_user if params.has_namenode else None,
            group=params.user_group,
            mode=01777
  )

def setup_java():
  """
  Install jdk using specific params.
  Install ambari jdk as well if the stack and ambari jdk are different.
  """
  import params
  __setup_java(custom_java_home=params.java_home, custom_jdk_name=params.jdk_name)
  if params.ambari_java_home and params.ambari_java_home != params.java_home:
    __setup_java(custom_java_home=params.ambari_java_home, custom_jdk_name=params.ambari_jdk_name)

def __setup_java(custom_java_home, custom_jdk_name):
  """
  Installs jdk using specific params, that comes from ambari-server
  """
  import params
  java_exec = format("{custom_java_home}/bin/java")

  if not os.path.isfile(java_exec):
    if not params.jdk_name: # if custom jdk is used.
      raise Fail(format("Unable to access {java_exec}. Confirm you have copied jdk to this host."))

    jdk_curl_target = format("{tmp_dir}/{custom_jdk_name}")
    java_dir = os.path.dirname(params.java_home)

    Directory(params.artifact_dir,
              create_parents = True,
              )

    File(jdk_curl_target,
         content = DownloadSource(format("{jdk_location}/{custom_jdk_name}")),
         not_if = format("test -f {jdk_curl_target}")
         )

    File(jdk_curl_target,
         mode = 0755,
         )

    tmp_java_dir = tempfile.mkdtemp(prefix="jdk_tmp_", dir=params.tmp_dir)

    try:
      if params.jdk_name.endswith(".bin"):
        chmod_cmd = ("chmod", "+x", jdk_curl_target)
        install_cmd = format("cd {tmp_java_dir} && echo A | {jdk_curl_target} -noregister && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")
      elif params.jdk_name.endswith(".gz"):
        chmod_cmd = ("chmod","a+x", java_dir)
        install_cmd = format("cd {tmp_java_dir} && tar -xf {jdk_curl_target} && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")

      Directory(java_dir
                )

      Execute(chmod_cmd,
              sudo = True,
              )

      Execute(install_cmd,
              )

    finally:
      Directory(tmp_java_dir, action="delete")

    File(format("{custom_java_home}/bin/java"),
         mode=0755,
         cd_access="a",
         )
    Execute(('chmod', '-R', '755', params.java_home),
            sudo = True,
            )

