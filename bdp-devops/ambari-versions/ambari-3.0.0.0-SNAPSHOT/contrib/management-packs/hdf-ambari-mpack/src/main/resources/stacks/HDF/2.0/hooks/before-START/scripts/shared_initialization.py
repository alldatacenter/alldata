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
from resource_management.libraries.providers.hdfs_resource import WebHDFSUtil

from resource_management import *

def setup_hadoop():
  """
  Setup hadoop files and directories
  """
  import params

  Execute(("setenforce","0"),
          only_if="test -f /selinux/enforce",
          not_if="(! which getenforce ) || (which getenforce && getenforce | grep -q Disabled)",
          sudo=True,
  )

  #directories
  if params.has_namenode or params.dfs_type == 'HCFS':
    Directory(params.hdfs_log_dir_prefix,
              create_parents = True,
              owner='root',
              group=params.user_group,
              mode=0775,
              cd_access='a',
    )
    if params.has_namenode:
      Directory(params.hadoop_pid_dir_prefix,
              create_parents = True,
              owner='root',
              group='root',
              cd_access='a',
      )
    Directory(params.hadoop_tmp_dir,
              create_parents = True,
              owner=params.hdfs_user,
              cd_access='a',
              )
  #files
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user
      
    # if WebHDFS is not enabled we need this jar to create hadoop folders.
    if params.host_sys_prepped:
      print "Skipping copying of fast-hdfs-resource.jar as host is sys prepped"
    elif params.dfs_type == 'HCFS' or not WebHDFSUtil.is_webhdfs_available(params.is_webhdfs_enabled, params.default_fs):
      # for source-code of jar goto contrib/fast-hdfs-resource
      File(format("{ambari_libs_dir}/fast-hdfs-resource.jar"),
           mode=0644,
           content=StaticFile("fast-hdfs-resource.jar")
      )
      
    if os.path.exists(params.hadoop_conf_dir):
      File(os.path.join(params.hadoop_conf_dir, 'commons-logging.properties'),
           owner=tc_owner,
           content=Template('commons-logging.properties.j2')
      )

      health_check_template_name = "health_check"
      File(os.path.join(params.hadoop_conf_dir, health_check_template_name),
           owner=tc_owner,
           content=Template(health_check_template_name + ".j2")
      )

      log4j_filename = os.path.join(params.hadoop_conf_dir, "log4j.properties")
      if (params.log4j_props != None):
        File(log4j_filename,
             mode=0644,
             group=params.user_group,
             owner=params.hdfs_user,
             content=params.log4j_props
        )
      elif (os.path.exists(format("{params.hadoop_conf_dir}/log4j.properties"))):
        File(log4j_filename,
             mode=0644,
             group=params.user_group,
             owner=params.hdfs_user,
        )

      File(os.path.join(params.hadoop_conf_dir, "hadoop-metrics2.properties"),
           owner=params.hdfs_user,
           group=params.user_group,
           content=Template("hadoop-metrics2.properties.j2")
      )

    if params.dfs_type == 'HCFS' and params.has_core_site and 'ECS_CLIENT' in params.component_list:
       create_dirs()


def setup_configs():
  """
  Creates configs for services HDFS mapred
  """
  import params

  if params.has_namenode or params.dfs_type == 'HCFS':
    if os.path.exists(params.hadoop_conf_dir):
      File(params.task_log4j_properties_location,
           content=StaticFile("task-log4j.properties"),
           mode=0755
      )

    if os.path.exists(os.path.join(params.hadoop_conf_dir, 'configuration.xsl')):
      File(os.path.join(params.hadoop_conf_dir, 'configuration.xsl'),
           owner=params.hdfs_user,
           group=params.user_group
      )
    if os.path.exists(os.path.join(params.hadoop_conf_dir, 'masters')):
      File(os.path.join(params.hadoop_conf_dir, 'masters'),
                owner=params.hdfs_user,
                group=params.user_group
      )

def create_javahome_symlink():
  if os.path.exists("/usr/jdk/jdk1.6.0_31") and not os.path.exists("/usr/jdk64/jdk1.6.0_31"):
    Directory("/usr/jdk64/",
         create_parents = True,
    )
    Link("/usr/jdk/jdk1.6.0_31",
         to="/usr/jdk64/jdk1.6.0_31",
    )

def create_dirs():
   import params
   params.HdfsResource(params.hdfs_tmp_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.hdfs_user,
                       mode=0777
   )
   params.HdfsResource(params.smoke_hdfs_user_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.smoke_user,
                       mode=params.smoke_hdfs_user_mode
   )
   params.HdfsResource(None,
                      action="execute"
   )

