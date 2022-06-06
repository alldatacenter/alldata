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
from ambari_commons import os_utils
from ambari_commons.inet_utils import download_file
from ambari_commons.os_windows import SystemWideLock

from resource_management.core.resources.system import Execute
from resource_management.core.resources.system import File
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.reload_windows_env import reload_windows_env
from resource_management.libraries.functions.windows_service_utils import check_windows_service_exists
from resource_management.libraries.functions.version import format_stack_version, compare_versions
import socket
import os
import glob
import urlparse


__all__ = ['install_windows_msi']

msi_save_dir = None
log_dir = "c:\\hadoop\\logs"
data_dir = "c:\\hadoopDefaultData"
local_host = socket.getfqdn()
db_flavor = "DERBY"
hdp_22 = """#Namenode Data directory
HDFS_NAMENODE_DATA_DIR={data_dir}\\hdpdatann

#Datanode Data directory
HDFS_DATANODE_DATA_DIR={data_dir}\\hdpdatadn

IS_PHOENIX=no
"""
cluster_properties = """#Log directory
HDP_LOG_DIR={log_dir}

#Data directory
HDP_DATA_DIR={data_dir}

{hdp_22_specific_props}

#hosts
NAMENODE_HOST={local_host}
SECONDARY_NAMENODE_HOST={local_host}
RESOURCEMANAGER_HOST={local_host}
HIVE_SERVER_HOST={local_host}
OOZIE_SERVER_HOST={local_host}
WEBHCAT_HOST={local_host}
SLAVE_HOSTS={local_host}
ZOOKEEPER_HOSTS={local_host}
CLIENT_HOSTS={local_host}
HBASE_MASTER={local_host}
HBASE_REGIONSERVERS={local_host}
FLUME_HOSTS={local_host}
FALCON_HOST={local_host}
KNOX_HOST={local_host}
STORM_NIMBUS={local_host}
STORM_SUPERVISORS={local_host}

#Database host
DB_FLAVOR={db_flavor}
DB_HOSTNAME={local_host}
DB_PORT=1527

#Hive properties
HIVE_DB_NAME=hive
HIVE_DB_USERNAME=hive
HIVE_DB_PASSWORD=hive

#Oozie properties
OOZIE_DB_NAME=oozie
OOZIE_DB_USERNAME=oozie
OOZIE_DB_PASSWORD=oozie
"""

INSTALL_MSI_CMD = 'cmd /C start /wait msiexec /qn /i  {msi_path} /lv {log_path} MSIUSEREALADMINDETECTION=1 ' \
                  'HDP_LAYOUT={layout_path} DESTROY_DATA=yes HDP_USER={hadoop_user} HDP_USER_PASSWORD={hadoop_password_arg} HDP=yes ' \
                  'KNOX=yes KNOX_MASTER_SECRET="AmbariHDP2Windows" FALCON=yes STORM=yes HBase=yes STORM=yes FLUME=yes PHOENIX=no RANGER=no'
CREATE_SERVICE_SCRIPT = os.path.abspath("sbin\createservice.ps1")
CREATE_SERVICE_CMD = 'cmd /C powershell -ExecutionPolicy Bypass -File "{script}" -username {username} -password "{password}" -servicename ' \
                     '{servicename} -hdpresourcesdir "{resourcedir}" -servicecmdpath "{servicecmd}"'
INSTALL_MARKER_OK = "msi.installed"
INSTALL_MARKER_FAILED = "msi.failed"
_working_dir = None


def _ensure_services_created(hadoop_user, hadoop_password):
  resource_dir_hdfs = os.path.join(os.environ["HADOOP_HDFS_HOME"], "bin")
  service_cmd_hdfs = os.path.join(os.environ["HADOOP_HDFS_HOME"], "bin", "hdfs.cmd")
  if not check_windows_service_exists("journalnode"):
    Execute(CREATE_SERVICE_CMD.format(script=CREATE_SERVICE_SCRIPT, username=hadoop_user, password=hadoop_password, servicename="journalnode",
                                      resourcedir=resource_dir_hdfs, servicecmd=service_cmd_hdfs), logoutput=True)
  if not check_windows_service_exists("zkfc"):
    Execute(CREATE_SERVICE_CMD.format(script=CREATE_SERVICE_SCRIPT, username=hadoop_user, password=hadoop_password, servicename="zkfc",
                                      resourcedir=resource_dir_hdfs, servicecmd=service_cmd_hdfs), logoutput=True)


# creating symlinks to services folders to avoid using stack-dependent paths
def _create_symlinks(stack_version):
  # folders
  Execute("cmd /c mklink /d %HADOOP_NODE%\\hadoop %HADOOP_HOME%")
  Execute("cmd /c mklink /d %HADOOP_NODE%\\hive %HIVE_HOME%")
  stack_version_formatted = format_stack_version(stack_version)
  if stack_version_formatted != "" and compare_versions(stack_version_formatted, '2.2') >= 0:
    Execute("cmd /c mklink /d %HADOOP_NODE%\\knox %KNOX_HOME%")
  # files pairs (symlink_path, path_template_to_target_file), use * to replace file version
  links_pairs = [
    ("%HADOOP_HOME%\\share\\hadoop\\tools\\lib\\hadoop-streaming.jar",
     "%HADOOP_HOME%\\share\\hadoop\\tools\\lib\\hadoop-streaming-*.jar"),
    ("%HIVE_HOME%\\hcatalog\\share\\webhcat\\svr\\lib\\hive-webhcat.jar",
     "%HIVE_HOME%\\hcatalog\\share\\webhcat\\svr\\lib\\hive-webhcat-*.jar"),
    ("%HIVE_HOME%\\lib\\zookeeper.jar", "%HIVE_HOME%\\lib\\zookeeper-*.jar")
  ]
  for link_pair in links_pairs:
    link, target = link_pair
    target = glob.glob(os.path.expandvars(target))[0].replace("\\\\", "\\")
    Execute('cmd /c mklink "{0}" "{1}"'.format(link, target))


# check if services exists and marker file present
def _is_msi_installed():
  return os.path.exists(os.path.join(_working_dir, INSTALL_MARKER_OK)) and check_windows_service_exists("namenode")


# check if msi was installed correctly and raise Fail in case of broken install
def _validate_msi_install():
  if not _is_msi_installed() and os.path.exists(os.path.join(_working_dir, INSTALL_MARKER_FAILED)):
    raise Fail("Current or previous hdp.msi install failed. Check hdp.msi install logs")
  return _is_msi_installed()


def _write_marker():
  if check_windows_service_exists("namenode"):
    open(os.path.join(_working_dir, INSTALL_MARKER_OK), "w").close()
  else:
    open(os.path.join(_working_dir, INSTALL_MARKER_FAILED), "w").close()


def install_windows_msi(url_base, save_dir, save_files, hadoop_user, hadoop_password, stack_version):
  global _working_dir
  _working_dir = save_dir
  save_dir = os.path.abspath(save_dir)
  msi_save_dir = save_dir
  # system wide lock to prevent simultaneous installations(when first task failed on timeout)
  install_lock = SystemWideLock("Global\\hdp_msi_lock")
  try:
    # try to acquire lock
    if not install_lock.lock():
      Logger.info("Some other task currently installing hdp.msi, waiting for 10 min for finish")
      if not install_lock.lock(600000):
        raise Fail("Timeout on acquiring lock")
    if _validate_msi_install():
      Logger.info("hdp.msi already installed")
      return

    stack_version_formatted = format_stack_version(stack_version)
    hdp_22_specific_props = ''
    if stack_version_formatted != "" and compare_versions(stack_version_formatted, '2.2') >= 0:
      hdp_22_specific_props = hdp_22.format(data_dir=data_dir)

    # MSIs cannot be larger than 2GB. HDPWIN 2.3 needed split in order to accommodate this limitation
    msi_file = ''
    for save_file in save_files:
      if save_file.lower().endswith(".msi"):
        msi_file = save_file
      file_url = urlparse.urljoin(url_base, save_file)
      try:
        download_file(file_url, os.path.join(msi_save_dir, save_file))
      except:
        raise Fail("Failed to download {url}".format(url=file_url))

    File(os.path.join(msi_save_dir, "properties.txt"), content=cluster_properties.format(log_dir=log_dir,
                                                                                         data_dir=data_dir,
                                                                                         local_host=local_host,
                                                                                         db_flavor=db_flavor,
                                                                                         hdp_22_specific_props=hdp_22_specific_props))

    # install msi
    msi_path = os_utils.quote_path(os.path.join(save_dir, msi_file))
    log_path = os_utils.quote_path(os.path.join(save_dir, msi_file[:-3] + "log"))
    layout_path = os_utils.quote_path(os.path.join(save_dir, "properties.txt"))
    hadoop_password_arg = os_utils.quote_path(hadoop_password)

    Execute(
      INSTALL_MSI_CMD.format(msi_path=msi_path, log_path=log_path, layout_path=layout_path,
                             hadoop_user=hadoop_user, hadoop_password_arg=hadoop_password_arg))
    reload_windows_env()
    # create additional services manually due to hdp.msi limitaitons
    _ensure_services_created(hadoop_user, hadoop_password)
    _create_symlinks(stack_version)
    # finalizing install
    _write_marker()
    _validate_msi_install()
  finally:
    install_lock.unlock()
