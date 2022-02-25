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

Ambari Agent

"""

import os
import grp
import re

from resource_management import Script

import sys


CONFIG_PARAM_PREFIX = 'component_configurations'
CONFIG_PARAM_SEPARATOR = '/'
MAX_SUBST = 40


# Check if the specified in config properties directory exists and is empty
# or can be created on the host
PROPERTIES_TO_CHECK = {
  # HDFS
  "NAMENODE": {
    "hadoop-env": ["hdfs_log_dir_prefix", "hadoop_pid_dir_prefix"],
    "hdfs-site": ["dfs.namenode.name.dir"]
  },
  "DATANODE": {
    "hadoop-env": ["hdfs_log_dir_prefix", "hadoop_pid_dir_prefix", "dfs.domain.socket.path"],
    "hdfs-site": ["dfs.datanode.data.dir"]
  },
  "SECONDARY_NAMENODE": {
    "hadoop-env": ["hdfs_log_dir_prefix", "hadoop_pid_dir_prefix"],
    "hdfs-site": ["dfs.namenode.checkpoint.dir", "dfs.namenode.checkpoint.edits.dir"]
  },
  "JOURNALNODE": {
    "hadoop-env": ["hdfs_log_dir_prefix", "hadoop_pid_dir_prefix"],
    "hdfs-site": ["dfs.journalnode.edits.dir"]
  },
  #MAPREDUCE
  "JOBTRACKER": {
    "mapred-env": ["mapred_local_dir", "mapred_system_dir", "mapred_jobstatus_dir"],
    "mapred-site": ["mapred.local.dir", "mapred.healthChecker.script.path", "mapred.job.tracker.persist.jobstatus.dir"]
  },
  "TASKTRACKER": {
    "mapred-env": ["mapred_local_dir", "mapred_system_dir", "mapred_jobstatus_dir"],
    "mapred-site": ["mapred.local.dir", "mapred.healthChecker.script.path"]
  },
  "MAPREDUCE_CLIENT": {
    "mapred-env": ["mapred_local_dir", "mapred_system_dir", "mapred_jobstatus_dir"],
    "mapred-site": ["mapred.local.dir", "mapred.healthChecker.script.path"]
  },
  "HISTORYSERVER": {
    "mapred-env": ["mapred_local_dir", "mapred_system_dir", "mapred_jobstatus_dir"],
    "mapred-site": ["mapred.local.dir", "mapred.healthChecker.script.path"]
  },
  #MAPREDUCE2
  "MAPREDUCE2_CLIENT": {
    "mapred-env": ["mapred_log_dir_prefix", "mapred_pid_dir_prefix"],
    "mapred-site": ["mapreduce.jobhistory.done-dir", "mapreduce.jobhistory.intermediate-done-dir"]
  },
  #YARN
  "RESOURCEMANAGER": {
    "yarn-env": ["yarn_log_dir_prefix", "yarn_pid_dir_prefix", ]
  },
  "NODEMANAGER": {
    "yarn-env": ["yarn_log_dir_prefix", "yarn_pid_dir_prefix", ],
    "yarn-site": ["yarn.nodemanager.log-dirs", "yarn.nodemanager.local-dirs", "yarn.nodemanager.log-dirs", "yarn.nodemanager.remote-app-log-dir"]
  },
  "YARN_CLIENT": {
    "yarn-env": ["yarn_log_dir_prefix", "yarn_pid_dir_prefix", ]
  },
  #ZOOKEEPER
  "ZOOKEEPER_SERVER": {
    "zookeeper-env": ["zk_data_dir", "zk_log_dir", "zk_pid_dir"]
  },
  "ZOOKEEPER_CLIENT": {
    "zookeeper-env": ["zk_data_dir", "zk_log_dir", "zk_pid_dir"]
  },
  #FLUME
  "FLUME_HANDLER": {
    "flume-env": ["flume_conf_dir", "flume_log_dir"]
  },
  #GANGLIA
  "GANGLIA_MONITOR": {
    "ganglia-env": ["ganglia_conf_dir", "ganglia_runtime_dir"]
  },
  "GANGLIA_SERVER": {
    "ganglia-env": ["ganglia_conf_dir", "ganglia_runtime_dir", "rrdcached_base_dir"]
  },
  #HBASE
  "HBASE_MASTER": {
    "hbase-env": ["hbase_log_dir", "hbase_pid_dir"],
    "hbase-site": ["hbase.tmp.dir", "hbase.local.dir"]
  },
  "HBASE_REGIONSERVER": {
    "hbase-env": ["hbase_log_dir", "hbase_pid_dir"],
    "hbase-site": ["hbase.tmp.dir", "hbase.local.dir"]
  },
  "HBASE_CLIENT": {
    "hbase-env": ["hbase_log_dir", "hbase_pid_dir"],
    "hbase-site": ["hbase.tmp.dir", "hbase.local.dir"]
  },
  #HIVE
  "HIVE_METASTORE": {
    "hive-env": ["hive_log_dir", "hive_pid_dir"]
  },
  "HIVE_SERVER": {
    "hive-env": ["hive_log_dir", "hive_pid_dir"]
  },
  "MYSQL_SERVER": {
    "hive-env": ["hive_log_dir", "hive_pid_dir"]
  },
  "HIVE_CLIENT": {
    "hive-env": ["hive_log_dir", "hive_pid_dir"]
  },
  "HCAT": {
    "hive-env": ["hcat_log_dir", "hcat_pid_dir"]
  },
  #OOZIE
  "OOZIE_SERVER": {
    "oozie-env": ["oozie_data_dir", "oozie_log_dir", "oozie_pid_dir"]
  },
  #PIG - no directories to check
  #SQOOP - no directories to check
  #WEBHCAT - no directories to check
  #FALCON - no directories to check
  "FALCON_CLIENT": {
    "falcon-env": ["falcon_log_dir", "falcon_pid_dir", "falcon_local_dir", "falcon.embeddedmq.data"]
  },
  "FALCON_SERVER": {
    "falcon-env": ["falcon_log_dir", "falcon_pid_dir", "falcon_local_dir", "falcon.embeddedmq.data"]
  }
  #STORM - no directories to check
  #TEZ - no directories to check
}

# Check if the usernames exists
# - if they do - make sure they belong the correct group
# - if not - check if new users can be created.
USERS_TO_GROUP_MAPPING = {
  #HDFS
  "NAMENODE" : {
    "hadoop-env" : {
      "hdfs_user" : "user_group",
      "smokeuser" : "user_group",
    }
  },
  "DATANODE" : {
    "hadoop-env" : {
      "hdfs_user" : "user_group",
      "smokeuser" : "user_group",
    }
  },
  "SECONDARY_NAMENODE" : {
    "hadoop-env" : {
      "hdfs_user" : "user_group",
      "smokeuser" : "user_group",
    }
  },
  "JOURNALNODE" : {
    "hadoop-env" : {
      "hdfs_user" : "user_group",
      "smokeuser" : "user_group",
    }
  },
  "HDFS_CLIENT" : {
    "hadoop-env" : {
      "hdfs_user" : "user_group",
      "smokeuser" : "user_group",
    }
  },
  #MAPREDUCE
  "JOBTRACKER": {
    "mapred-env": {
      "mapred_user" : "mapred_user"
    }
  },
  "TASKTRACKER": {
    "mapred-env": {
      "mapred_user" : "mapred_user"
    }
  },
  "MAPREDUCE_CLIENT": {
    "mapred-env": {
      "mapred_user" : "mapred_user"
    }
  },
  "HISTORYSERVER": {
    "mapred-env": {
      "mapred_user" : "mapred_user"
    }
  },
  #MAPREDUCE2
  "MAPREDUCE2_CLIENT": {
    "mapred-env": {
      "mapred_user" : "mapred_user"
    }
  },
  #YARN
  "RESOURCEMANAGER": {
    "yarn-env": {
      "yarn_user": "yarn_user"
    }
  },
  "NODEMANAGER": {
    "yarn-env": {
      "yarn_user": "yarn_user"
    }
  },
  "YARN_CLIENT": {
    "yarn-env": {
      "yarn_user": "yarn_user"
    }
  },
  #ZOOKEEPER
  "ZOOKEEPER_SERVER": {
    "zookeeper-env": {
      "zk_user" : "zk_user"
    }
  },
  "ZOOKEEPER_CLIENT": {
    "zookeeper-env": {
      "zk_user" : "zk_user"
    }
  },
  #FLUME
  "FLUME_HANDLER": {
    "flume-env": {
      "flume_user": "flume_user"
    }
  },
  #GANGLIA
  "GANGLIA_MONITOR": {
    "ganglia-env": {
      "gmond_user" : "gmond_user"
    }
  },
  "GANGLIA_SERVER": {
    "ganglia-env": {
      "gmetad_user" : "gmetad_user"
    }
  },
  #HBASE
  "HBASE_MASTER": {
    "hbase-env": {
      "hbase_user": "hbase_user"
    }
  },
  "HBASE_REGIONSERVER": {
    "hbase-env": {
      "hbase_user": "hbase_user"
    }
  },
  "HBASE_CLIENT": {
    "hbase-env": {
      "hbase_user": "hbase_user"
    }
  },
  #HIVE
  "HIVE_METASTORE": {
    "hive-env": {
      "hive_user": "hive_user"
    }
  },
  "HIVE_SERVER": {
    "hive-env": {
      "hive_user": "hive_user"
    }
  },
  "MYSQL_SERVER": {
    "hive-env": {
      "hive_user": "hive_user"
    }
  },
  "HIVE_CLIENT": {
    "hive-env": {
      "hive_user": "hive_user"
    }
  },
  "HCAT": {
    "hive-env": {
      "hive_user": "hive_user"
    }
  },
  #OOZIE
  "OOZIE_SERVER": {
    "oozie-env": {
      "oozie_user": "oozie_user"
    }
  },
  #PIG - no users to check
  #SQOOP - no users to check
  #WEBHCAT - no users to check
  #FALCON
  "FALCON_CLIENT": {
    "falcon-env": {
      "falcon_user": "falcon_user"
    }
  },
  "FALCON_SERVER": {
    "falcon-env": {
      "falcon_user": "falcon_user"
    }
  },
  #STORM
  "NIMBUS": {
    "storm-env": {
      "storm_user": "storm_user"
    }
  },
  "STORM_REST_API": {
    "storm-env": {
      "storm_user": "storm_user"
    }
  },
  "SUPERVISOR": {
    "storm-env": {
      "storm_user": "storm_user"
    }
  },
  "STORM_UI_SERVER": {
    "storm-env": {
      "storm_user": "storm_user"
    }
  },
  "DRPC_SERVER": {
    "storm-env": {
      "storm_user": "storm_user"
    }
  },
  #TEZ
  "TEZ_CLIENT": {
    "tez-env": {
      "tez_user": "tez_user"
    }
  }
}

class ValidateConfigs(Script):

  def actionexecute(self, env):
    config = Script.get_config()
    params = config['commandParams']

    validation_passed = self.check_users(params) and self.check_directories(params)

    if validation_passed:
      print 'All configurations validated!'
    else:
      self.fail_with_error('Configurations validation failed!')

  def check_directories(self, params):
    validation_failed = False
    properties_to_check = self.flatten_dict(PROPERTIES_TO_CHECK)
    for property, value in params.items():
      if property in properties_to_check:
        if self.dir_exists_or_not_empty_or_cant_be_created(self.get_value(property, params)):
          validation_failed = True
    return not validation_failed

  def check_users(self, params):
    validation_passed = True
    for user, group in self.dict_to_list(USERS_TO_GROUP_MAPPING).items():
      if user in params and group in params:
        username = self.get_value(user, params)
        groupname = self.get_value(group, params)
        if not self.check_user_in_group(username, groupname):
          if os.geteuid() != 0:
            msg = 'Validation failed. ' + username + ' is not a member of ' \
                  + groupname + ' group and ' + username + ' account can\'t ' \
                  'be created!\n'
            sys.stderr.write('Error: ' + msg)
            validation_passed = False
    return validation_passed

  def flatten_dict(self, dic, prefix=CONFIG_PARAM_PREFIX, separator=CONFIG_PARAM_SEPARATOR):
    result = []
    for key, val in dic.items():
      sum_key = prefix + separator + key
      if isinstance(val, dict):
        result += self.flatten_dict(val, sum_key, separator)
      else:
        for v in val:
          result.append(sum_key + separator + v)
    return result

  def dir_exists_or_not_empty_or_cant_be_created(self, filepath):
    if os.path.isdir(filepath) and os.access(filepath, os.W_OK) and os.listdir(filepath):
      msg = 'Validation failed. Directory ' + filepath \
            + ' already exists and isn\'t empty!\n'
      sys.stderr.write('Error: ' + msg)
      return True
    elif not os.access(self.get_existed_subdir(filepath), os.W_OK):
      msg = 'Validation failed. Directory ' + filepath + ' can\'t be created!\n'
      sys.stderr.write('Error: ' + msg)
      return True
    return False

  def get_existed_subdir(self, path):
    if os.path.exists(path) and os.path.isdir(path):
      return path
    else:
      return self.get_existed_subdir(os.path.dirname(path))

  def dict_to_list(self, dic, prefix=CONFIG_PARAM_PREFIX, separator=CONFIG_PARAM_SEPARATOR):
    result = {}
    for key, val in dic.items():
      sum_key = prefix + separator + key
      if isinstance(val, dict):
        result.update(self.dict_to_list(val, sum_key, separator))
      else:
        result[sum_key] = prefix + separator + val
    return result

  # username is a member of groupname or new account can be created
  def check_user_in_group(self, username, groupname):
    try:
      result = username == groupname or username in grp.getgrnam(groupname).gr_mem
    except KeyError:
      result = False
    return result

  # handle values like $(some.another.property.name}
  def get_value(self, key, params):

    result = params[key]

    pattern = re.compile("\$\{[^\}\$\x0020]+\}")

    for depth in range(0, MAX_SUBST - 1):
      match = pattern.search(result)

      if match:
        start = match.start()
        end = match.end()

        rx = re.compile(r'(.*/)(.+)')
        name = rx.sub(r'\g<1>' + result[start + 2 : end - 1], key)

        try:
          value = params[name]
        except KeyError:
          return result

        result = result[:start] + value + result[end:]
      else:
        break

    return result

if __name__ == "__main__":
  ValidateConfigs().execute()


