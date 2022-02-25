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

from resource_management.libraries.script import Script
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.default import default
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.packaging import Package


class RemoveBits(Script):
  """
  This script is called during Express Upgrade to remove bits.
  """

  def remove_hdp_21(self, env):
    """
    During Express Upgrade from HDP 2.1 to any higher version (HDP 2.2 or 2.3), the HDP 2.1 bits must be uninstalled.
    This is because /usr/bin/hadoop used to be a shell script in HDP 2.1, but in HDP 2.3 it is
    a symlink to <stack-root>/current/hadoop-client/bin/hadoop
    so both versions cannot coexist.
    """
    Logger.info("Attempting to remove bits for HDP 2.1")
    config = Script.get_config()

    packages_to_remove = ["zookeeper", "hadoop", "hadoop-lzo", "hadoop-hdfs", "hadoop-libhdfs", "hadoop-yarn", "hadoop-client", "hadoop-mapreduce", "hive", "hive-hcatalog", "hive-jdbc", "hive-webhcat", "hcatalog", "webhcat-tar-hive", "webhcat-tar-pig", "oozie", "oozie-client", "pig", "sqoop", "tez" "falcon", "storm", "flume", "hbase", "phoenix"]
    packages_to_remove.reverse()
    Logger.info("Packages to remove: {0}".format(" ".join(packages_to_remove)))

    for name in packages_to_remove:
      Logger.info("Attempting to remove {0}".format(name))
      Package(name, action="remove")

if __name__ == "__main__":
  RemoveBits().execute()
