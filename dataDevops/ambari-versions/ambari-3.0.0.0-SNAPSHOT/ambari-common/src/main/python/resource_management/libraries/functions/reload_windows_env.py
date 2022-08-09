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

from _winreg import (OpenKey, EnumValue, HKEY_LOCAL_MACHINE, KEY_READ, CloseKey)
import os

default_whitelist = ["FALCON_CONF_DIR", "FALCON_DATA_DIR", "FALCON_HOME", "FALCON_LOG_DIR", "FLUME_HOME",
                     "HADOOP_COMMON_HOME", "HADOOP_CONF_DIR", "HADOOP_HDFS_HOME", "HADOOP_HOME", "HADOOP_LOG_DIR",
                     "HADOOP_MAPRED_HOME", "HADOOP_NODE", "HADOOP_NODE_INSTALL_ROOT", "HADOOP_PACKAGES",
                     "HADOOP_SETUP_TOOLS", "HADOOP_YARN_HOME", "HBASE_CONF_DIR", "HBASE_HOME", "HCAT_HOME",
                     "HDFS_AUDIT_LOGGER", "HDFS_DATA_DIR", "HIVE_CONF_DIR", "HIVE_HOME", "HIVE_LIB_DIR", "HIVE_LOG_DIR",
                     "HIVE_OPTS", "KNOX_CONF_DIR", "KNOX_HOME", "KNOX_LOG_DIR", "MAHOUT_HOME", "OOZIE_DATA",
                     "OOZIE_HOME", "OOZIE_LOG", "OOZIE_ROOT", "PIG_HOME", "SQOOP_HOME", "STORM_CONF_DIR",
                     "STORM_HOME", "STORM_LOG_DIR", "TEZ_HOME", "WEBHCAT_CONF_DIR", "YARN_LOG_DIR", "ZOOKEEPER_CONF_DIR",
                     "ZOOKEEPER_HOME", "ZOOKEEPER_LIB_DIR", "ZOO_LOG_DIR", "COLLECTOR_CONF_DIR", "COLLECTOR_HOME",
                     "MONITOR_CONF_DIR", "MONITOR_HOME", "SINK_HOME"]
def reload_windows_env(keys_white_list=default_whitelist):
  root = HKEY_LOCAL_MACHINE
  subkey = r'SYSTEM\CurrentControlSet\Control\Session Manager\Environment'
  key = OpenKey(root, subkey, 0, KEY_READ)
  finish = False
  index = 0
  while not finish:
    try:
      _key, _value, _ = EnumValue(key, index)
      if (_key in keys_white_list):
        os.environ[_key] = _value
    except WindowsError:
      finish = True
    index += 1
  CloseKey(key)
