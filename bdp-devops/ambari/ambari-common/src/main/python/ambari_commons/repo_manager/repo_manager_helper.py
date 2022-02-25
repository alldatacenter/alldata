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
from . import ManagerFactory
from resource_management.libraries.functions.version import compare_versions
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from ambari_commons import OSCheck

def check_installed_metrics_hadoop_sink_version(hadoop_sink_package_name="ambari-metrics-hadoop-sink",
                                                checked_version="2.7.0.0", less_valid=True, equal_valid=False):

  # The default package name is different for ubuntu and debian, so if the dafault one is used change the name
  if hadoop_sink_package_name == "ambari-metrics-hadoop-sink" and OSCheck.is_ubuntu_family():
    hadoop_sink_package_name = "ambari-metrics-assembly"

  pkg_provider = ManagerFactory.get()
  hadoop_sink_version = pkg_provider.get_installed_package_version(hadoop_sink_package_name)

  if not hadoop_sink_version:
    Logger.warning("Couldn't determine %s package version, skipping the sink version check" % hadoop_sink_package_name)
    return
  else:
    if "-" in hadoop_sink_version:
      hadoop_sink_version = hadoop_sink_version.split("-")[0]
    # installed version should be less than next version
    compare_result = compare_versions(hadoop_sink_version, checked_version)
    if equal_valid and compare_result == 0:
      pass
    elif less_valid and compare_result != -1:
      raise Fail("%s installed package version is %s. It should be less than %s due to"
                 " incompatibility. Please downgrade the package or upgrade the stack and try again."
                 % (hadoop_sink_package_name, hadoop_sink_version, checked_version))

    elif not less_valid and compare_result != 1:
      raise Fail("%s installed package version is %s. It should be greater than or equal to %s due to"
                 " incompatibility. Please upgrade the package or downgrade the stack and try again."
                 % (hadoop_sink_package_name, hadoop_sink_version, checked_version))

  Logger.info("ambari-metrics-hadoop-sink package version is OK")