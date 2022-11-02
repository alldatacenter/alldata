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
from resource_management.core.resources.jcepolicyinfo import JcePolicyInfo

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
      Directory(format("{hadoop_pid_dir_prefix}/{hdfs_user}"),
              owner=params.hdfs_user,
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
             content=InlineTemplate(params.log4j_props)
        )
      elif (os.path.exists(format("{params.hadoop_conf_dir}/log4j.properties"))):
        File(log4j_filename,
             mode=0644,
             group=params.user_group,
             owner=params.hdfs_user,
        )

    create_microsoft_r_dir()


  # if WebHDFS is not enabled we need this jar to create hadoop folders and copy tarballs to HDFS.
  if params.sysprep_skip_copy_fast_jar_hdfs:
    print "Skipping copying of fast-hdfs-resource.jar as host is sys prepped"
  else:
    # for source-code of jar goto contrib/fast-hdfs-resource
    File(format("{ambari_libs_dir}/fast-hdfs-resource.jar"),
         mode=0644,
         content=StaticFile("fast-hdfs-resource.jar")
         )

  if params.has_hdfs or params.dfs_type == 'HCFS':
    if os.path.exists(params.hadoop_conf_dir):
      if params.hadoop_metrics2_properties_content:
        File(os.path.join(params.hadoop_conf_dir, "hadoop-metrics2.properties"),
             owner=params.hdfs_user,
             group=params.user_group,
             content=InlineTemplate(params.hadoop_metrics2_properties_content)
             )
      else:
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

def create_microsoft_r_dir():
  import params
  if 'MICROSOFT_R_NODE_CLIENT' in params.component_list and params.default_fs:
    directory = '/user/RevoShare'
    try:
      params.HdfsResource(directory,
                          type="directory",
                          action="create_on_execute",
                          owner=params.hdfs_user,
                          mode=0777)
      params.HdfsResource(None, action="execute")
    except Exception as exception:
      Logger.warning("Could not check the existence of {0} on DFS while starting {1}, exception: {2}".format(directory, params.current_service, str(exception)))

def setup_unlimited_key_jce_policy():
  """
  Sets up the unlimited key JCE policy if needed. (sets up ambari JCE as well if ambari and the  stack use different JDK)
  """
  import params
  __setup_unlimited_key_jce_policy(custom_java_home=params.java_home, custom_jdk_name=params.jdk_name, custom_jce_name = params.jce_policy_zip)
  if params.ambari_jce_name and params.ambari_jce_name != params.jce_policy_zip:
    __setup_unlimited_key_jce_policy(custom_java_home=params.ambari_java_home, custom_jdk_name=params.ambari_jdk_name, custom_jce_name = params.ambari_jce_name)

def __setup_unlimited_key_jce_policy(custom_java_home, custom_jdk_name, custom_jce_name):
  """
  Sets up the unlimited key JCE policy if needed.

  The following criteria must be met:

    * The cluster has not been previously prepared (sys preped) - cluster-env/sysprep_skip_setup_jce = False
    * Ambari is managing the host's JVM - /ambariLevelParams/jdk_name is set
    * Either security is enabled OR a service requires it - /componentLevelParams/unlimited_key_jce_required = True
    * The unlimited key JCE policy has not already been installed

  If the conditions are met, the following steps are taken to install the unlimited key JCE policy JARs

    1. The unlimited key JCE policy ZIP file is downloaded from the Ambari server and stored in the
        Ambari agent's temporary directory
    2. The existing JCE policy JAR files are deleted
    3. The downloaded ZIP file is unzipped into the proper JCE policy directory

  :return: None
  """
  import params

  if params.sysprep_skip_setup_jce:
    Logger.info("Skipping unlimited key JCE policy check and setup since the host is sys prepped")

  elif not custom_jdk_name:
    Logger.info("Skipping unlimited key JCE policy check and setup since the Java VM is not managed by Ambari")

  elif not params.unlimited_key_jce_required:
    Logger.info("Skipping unlimited key JCE policy check and setup since it is not required")

  else:
    jcePolicyInfo = JcePolicyInfo(custom_java_home)

    if jcePolicyInfo.is_unlimited_key_jce_policy():
      Logger.info("The unlimited key JCE policy is required, and appears to have been installed.")

    elif custom_jce_name is None:
      raise Fail("The unlimited key JCE policy needs to be installed; however the JCE policy zip is not specified.")

    else:
      Logger.info("The unlimited key JCE policy is required, and needs to be installed.")

      jce_zip_target = format("{artifact_dir}/{custom_jce_name}")
      jce_zip_source = format("{ambari_server_resources_url}/{custom_jce_name}")
      java_security_dir = format("{custom_java_home}/jre/lib/security")

      Logger.debug("Downloading the unlimited key JCE policy files from {0} to {1}.".format(jce_zip_source, jce_zip_target))
      Directory(params.artifact_dir, create_parents=True)
      File(jce_zip_target, content=DownloadSource(jce_zip_source))

      Logger.debug("Removing existing JCE policy JAR files: {0}.".format(java_security_dir))
      File(format("{java_security_dir}/US_export_policy.jar"), action="delete")
      File(format("{java_security_dir}/local_policy.jar"), action="delete")

      Logger.debug("Unzipping the unlimited key JCE policy files from {0} into {1}.".format(jce_zip_target, java_security_dir))
      extract_cmd = ("unzip", "-o", "-j", "-q", jce_zip_target, "-d", java_security_dir)
      Execute(extract_cmd,
              only_if=format("test -e {java_security_dir} && test -f {jce_zip_target}"),
              path=['/bin/', '/usr/bin'],
              sudo=True
              )
